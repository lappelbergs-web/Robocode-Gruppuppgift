package grupp5;

import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * LeoTeamRobot - lagets Livvakt.
 *
 * Håller sig nära ledaren (PeterTeamRobot), på den sida där hotet finns,
 * och skjuter på motståndaren som är närmast ledaren.
 * Har Leo blivit träffad många gånger, eller är ledaren död, slutar han vakta
 * och slåss som en vanlig krigare i stället.
 * Alla vinklar är i grader.
 */
public class LeoTeamRobot extends TeamRobot {

    private static final String LEADER_NAME = "PeterTeamRobot"; // ingår i ledarens namn i striden
    private static final double WALL_MARGIN = 60;        // pixlar från väggen
    private static final double SAFETY_DISTANCE = 40;    // pixlar från skottlinjen
    private static final int MAX_ENEMY_AGE = 30;         // turer
    private static final double GUARD_DISTANCE = 120;    // avstånd till ledaren
    private static final int MAX_HITS_AS_GUARD = 4;      // sedan slutar vi vakta

    // ---------- Minne ----------
    private final ArrayList<EnemyInfo> enemies = new ArrayList<>();
    private final ArrayList<TeammateInfo> teammates = new ArrayList<>();
    private int moveDirection = 1;   // 1 = framåt, -1 = bakåt
    private int hitsTaken = 0;       // antal träffar denna runda, se isGuarding()

    @Override
    public void run() {
        setUp();
        while (true) {
            setTurnRadarRight(Double.POSITIVE_INFINITY);
            shareWithTeam(new TeammateInfo(getName(), getX(), getY()));

            TeammateInfo leader = findLeader();
            EnemyInfo target = chooseTarget(leader);
            if (target != null) {
                aimAndFire(target);
            }
            move(leader, target);
            execute();
        }
    }

