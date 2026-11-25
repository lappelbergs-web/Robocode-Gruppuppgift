package grupp;

import java.io.Serializable;

/**
 * Meddelande "där är fienden": skickas till laget när en robot ser en fiende.
 * Innehåller även riktning, fart och energi så att alla kan förutsäga var
 * fienden är på väg och välja samma mål.
 *
 * @param name     fiendens namn
 * @param x        x-position när den sågs
 * @param y        y-position när den sågs
 * @param heading  fiendens färdriktning i radianer
 * @param velocity fiendens fart (negativ = backar)
 * @param energy   fiendens energi
 * @param time     turen då den sågs (getTime(), samma klocka för alla robotar)
 */
public record EnemyPosition(String name, double x, double y,
                            double heading, double velocity,
                            double energy, long time) implements Serializable {
}
