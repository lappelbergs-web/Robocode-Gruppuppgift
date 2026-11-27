package grupp;

import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MinnaTeamRobot - lagets "Tank".
 *
 * Roll i laget:
 *  - Droid: har INGEN radar, men startar med 120 i energi i stället för 100.
 *  - Ser alltså inte själv var fienderna är. Den lever helt på EnemyPosition-
 *    meddelanden från lagkamraterna som har radar.
 *  - Slåss på kort avstånd (ca 200 pixlar) och skjuter hårda skott.
 *  - Väljer samma mål som resten av laget: fienden med lägst energi.
 *
 * Förstår samma meddelanden som PeterTeamRobot (TeammatePosition och EnemyPosition).
 */
public class MinnaTeamRobot extends TeamRobot implements Droid {

    /** Avståndet till målet som Tanken försöker hålla (pixlar). */
    private static final double PREFERRED_DISTANCE = 200;
    /** Hur gammal (i turer) en fiendeposition får vara innan vi struntar i den. */
    private static final int MAX_ENEMY_AGE = 30;
    /** Hur nära skottlinjen en lagkamrat får stå innan vi håller elden (pixlar). */
    private static final double TEAMMATE_SAFETY_DISTANCE = 40;
    /** Avstånd från väggen där vi börjar svänga in mot mitten (pixlar). */
    private static final double WALL_MARGIN = 60;

    /** Lagkamraternas senaste positioner: namn -> position. */
    private final Map<String, Point2D.Double> teammates = new HashMap<>();
    /** Allt laget vet om fienderna: namn -> senaste observation. */
    private final Map<String, EnemyPosition> enemies = new HashMap<>();

    /** 1 = framåt, -1 = bakåt. */
    private int moveDirection = 1;

    /** Reserv när laget inte rapporterar något: varifrån vi senast blev beskjutna. */
    private double lastThreatAngle;
    private long lastThreatTime = -1000;

    @Override
    public void run() {
        // Egen färg så att man ser vem som är vem på slagfältet.
        setColors(new Color(150, 40, 150), Color.PINK, Color.WHITE);

        setAdjustGunForRobotTurn(true);

        while (true) {
            sendToTeam(new TeammatePosition(getX(), getY()));

            EnemyPosition target = chooseTarget();
            if (target != null) {
                aimAndFire(target);
                move(target);
            } else {
                patrol();
            }

            execute();
        }
    }

    // ======================================================================
    //  Händelser
    // ======================================================================

