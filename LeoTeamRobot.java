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
 * LeoTeamRobot - lagets "Livvakt".
 *
 * Roll i laget:
 *  - Har radar och delar allt den ser med laget (EnemyPosition).
 *  - Håller sig nära ledaren (PeterTeamRobot), som har mest energi och är viktigast.
 *  - Skjuter på den fiende som är NÄRMAST LEDAREN, alltså det största hotet mot honom,
 *    i stället för lagets gemensamma mål.
 *  - Om ledaren är död: blir en vanlig krigare och skjuter på lagets gemensamma mål.
 */
public class LeoTeamRobot extends TeamRobot {

    /** Ledarens klassnamn. Hans robotnamn i striden innehåller den här texten. */
    private static final String LEADER_CLASS = "PeterTeamRobot";
    /** Avståndet till ledaren som livvakten försöker hålla (pixlar). */
    private static final double GUARD_DISTANCE = 120;
    private static final int MAX_ENEMY_AGE = 30;
    private static final double TEAMMATE_SAFETY_DISTANCE = 40;
    private static final double WALL_MARGIN = 60;

    private final Map<String, Point2D.Double> teammates = new HashMap<>();
    private final Map<String, EnemyPosition> enemies = new HashMap<>();

    private int moveDirection = 1;

    @Override
    public void run() {
        setColors(new Color(20, 50, 140), Color.CYAN, Color.WHITE);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            setTurnRadarRight(Double.POSITIVE_INFINITY);

            sendToTeam(new TeammatePosition(getX(), getY()));

            Point2D.Double leader = findLeader();
            EnemyPosition target = chooseTarget(leader);

            if (target != null) {
                aimAndFire(target);
            }

            if (avoidWalls()) {
                // väggen först
            } else if (leader != null) {
                guard(leader, target);
            } else if (target != null) {
                circle(target);
            } else {
                setTurnRight(20);
                setAhead(50);
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
    //  Ledaren och målval
    // ======================================================================

    /** Ledarens senaste position, eller null om han är död eller inte med i laget. */
    private Point2D.Double findLeader() {
        for (Map.Entry<String, Point2D.Double> entry : teammates.entrySet()) {
            if (entry.getKey().contains(LEADER_CLASS)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Lever ledaren: fienden närmast ledaren.
     * Annars: lagets gemensamma mål (lägst energi).
     */
    private EnemyPosition chooseTarget(Point2D.Double leader) {
        EnemyPosition best = null;
        for (EnemyPosition enemy : enemies.values()) {
            if (getTime() - enemy.time() > MAX_ENEMY_AGE) {
                continue;
            }
            if (best == null) {
                best = enemy;
            } else if (leader != null) {
                if (leader.distance(enemy.x(), enemy.y()) < leader.distance(best.x(), best.y())) {
                    best = enemy;
                }
            } else if (enemy.energy() < best.energy()
                    || (enemy.energy() == best.energy() && enemy.name().compareTo(best.name()) < 0)) {
                best = enemy;
            }
        }
        return best;
    }

    // ======================================================================
    //  Sikte och rörelse
    // ======================================================================

    private void aimAndFire(EnemyPosition target) {
        double distance = Point2D.distance(getX(), getY(), target.x(), target.y());

        double power = Math.min(3.0, 500 / distance);
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

    private Point2D.Double predictPosition(EnemyPosition enemy, double ticks) {
        double x = enemy.x() + Math.sin(enemy.heading()) * enemy.velocity() * ticks;
        double y = enemy.y() + Math.cos(enemy.heading()) * enemy.velocity() * ticks;
        x = Math.max(18, Math.min(getBattleFieldWidth() - 18, x));
        y = Math.max(18, Math.min(getBattleFieldHeight() - 18, y));
        return new Point2D.Double(x, y);
    }

    /**
     * Livvaktens rörelse: håll dig nära ledaren, helst på den sida där hotet finns,
     * så att du står mellan ledaren och fienden.
     */
    private void guard(Point2D.Double leader, EnemyPosition threat) {
        // Var vill vi stå? GUARD_DISTANCE från ledaren, i riktning mot hotet.
        double guardX = leader.x;
        double guardY = leader.y;
        if (threat != null) {
            double angleToThreat = Math.atan2(threat.x() - leader.x, threat.y() - leader.y);
            guardX += Math.sin(angleToThreat) * GUARD_DISTANCE;
            guardY += Math.cos(angleToThreat) * GUARD_DISTANCE;
        }

        double distanceToSpot = Point2D.distance(getX(), getY(), guardX, guardY);

        if (distanceToSpot > 40) {
            // Kör till platsen. Kortaste svängen: framåt eller bakåt.
            double angleToSpot = Math.atan2(guardX - getX(), guardY - getY());
            double turn = Utils.normalRelativeAngle(angleToSpot - getHeadingRadians());
            if (Math.abs(turn) > Math.PI / 2) {
                turn = Utils.normalRelativeAngle(turn + Math.PI);
                setAhead(-distanceToSpot);
            } else {
                setAhead(distanceToSpot);
            }
            setTurnRightRadians(turn);
        } else {
            // Framme: rör dig lite fram och tillbaka så att du inte står helt stilla.
            if (Math.random() < 0.1) {
                moveDirection = -moveDirection;
            }
            setAhead(40 * moveDirection);
        }
    }

    /** Om ledaren är död: cirkla runt målet som en vanlig krigare. */
    private void circle(EnemyPosition target) {
        if (Math.random() < 0.05) {
            moveDirection = -moveDirection;
        }
        double angleToTarget = Math.atan2(target.x() - getX(), target.y() - getY());
        double desiredHeading = angleToTarget + Math.PI / 2 - 0.25 * moveDirection;
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