    // ======================================================================
    //  Händelser - korta, anropar våra egna metoder
    // ======================================================================

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if (isTeammate(e.getName())) {
            return;   // aldrig ett mål
        }
        EnemyInfo enemy = createEnemyInfo(e);
        updateEnemy(enemy);
        shareWithTeam(enemy);
    }

    @Override
    public void onMessageReceived(MessageEvent e) {
        if (e.getMessage() instanceof EnemyInfo) {
            updateEnemy((EnemyInfo) e.getMessage());
        } else if (e.getMessage() instanceof TeammateInfo) {
            updateTeammate((TeammateInfo) e.getMessage());
        }
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        hitsTaken++;          // efter MAX_HITS_AS_GUARD träffar slutar vi vakta
        reverseDirection();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        reverseDirection();
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        reverseDirection();
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        removeRobot(e.getName());
    }

    // ======================================================================
    //  Inställningar
    // ======================================================================

    private void setUp() {
        setColors(new Color(20, 50, 140), Color.CYAN, Color.WHITE);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
    }

    // ======================================================================
    //  Minne: listorna med motståndare och lagkamrater
    // ======================================================================

    private EnemyInfo createEnemyInfo(ScannedRobotEvent e) {
        double angle = Math.toRadians(getHeading() + e.getBearing());
        double x = getX() + e.getDistance() * Math.sin(angle);
        double y = getY() + e.getDistance() * Math.cos(angle);
        return new EnemyInfo(e.getName(), x, y, e.getHeading(),
                e.getVelocity(), e.getEnergy(), getTime());
    }

    private void updateEnemy(EnemyInfo enemy) {
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).getName().equals(enemy.getName())) {
                if (enemy.getLastSeen() > enemies.get(i).getLastSeen()) {
                    enemies.set(i, enemy);
                }
                return;
            }
        }
        enemies.add(enemy);
    }

    private void updateTeammate(TeammateInfo teammate) {
        if (teammate.getName().equals(getName())) {
            return;   // vår egen position: då skulle vi aldrig våga skjuta
        }
        for (int i = 0; i < teammates.size(); i++) {
            if (teammates.get(i).getName().equals(teammate.getName())) {
                teammates.set(i, teammate);
                return;
            }
        }
        teammates.add(teammate);
    }

    private void removeRobot(String name) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            if (enemies.get(i).getName().equals(name)) {
                enemies.remove(i);
            }
        }
        for (int i = teammates.size() - 1; i >= 0; i--) {
            if (teammates.get(i).getName().equals(name)) {
                teammates.remove(i);
            }
        }
    }

    // ======================================================================
    //  Ledaren och målval
    // ======================================================================

    /** Ledaren i listan över lagkamrater, eller null om han är död. */
    private TeammateInfo findLeader() {
        for (TeammateInfo teammate : teammates) {
            if (teammate.getName().contains(LEADER_NAME)) {
                return teammate;
            }
        }
        return null;
    }

    /** Vaktar vi ledaren: hotet närmast honom. Annars: lagets gemensamma mål. */
    private EnemyInfo chooseTarget(TeammateInfo leader) {
        if (isGuarding(leader)) {
            return closestEnemyTo(leader.getX(), leader.getY());
        }
        return lowestEnergyEnemy();
    }

    /** Vaktar vi? Bara om ledaren lever och vi inte blivit träffade för många gånger. */
    private boolean isGuarding(TeammateInfo leader) {
        return leader != null && hitsTaken < MAX_HITS_AS_GUARD;
    }

    private EnemyInfo closestEnemyTo(double x, double y) {
        EnemyInfo closest = null;
        for (EnemyInfo enemy : enemies) {
            if (enemy.isOlderThan(getTime(), MAX_ENEMY_AGE)) {
                continue;
            }
            if (closest == null || enemy.distanceTo(x, y) < closest.distanceTo(x, y)) {
                closest = enemy;
            }
        }
        return closest;
    }

    /** Lagets gemensamma regel: motståndaren med lägst energi (vid lika energi: namnet). */
    private EnemyInfo lowestEnergyEnemy() {
        EnemyInfo best = null;
        for (EnemyInfo enemy : enemies) {
            if (enemy.isOlderThan(getTime(), MAX_ENEMY_AGE)) {
                continue;
            }
            if (best == null || enemy.getEnergy() < best.getEnergy()
                    || (enemy.getEnergy() == best.getEnergy()
                        && enemy.getName().compareTo(best.getName()) < 0)) {
                best = enemy;
            }
        }
        return best;
    }

    // ======================================================================
    //  Skjutning
    // ======================================================================

    /** Hårt på nära håll, svagt långt bort. Sparar energi när vi själva har lite kvar. */
    private double choosePower(double distance) {
        double power = Math.min(3.0, 500 / distance);
        power = Math.min(power, getEnergy() / 10);
        return Math.max(0.1, power);
    }

    private void aimAndFire(EnemyInfo target) {
        double distance = target.distanceTo(getX(), getY());
        double power = choosePower(distance);

        double flightTime = distance / (20 - 3 * power);
        double ticks = (getTime() - target.getLastSeen()) + flightTime;
        double aimX = predictX(target, ticks);
        double aimY = predictY(target, ticks);

        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10
                && !isTeammateInLineOfFire(aimX, aimY)) {
            setFire(power);
        }
        setTurnGunRight(Utils.normalRelativeAngleDegrees(angleTo(aimX, aimY) - getGunHeading()));
    }

    private double predictX(EnemyInfo enemy, double ticks) {
        double x = enemy.getX() + Math.sin(Math.toRadians(enemy.getHeading())) * enemy.getVelocity() * ticks;
        return Math.max(18, Math.min(getBattleFieldWidth() - 18, x));
    }

    private double predictY(EnemyInfo enemy, double ticks) {
        double y = enemy.getY() + Math.cos(Math.toRadians(enemy.getHeading())) * enemy.getVelocity() * ticks;
        return Math.max(18, Math.min(getBattleFieldHeight() - 18, y));
    }

    private boolean isTeammateInLineOfFire(double targetX, double targetY) {
        Line2D.Double lineOfFire = new Line2D.Double(getX(), getY(), targetX, targetY);
        for (TeammateInfo teammate : teammates) {
            if (lineOfFire.ptSegDist(teammate.getX(), teammate.getY()) < SAFETY_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    // ======================================================================
    //  Rörelse
    // ======================================================================

    private void move(TeammateInfo leader, EnemyInfo target) {
        if (isNearWall()) {
            turnTowardCenter();
        } else if (isGuarding(leader)) {
            guard(leader, target);
        } else if (target != null) {
            circleAround(target);
        } else {
            setTurnRight(20);
            setAhead(50);
        }
    }

    /** Ställ dig GUARD_DISTANCE från ledaren, mellan honom och hotet. */
    private void guard(TeammateInfo leader, EnemyInfo threat) {
        double spotX = leader.getX();
        double spotY = leader.getY();
        if (threat != null) {
            // Vinkeln (radianer) från ledaren till hotet.
            double angle = Math.atan2(threat.getX() - leader.getX(), threat.getY() - leader.getY());
            spotX += Math.sin(angle) * GUARD_DISTANCE;
            spotY += Math.cos(angle) * GUARD_DISTANCE;
        }
        goTo(spotX, spotY);
    }

    /**
     * Kör till en punkt. Ligger punkten bakom oss backar vi dit i stället för
     * att vända hela roboten, det går snabbare.
     */
    private void goTo(double x, double y) {
        double distance = Math.hypot(x - getX(), y - getY());
        if (distance < 40) {
            // Framme: rör dig lite fram och tillbaka så att du inte står helt stilla.
            setAhead(40 * moveDirection);
            return;
        }

        double turn = Utils.normalRelativeAngleDegrees(angleTo(x, y) - getHeading());
        if (Math.abs(turn) > 90) {
            turn = Utils.normalRelativeAngleDegrees(turn + 180);
            setAhead(-distance);
        } else {
            setAhead(distance);
        }
        setTurnRight(turn);
    }

    /** Om vi inte vaktar: kör i sidled runt målet som en vanlig krigare. */
    private void circleAround(EnemyInfo target) {
        if (Math.random() < 0.05) {
            reverseDirection();
        }
        double heading = angleTo(target.getX(), target.getY()) + 90;
        setTurnRight(Utils.normalRelativeAngleDegrees(heading - getHeading()));
        setAhead(120 * moveDirection);
    }

    private boolean isNearWall() {
        return getX() < WALL_MARGIN || getX() > getBattleFieldWidth() - WALL_MARGIN
                || getY() < WALL_MARGIN || getY() > getBattleFieldHeight() - WALL_MARGIN;
    }

    private void turnTowardCenter() {
        double center = angleTo(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2);
        setTurnRight(Utils.normalRelativeAngleDegrees(center - getHeading()));
        moveDirection = 1;
        setAhead(100);
    }

    private void reverseDirection() {
        moveDirection = -moveDirection;
    }

    // ======================================================================
    //  Hjälpmetoder
    // ======================================================================

    /** Vinkeln (grader, 0 = norr) från vår robot till en punkt. */
    private double angleTo(double x, double y) {
        return Math.toDegrees(Math.atan2(x - getX(), y - getY()));
    }

    private void shareWithTeam(Serializable message) {
        try {
            broadcastMessage(message);
        } catch (IOException ex) {
            out.println("Kunde inte skicka meddelande: " + ex.getMessage());
        }
    }
}
