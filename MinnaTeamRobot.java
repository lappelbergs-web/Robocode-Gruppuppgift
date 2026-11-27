package grupp5;

import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * MinnaTeamRobot - lagets Tank.
 *
 * Slåss på nära håll med hårda skott. Missar hon ofta går hon ännu närmare.
 * Krockar hon med en motståndare kör hon rakt på den en stund (ramning ger extra poäng).
 * Alla vinklar är i grader.
 */
public class MinnaTeamRobot extends TeamRobot {

    private static final double WALL_MARGIN = 60;        // pixlar från väggen
    private static final double SAFETY_DISTANCE = 40;    // pixlar från skottlinjen
    private static final int MAX_ENEMY_AGE = 30;         // turer
    private static final int RAM_TURNS = 20;             // så länge hon ramar efter en krock

    // ---------- Minne ----------
    private final ArrayList<EnemyInfo> enemies = new ArrayList<>();
    private final ArrayList<TeammateInfo> teammates = new ArrayList<>();
    private int moveDirection = 1;   // 1 = framåt, -1 = bakåt
    private int missedShots = 0;     // missade skott denna runda, se preferredDistance()
    private int ramTurnsLeft = 0;    // > 0 betyder att vi ramar just nu

    @Override
    public void run() {
        setUp();
        while (true) {
            setTurnRadarRight(Double.POSITIVE_INFINITY);
            shareWithTeam(new TeammateInfo(getName(), getX(), getY()));

            EnemyInfo target = chooseTarget();
            if (target != null) {
                aimAndFire(target);
            }
            move(target);
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
        reverseDirection();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        if (isTeammate(e.getName())) {
            reverseDirection();          // lagkamrat: backa undan
        } else {
            ramTurnsLeft = RAM_TURNS;    // motståndare: kör på den
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        reverseDirection();
        ramTurnsLeft = 0;   // sluta rama om vi kört in i väggen
    }

    @Override
    public void onBulletMissed(BulletMissedEvent e) {
        missedShots++;
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        removeRobot(e.getName());
    }

    // ======================================================================
    //  Inställningar
    // ======================================================================

    private void setUp() {
        setColors(new Color(150, 40, 150), Color.PINK, Color.WHITE);
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
    //  Målval och skjutning
    // ======================================================================

    /** Lagets gemensamma regel: motståndaren med lägst energi (vid lika energi: namnet). */
    private EnemyInfo chooseTarget() {
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

    /** Tanken skjuter hårt: full styrka ända ut till 300 pixlar. Sparar energi när den är låg. */
    private double choosePower(double distance) {
        double power = Math.min(3.0, 900 / distance);
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

    private void move(EnemyInfo target) {
        if (isNearWall()) {
            turnTowardCenter();
        } else if (target != null && ramTurnsLeft > 0) {
            ramTurnsLeft--;
            driveStraightAt(target);
        } else if (target != null) {
            circleAround(target);
        } else {
            setTurnRight(20);
            setAhead(50);
        }
    }

    /** Ramning: kör rakt mot målet. */
    private void driveStraightAt(EnemyInfo target) {
        double angle = angleTo(target.getX(), target.getY());
        setTurnRight(Utils.normalRelativeAngleDegrees(angle - getHeading()));
        setAhead(100);
    }

    /** Kör i sidled runt målet och håll avståndet från preferredDistance(). */
    private void circleAround(EnemyInfo target) {
        if (Math.random() < 0.05) {
            reverseDirection();
        }

        double distance = target.distanceTo(getX(), getY());
        double closeIn = 0;
        if (distance > preferredDistance() + 50) {
            closeIn = 30;    // för långt bort: närma dig
        } else if (distance < preferredDistance() - 50) {
            closeIn = -30;   // för nära: dra dig undan
        }

        double heading = angleTo(target.getX(), target.getY()) + 90 - closeIn * moveDirection;
        setTurnRight(Utils.normalRelativeAngleDegrees(heading - getHeading()));
        setAhead(120 * moveDirection);
    }

    /** Minnet påverkar beteendet: har vi missat många skott går vi närmare. */
    private double preferredDistance() {
        if (missedShots >= 5) {
            return 150;
        }
        return 200;
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
