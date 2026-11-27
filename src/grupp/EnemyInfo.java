package grupp5;

import java.io.Serializable;

/**
 * Allt laget vet om en motståndare.
 *
 * Används på två sätt:
 *  1. Varje robot har en ArrayList<EnemyInfo> med de motståndare den känner till.
 *  2. Skickas som meddelande till lagkamraterna (därför Serializable).
 */
public class EnemyInfo implements Serializable {

    private final String name;
    private final double x;
    private final double y;
    private final double heading;   // färdriktning i grader
    private final double velocity;  // fart, negativ när den backar
    private final double energy;
    private final long lastSeen;    // turen då den sågs (getTime())

    public EnemyInfo(String name, double x, double y, double heading,
                     double velocity, double energy, long lastSeen) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.velocity = velocity;
        this.energy = energy;
        this.lastSeen = lastSeen;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHeading() {
        return heading;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getEnergy() {
        return energy;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    /** Avståndet från en punkt (t.ex. vår egen robot) till motståndaren. */
    public double distanceTo(double fromX, double fromY) {
        return Math.hypot(x - fromX, y - fromY);
    }

    /** Är informationen för gammal för att lita på? */
    public boolean isOlderThan(long now, int maxAge) {
        return now - lastSeen > maxAge;
    }
}
