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
 * JonTeamRobot - lagets "Prickskytt".
 *
 * Roll i laget:
 *  - Har radar och delar allt den ser med laget (EnemyPosition).
 *  - Håller sig långt bort från målet (ca 400 pixlar) så att den är svår att träffa.
 *  - Skjuter snabbare, lite svagare kulor och bara när kanonen är exakt riktad.
 *  - Byter riktning oftare än de andra, för att undvika fiendens skott.
 *  - Väljer samma mål som resten av laget: fienden med lägst energi.
 */
public class JonTeamRobot extends TeamRobot {

    /** Avståndet till målet som Prickskytten försöker hålla (pixlar). */
    private static final double PREFERRED_DISTANCE = 400;
    private static final int MAX_ENEMY_AGE = 30;
    private static final double TEAMMATE_SAFETY_DISTANCE = 40;
    private static final double WALL_MARGIN = 60;

    private final Map<String, Point2D.Double> teammates = new HashMap<>();
    private final Map<String, EnemyPosition> enemies = new HashMap<>();

    private int moveDirection = 1;

    @Override
    public void run() {
        setColors(new Color(30, 100, 40), Color.GREEN, Color.YELLOW);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            // Radarn snurrar hela tiden så att laget får information om alla fiender.
            setTurnRadarRight(Double.POSITIVE_INFINITY);

            sendToTeam(new TeammatePosition(getX(), getY()));

            EnemyPosition target = chooseTarget();
            if (target != null) {
                aimAndFire(target);
                move(target);
            } else {
                if (!avoidWalls()) {
                    setTurnRight(20);
                    setAhead(50);
                }
            }

            execute();
        }
    }

    // ======================================================================
    //  Händelser
    // ======================================================================

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if (isTeammate(e.getName())) {
            return;
        }

        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
        double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);

        EnemyPosition seen = new EnemyPosition(e.getName(), enemyX, enemyY,
                e.getHeadingRadians(), e.getVelocity(), e.getEnergy(), getTime());

        enemies.put(seen.name(), seen);
        sendToTeam(seen);
    }

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
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        moveDirection = -moveDirection;
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        // Prickskytten vill inte vara nära någon: backa ifrån.
        moveDirection = -moveDirection;
        setAhead(100 * moveDirection);
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

    /** Samma regel som resten av laget: lägst energi, vid lika energi avgör namnet. */
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

    /**
     * Prickskyttens sikte: kulor med styrka 2 (snabbare än 3, så de hinner fram
     * på långt avstånd) och skott bara när kanonen pekar exakt rätt.
     */
    private void aimAndFire(EnemyPosition target) {
        double distance = Point2D.distance(getX(), getY(), target.x(), target.y());

        double power = distance < 200 ? 3.0 : 2.0;
        power = Math.min(power, getEnergy() / 10);
        power = Math.max(0.1, power);
        double bulletSpeed = 20 - 3 * power;

        Point2D.Double aimPoint = new Point2D.Double(target.x(), target.y());
        for (int i = 0; i < 10; i++) {
            double flightTime = Point2D.distance(getX(), getY(), aimPoint.x, aimPoint.y) / bulletSpeed;
            double ticks = (getTime() - target.time()) + flightTime;
            aimPoint = predictPosition(target, ticks);
        }

        // Strängare krav än de andra robotarna: kanonen ska peka inom halva robotens bredd.
        boolean gunReady = getGunHeat() == 0
                && Math.abs(getGunTurnRemainingRadians()) < Math.atan(9 / distance);

        if (gunReady && getEnergy() > 0.2 && !teammateInLineOfFire(aimPoint.x, aimPoint.y)) {
            setFire(power);
        }

        double angleToAim = Math.atan2(aimPoint.x - getX(), aimPoint.y - getY());
        setTurnGunRightRadians(Utils.normalRelativeAngle(angleToAim - getGunHeadingRadians()));
    }

    private Point2D.Double predictPosition(EnemyPosition enemy, double ticks) {
        double x = enemy.x() + Math.sin(enemy.heading()) * enemy.velocity() * ticks;
        double y = enemy.y() + Math.cos(enemy.heading()) * enemy.velocity() * ticks;
        x = Math.max(18, Math.min(getBattleFieldWidth() - 18, x));
        y = Math.max(18, Math.min(getBattleFieldHeight() - 18, y));
        return new Point2D.Double(x, y);
    }

    /** Cirkla runt målet på långt avstånd och byt riktning ofta. */
    private void move(EnemyPosition target) {
        if (avoidWalls()) {
            return;
        }

        // Byter riktning dubbelt så ofta som de andra: svårare att förutsäga.
        if (Math.random() < 0.10) {
            moveDirection = -moveDirection;
        }

        double distance = Point2D.distance(getX(), getY(), target.x(), target.y());
        double angleToTarget = Math.atan2(target.x() - getX(), target.y() - getY());

        double approach = (distance - PREFERRED_DISTANCE) / 300;
        approach = Math.max(-0.7, Math.min(0.7, approach));

        double desiredHeading = angleToTarget + Math.PI / 2 - approach * moveDirection;
        setTurnRightRadians(Utils.normalRelativeAngle(desiredHeading - getHeadingRadians()));
        setAhead(120 * moveDirection);
    }

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
