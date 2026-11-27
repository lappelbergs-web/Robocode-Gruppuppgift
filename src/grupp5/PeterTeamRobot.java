package grupp5;

import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * PeterTeamRobot - lagets Ledare.
 *
 * Står först i laget och startar därför med 200 i energi.
 * Spanar med radarn, delar allt den ser med laget och cirklar runt målet.
 * Alla vinklar är i grader.
 */
public class PeterTeamRobot extends TeamRobot {

    private static final double WALL_MARGIN = 60;        // pixlar från väggen
    private static final double SAFETY_DISTANCE = 40;    // pixlar från skottlinjen
    private static final int MAX_ENEMY_AGE = 30;         // turer

    // ---------- Minne ----------
    private final ArrayList<EnemyInfo> enemies = new ArrayList<>();
    private final ArrayList<TeammateInfo> teammates = new ArrayList<>();
    private int moveDirection = 1;   // 1 = framåt, -1 = bakåt
    private int hitsTaken = 0;       // antal gånger vi blivit träffade denna runda

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
        hitsTaken++;          // påverkar avståndet vi håller, se preferredDistance()
        reverseDirection();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        reverseDirection();
        setAhead(80 * moveDirection);   // backa ifrån
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
        setColors(Color.DARK_GRAY, Color.RED, Color.ORANGE);
        // Låt kanon och radar vrida sig oberoende av kroppen.
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
    }

    // ======================================================================
    //  Minne: listorna med motståndare och lagkamrater
    // ======================================================================

    /** Gör om en radarträff till ett EnemyInfo med motståndarens position på slagfältet. */
    private EnemyInfo createEnemyInfo(ScannedRobotEvent e) {
        double angle = Math.toRadians(getHeading() + e.getBearing());
        double x = getX() + e.getDistance() * Math.sin(angle);
        double y = getY() + e.getDistance() * Math.cos(angle);
        return new EnemyInfo(e.getName(), x, y, e.getHeading(),
                e.getVelocity(), e.getEnergy(), getTime());
    }

    /** Lägg till motståndaren, eller ersätt den gamla informationen om den nya är nyare. */
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

    /** En robot har dött: ta bort den ur båda listorna. Baklänges så att indexen stämmer. */
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

    /**
     * Lagets gemensamma regel: motståndaren med lägst energi.
     * Vid lika energi avgör namnet, så att alla i laget väljer samma mål.
     */
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

    /** Hårt på nära håll, svagt långt bort. Sparar energi när vi själva har lite kvar. */
    private double choosePower(double distance) {
        double power = Math.min(3.0, 400 / distance);
        power = Math.min(power, getEnergy() / 10);
        return Math.max(0.1, power);
    }

    private void aimAndFire(EnemyInfo target) {
        double distance = target.distanceTo(getX(), getY());
        double power = choosePower(distance);

        // Var är målet när kulan kommer fram? Kulans fart är 20 - 3 * styrkan.
        double flightTime = distance / (20 - 3 * power);
        double ticks = (getTime() - target.getLastSeen()) + flightTime;
        double aimX = predictX(target, ticks);
        double aimY = predictY(target, ticks);

        // Skjut när kanonen är sval, pekar nästan rätt och ingen lagkamrat är i vägen.
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10
                && !isTeammateInLineOfFire(aimX, aimY)) {
            setFire(power);
        }
        setTurnGunRight(Utils.normalRelativeAngleDegrees(angleTo(aimX, aimY) - getGunHeading()));
    }

    /** Var är målet i x-led om 'ticks' turer, om det fortsätter rakt fram? */
    private double predictX(EnemyInfo enemy, double ticks) {
        double x = enemy.getX() + Math.sin(Math.toRadians(enemy.getHeading())) * enemy.getVelocity() * ticks;
        return Math.max(18, Math.min(getBattleFieldWidth() - 18, x));   // inte genom väggen
    }

    private double predictY(EnemyInfo enemy, double ticks) {
        double y = enemy.getY() + Math.cos(Math.toRadians(enemy.getHeading())) * enemy.getVelocity() * ticks;
        return Math.max(18, Math.min(getBattleFieldHeight() - 18, y));
    }

    /** Står någon lagkamrat nära linjen mellan oss och punkten vi siktar på? */
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
        } else if (target != null) {
            circleAround(target);
        } else {
            setTurnRight(20);   // inget mål än: kör runt och leta
            setAhead(50);
        }
    }

    /**
     * Kör i sidled runt målet (svårast att träffa) och håll rätt avstånd.
     * 90 grader mot målet = rakt i sidled. closeIn vinklar oss in mot eller bort från målet.
     */
    private void circleAround(EnemyInfo target) {
        if (Math.random() < 0.05) {
            reverseDirection();   // ibland: svårare att förutsäga
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

    /** Minnet påverkar beteendet: har vi blivit träffade ofta håller vi större avstånd. */
    private double preferredDistance() {
        if (hitsTaken >= 3) {
            return 400;
        }
        return 250;
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