    @Override
    public void onMessageReceived(MessageEvent e) {
        Object message = e.getMessage();

        if (message instanceof TeammatePosition p) {
            teammates.put(e.getSender(), new Point2D.Double(p.x(), p.y()));

        } else if (message instanceof EnemyPosition enemy) {
            EnemyPosition old = enemies.get(enemy.name());
            if (old == null || enemy.time() > old.time()) {
                enemies.put(enemy.name(), enemy);
            }
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        enemies.remove(e.getName());
        teammates.remove(e.getName());
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        moveDirection = -moveDirection;
        // Kom ihåg varifrån skottet kom, om laget inte kan berätta var fienden är.
        lastThreatAngle = getHeadingRadians() + e.getBearingRadians();
        lastThreatTime = getTime();
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        moveDirection = -moveDirection;
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        moveDirection = -moveDirection;
        setAhead(80 * moveDirection);
        if (!isTeammate(e.getName())) {
            // Fiende rakt intill oss: kom ihåg riktningen så vi kan skjuta dit.
            lastThreatAngle = getHeadingRadians() + e.getBearingRadians();
            lastThreatTime = getTime();
        }
    }

    @Override
    public void onWin(WinEvent e) {
        for (int i = 0; i < 20; i++) {
            turnRight(30);
            turnLeft(30);
        }
    }

    // ======================================================================
    //  Målval, sikte och rörelse
    // ======================================================================

    /**
     * Samma regel som resten av laget: fienden med lägst energi bland dem som
     * setts nyligen. Vid lika energi avgör namnet.
     */
    private EnemyPosition chooseTarget() {
        EnemyPosition best = null;
        for (EnemyPosition enemy : enemies.values()) {
            if (getTime() - enemy.time() > MAX_ENEMY_AGE) {
                continue;
            }
            if (best == null
                    || enemy.energy() < best.energy()
                    || (enemy.energy() == best.energy() && enemy.name().compareTo(best.name()) < 0)) {
                best = enemy;
            }
        }
        return best;
    }

    /** Sikta dit målet är på väg och skjut hårt, om ingen lagkamrat är i vägen. */
    private void aimAndFire(EnemyPosition target) {
        double distance = Point2D.distance(getX(), getY(), target.x(), target.y());

        // Tanken skjuter hårdare än övriga: full styrka ända ut till 300 pixlar.
        double power = Math.min(3.0, 900 / distance);
        power = Math.min(power, getEnergy() / 10);
        power = Math.max(0.1, power);
        double bulletSpeed = 20 - 3 * power;

        Point2D.Double aimPoint = new Point2D.Double(target.x(), target.y());
        for (int i = 0; i < 10; i++) {
            double flightTime = Point2D.distance(getX(), getY(), aimPoint.x, aimPoint.y) / bulletSpeed;
            double ticks = (getTime() - target.time()) + flightTime;
            aimPoint = predictPosition(target, ticks);
        }

        boolean gunReady = getGunHeat() == 0
                && Math.abs(getGunTurnRemainingRadians()) < Math.atan(18 / distance) + 0.05;

        if (gunReady && getEnergy() > 0.2 && !teammateInLineOfFire(aimPoint.x, aimPoint.y)) {
            setFire(power);
        }

        double angleToAim = Math.atan2(aimPoint.x - getX(), aimPoint.y - getY());
        setTurnGunRightRadians(Utils.normalRelativeAngle(angleToAim - getGunHeadingRadians()));
    }

    /** Var är fienden om 'ticks' turer, om den fortsätter rakt fram? */
    private Point2D.Double predictPosition(EnemyPosition enemy, double ticks) {
        double x = enemy.x() + Math.sin(enemy.heading()) * enemy.velocity() * ticks;
        double y = enemy.y() + Math.cos(enemy.heading()) * enemy.velocity() * ticks;
        x = Math.max(18, Math.min(getBattleFieldWidth() - 18, x));
        y = Math.max(18, Math.min(getBattleFieldHeight() - 18, y));
        return new Point2D.Double(x, y);
    }

    /**
     * Cirkla runt målet på ungefär PREFERRED_DISTANCE.
     * För långt bort: svänga in mot målet. För nära: svänga ut.
     */
    private void move(EnemyPosition target) {
        if (avoidWalls()) {
            return;
        }

        if (Math.random() < 0.05) {
            moveDirection = -moveDirection;
        }

        double distance = Point2D.distance(getX(), getY(), target.x(), target.y());
        double angleToTarget = Math.atan2(target.x() - getX(), target.y() - getY());

        // Positivt = närma oss, negativt = backa ut. Begränsat så vi aldrig kör rakt på.
        double approach = (distance - PREFERRED_DISTANCE) / 300;
        approach = Math.max(-0.7, Math.min(0.7, approach));

        double desiredHeading = angleToTarget + Math.PI / 2 - approach * moveDirection;
        setTurnRightRadians(Utils.normalRelativeAngle(desiredHeading - getHeadingRadians()));
        setAhead(120 * moveDirection);
    }

    /**
     * Inget mål från laget (t.ex. alla med radar är döda):
     * kör runt, och skjut tillbaka mot den som senast sköt på oss.
     */
    private void patrol() {
        if (!avoidWalls()) {
            setTurnRight(15);
            setAhead(80);
        }

        if (getTime() - lastThreatTime < 50) {
            setTurnGunRightRadians(Utils.normalRelativeAngle(lastThreatAngle - getGunHeadingRadians()));
            if (getGunHeat() == 0 && Math.abs(getGunTurnRemainingRadians()) < 0.1 && getEnergy() > 1) {
                setFire(1);
            }
        }
    }

    /** Om vi är nära en vägg: sväng in mot mitten innan vi krockar. */
    private boolean avoidWalls() {
        double width = getBattleFieldWidth();
        double height = getBattleFieldHeight();
        boolean nearWall = getX() < WALL_MARGIN || getX() > width - WALL_MARGIN
                || getY() < WALL_MARGIN || getY() > height - WALL_MARGIN;

        if (!nearWall) {
            return false;
        }

        double angleToCenter = Math.atan2(width / 2 - getX(), height / 2 - getY());
        setTurnRightRadians(Utils.normalRelativeAngle(angleToCenter - getHeadingRadians()));
        moveDirection = 1;
        setAhead(100);
        return true;
    }

    // ======================================================================
    //  Friendly fire och meddelanden
    // ======================================================================

    /** Står någon lagkamrat nära linjen mellan oss och siktpunkten? */
    private boolean teammateInLineOfFire(double targetX, double targetY) {
        Line2D.Double lineOfFire = new Line2D.Double(getX(), getY(), targetX, targetY);
        for (Point2D.Double teammate : teammates.values()) {
            if (lineOfFire.ptSegDist(teammate) < TEAMMATE_SAFETY_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private void sendToTeam(java.io.Serializable message) {
        try {
            broadcastMessage(message);
        } catch (IOException ex) {
            ex.printStackTrace(out);
        }
    }
}
