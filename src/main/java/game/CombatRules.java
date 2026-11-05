package game;

public class CombatRules {

    /**
     * Calculates the new hit points after applying a change, ensuring it does not
     * exceed the maximum allowed.
     * 
     * @param currentHp The current hit points.
     * @param maxHp     The maximum allowable hit points.
     * @param amount    The amount of hit points to add or subtract.
     * @return The updated hit points constrained by the maximum hit points.
     */
    public static int calculateNewHp(int currentHp, int maxHp, int amount) {
        int newHp = currentHp + amount;

        if (newHp > maxHp)
            return maxHp;

        return newHp;
    }

    /**
     * Determines if an entity is dead based on its hit points.
     * 
     * @param hp The current hit points to check.
     * @return True if hit points are less than one, otherwise false.
     */
    public static boolean isDead(int hp) {
        return hp < 1;
    }
}
