package grupp;

import java.io.Serializable;

/** Meddelande "här är jag": skickas till laget varje tur. */
public record TeammatePosition(double x, double y) implements Serializable {
}
