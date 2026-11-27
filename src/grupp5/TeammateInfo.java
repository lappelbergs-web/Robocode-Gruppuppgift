package grupp5;

import java.io.Serializable;

/**
 * En lagkamrats position.
 *
 * Varje robot skickar ett TeammateInfo om sig själv till laget varje tur,
 * så att de andra vet var den står och inte skjuter på den.
 */
public class TeammateInfo implements Serializable {

    private final String name;
    private final double x;
    private final double y;

    public TeammateInfo(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
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
}
