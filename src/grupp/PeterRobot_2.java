package grupp;

import robocode.*;
import robocode.util.Utils;

import java.awt.Color;

/**
 * PeterRobot_2 - en förbättrad version av PeterRobot_1.
 *
 * Tre huvudidéer:
 *  1. AdvancedRobot: rörelse, kanon och radar körs samtidigt (set...-metoder + execute()).
 *  2. Radarlås: radarn följer fienden hela tiden i stället för att snurra 360 grader.
 *  3. Siktar dit fienden är på väg (linjär förutsägelse) och kör i sidled runt fienden.
 */
public class PeterRobot_2 extends AdvancedRobot {

    // 1 = framåt, -1 = bakåt. Byts för att göra roboten svårare att träffa.
    private int moveDirection = 1;

    @Override
    public void run() {
        setColors(Color.DARK_GRAY, Color.RED, Color.ORANGE);

        // Låt kanon och radar röra sig oberoende av kroppen.
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        // Snurra radarn tills vi hittar någon.
        setTurnRadarRight(Double.POSITIVE_INFINITY);

        while (true) {
            // Har vi tappat fienden: börja leta igen.
            if (getRadarTurnRemaining() == 0) {
                setTurnRadarRight(Double.POSITIVE_INFINITY);
            }
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Vinkeln till fienden räknat från norr (i radianer).
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();

        // ---------- Radar: lås på fienden ----------
        double radarTurn = Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
        setTurnRadarRightRadians(radarTurn * 2); // lite överskjutning så vi inte tappar den

        // ---------- Kanon: välj skottstyrka ----------
        // Nära = starkt skott, långt bort = svagt (svaga skott är snabbare och träffar oftare).
        double power = Math.min(3.0, 400 / e.getDistance());
        // Spara energi när vi börjar ta slut, annars kan roboten bli "disabled".
        power = Math.min(power, getEnergy() / 10);
        power = Math.max(0.1, power);

        // ---------- Kanon: sikta dit fienden är på väg ----------
        double bulletSpeed = 20 - 3 * power;
        double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing);
        double gunTurn = Utils.normalRelativeAngle(
                absoluteBearing - getGunHeadingRadians() + Math.asin(lateralVelocity / bulletSpeed));
        setTurnGunRightRadians(gunTurn);

        // Skjut bara när kanonen är sval och nästan riktad rätt.
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
            setFire(power);
            out.println("PANG! styrka " + String.format("%.1f", power));
        }

        // ---------- Rörelse: kör i sidled runt fienden ----------
        // +90 grader = vinkelrätt mot fienden, svårast att träffa.
        // -15*moveDirection drar oss lite närmare så vi inte driver iväg.
        setTurnRight(e.getBearing() + 90 - 15 * moveDirection);

        // Byt riktning ibland så fienden inte kan förutsäga oss.
        if (Math.random() < 0.05) {
            moveDirection = -moveDirection;
        }
        setAhead(150 * moveDirection);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Träffad: byt riktning.
        moveDirection = -moveDirection;
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        // Studsa från väggen.
        moveDirection = -moveDirection;
        setAhead(150 * moveDirection);
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        // Krock med annan robot: backa ifrån.
        moveDirection = -moveDirection;
        setAhead(100 * moveDirection);
    }

    @Override
    public void onWin(WinEvent e) {
        // Segerdans.
        for (int i = 0; i < 20; i++) {
            turnRight(30);
            turnLeft(30);
        }
    }
}