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
 * PeterTeamRobot - teamversion av PeterRobot_2.
 *
 * Alla i laget är kopior av den här roboten. De samarbetar så här:
 *  1. Skjuter aldrig på lagkamrater (isTeammate + kontroll av skottlinjen).
 *  2. Delar sin egen position varje tur (TeammatePosition).
 *  3. Delar allt de ser av fienden (EnemyPosition).
 *  4. Alla väljer samma mål: fienden med lägst energi ("focus fire").
 *
 * All skjutning och rörelse sker i run()-loopen, en gång per tur,
 * baserat på det roboten själv och laget har sett.
 */
public class PeterTeamRobot extends TeamRobot {

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

    @Override
    public void run() {
        setColors(Color.DARK_GRAY, Color.RED, Color.ORANGE);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            // Radarn snurrar hela tiden: i lagstrid vill vi se alla, inte låsa på en.
            setTurnRadarRight(Double.POSITIVE_INFINITY);

            sendMyPosition();

            EnemyPosition target = chooseTarget();
            if (target != null) {
                aimAndFire(target);
                move(target);
            } else {
                // Inget mål än: rör oss lite så vi inte står still.
                setTurnRight(20);
                setAhead(50);
                avoidWalls();
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
            return; // lagkamrat: inte ett mål
        }

        // Räkna ut fiendens position på slagfältet.
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
            // Spara bara om lagkamratens information är nyare än vår egen.
            EnemyPosition old = enemies.get(enemy.name());
            if (old == null || enemy.time() > old.time()) {
                enemies.put(enemy.name(), enemy);
            }
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        // Död robot: varken mål eller i vägen längre.
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
        // Krock: backa ifrån, oavsett om det är fiende eller lagkamrat.
        moveDirection = -moveDirection;
        setAhead(80 * moveDirection);
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
     * Välj mål: fienden med lägst energi bland dem som setts nyligen.
     * Eftersom alla i laget delar samma information väljer alla samma mål.
     * Vid lika energi avgör namnet, så att laget inte delar upp sig.
     */
    private EnemyPosition chooseTarget() {
        EnemyPosition best = null;
        for (EnemyPosition enemy : enemies.values()) {
            if (getTime() - enemy.time() > MAX_ENEMY_AGE) {
                continue; // för gammal information
            }
            if (best == null
                    || enemy.energy() < best.energy()
                    || (enemy.energy() == best.energy() && enemy.name().compareTo(best.name()) < 0)) {
                best = enemy;
            }
        }
        return best;
    }

    /** Sikta dit målet är på väg och skjut om ingen lagkamrat är i vägen. */
    private void aimAndFire(EnemyPosition target) {
        double distance = Point2D.distance(getX(), getY(), target.x(), target.y());

        // Skottstyrka: hårt på nära håll, svagt långt bort, spara energi när den är låg.
        double power = Math.min(3.0, 400 / distance);
        power = Math.min(power, getEnergy() / 10);
        power = Math.max(0.1, power);
        double bulletSpeed = 20 - 3 * power;

        // Förutsäg var målet är när kulan kommer fram (rak linje, konstant fart).
        // Räknas i några varv eftersom flygtiden beror på var målet hamnar.
        Point2D.Double aimPoint = new Point2D.Double(target.x(), target.y());
        for (int i = 0; i < 10; i++) {
            double flightTime = Point2D.distance(getX(), getY(), aimPoint.x, aimPoint.y) / bulletSpeed;
            double ticks = (getTime() - target.time()) + flightTime;
            aimPoint = predictPosition(target, ticks);
        }

        // Skjut först (kanonen har riktats sedan förra turen), vrid sedan för nästa tur.
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
        // Fienden kan inte åka igenom väggen.
        x = Math.max(18, Math.min(getBattleFieldWidth() - 18, x));
        y = Math.max(18, Math.min(getBattleFieldHeight() - 18, y));
        return new Point2D.Double(x, y);
    }

    /** Kör i sidled runt målet, svårast att träffa. */
    private void move(EnemyPosition target) {
        if (avoidWalls()) {
            return;
        }

        // Byt riktning ibland så att fienden inte kan förutsäga oss.
        if (Math.random() < 0.05) {
            moveDirection = -moveDirection;
        }

        double angleToTarget = Math.atan2(target.x() - getX(), target.y() - getY());
        // +90 grader = vinkelrätt mot målet. -0.25*riktning drar oss lite närmare.
        double desiredHeading = angleToTarget + Math.PI / 2 - 0.25 * moveDirection;
        setTurnRightRadians(Utils.normalRelativeAngle(desiredHeading - getHeadingRadians()));
        setAhead(120 * moveDirection);
    }

    /**
     * Om vi är nära en vägg: sväng in mot mitten innan vi krockar.
     * Returnerar true om vi svängde undan.
     */
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

    private void sendMyPosition() {
        sendToTeam(new TeammatePosition(getX(), getY()));
    }

    private void sendToTeam(java.io.Serializable message) {
        try {
            broadcastMessage(message);
        } catch (IOException ex) {
            ex.printStackTrace(out);
        }
    }
}
