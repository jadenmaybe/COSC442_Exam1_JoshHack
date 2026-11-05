package game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CombatRulesTest {
    private CombatRules combatRules; // To satisfy statement coverage

    @BeforeEach
    void setUp() {
        combatRules = new CombatRules();
    }

    @AfterEach
    void tearDown() {
        combatRules = null;
    }

    // Tests to see if, with valid values, calculateNewHp returns a correct int
    // value,
    // and isDead returns a correct boolean value
    @ParameterizedTest
    @CsvSource({
            "1, 100, -5, -4", // 5 damage, should return -4. Below boundary
            "1, 100, -1, 0", // 1 damage, should return 0. 1 below boundary
            "1, 100, 0, 1", // 0 damage, should return 1. 1 above the lower boundary
            "98, 100, 1, 99", // 1 healing, should return 99, 1 below the upper boundary
            "99, 100, 1, 100", // 1 healing, healing to max, should return max
            "100, 100, 0, 100", // At max boundary
            "100, 100, 5, 100", // 5 healing, should return 100 (max). Shouldn't go above max boundary
    })
    void testCalculateNewHp_AndIsDead(int currentHp, int maxHp, int amount, int expected) {
        // Act
        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);
        // Assert
        assertEquals(expected, newHp);
    }

    // Tests to see if isDead correctly returns dead or not based on newHp
    // newHp < 1 = dead, else alive
    @ParameterizedTest
    @CsvSource({
            "-5, true", // below alive boundary, is dead
            "0, true", // 1 below alive lower boundary, is dead
            "1, false", // 1 above alive lower boundary, is alive
            "99, false", // 1 below alive upper boundary, is alive
            "100, false", // at "max"/upper boundary, is alive
            "105, false", // above the max, should still be alive
    })
    void testIsDead(int newHp, boolean expected) {
        // Act, assert
        assertEquals(expected, CombatRules.isDead(newHp));
    }
}
