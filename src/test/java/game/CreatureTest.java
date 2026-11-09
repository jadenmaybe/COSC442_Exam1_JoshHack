package game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CreatureTest {

    private World world;
    private Creature creature;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[3][3][1];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }

        world = new World(tiles);

        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);

        creature.x = 1;
        creature.y = 1;
        creature.z = 0;
    }

    @ParameterizedTest
    @CsvSource({
            "-101, ouch, -1", // take 101 damage, health should be -1
            "-100, ouch, 0", // take 100 damage, health should be 0
            "1, none, 100", // 1 healing, health should not go over max 100, alive
            "-5, hit, 95", // take 5 damage, health should be 95, should still be alive
    })
    void testModifyHp_ExpectedHealth(int amount, String causeOfDeath, int expected) {
        // Act
        creature.modifyHp(amount, causeOfDeath);
        // Assert
        assertEquals(expected, creature.hp());
    }

    @ParameterizedTest
    @CsvSource({
            "-101, ouch, true", // take 101 damage, health should be -1
            "-100, ouch, true", // take 100 damage, health should be 0
            "1, none, false", // 1 healing, health should not go over max 100, alive
            "-5, hit, false",
    })
    void testModifyHp_ExpectedDead(int amount, String causeOfDeath, boolean expected) {
        // Act
        creature.modifyHp(amount, causeOfDeath);
        // Assert
        assertEquals(expected, CombatRules.isDead(creature.hp()));
    }

    @ParameterizedTest
    @CsvSource({
            "-101, ouch, true", // dies, corpse is left
            "-100, ouch, true", // take 100 damage, health should be 0, corpse should be left
            "-99, ouch, false", // minimum health boundary, should still be alive, no corpse
            "1, none, false", // 1 healing, health should be 100, no corpse
            "-5, hit, false", // non-fatal damage, no corpse
    })
    void testModifyHp_IfDead_CheckForCorpse(int amount, String causeOfDeath, boolean expectedDead) {
        // Arrange
        creature.modifyHp(amount, causeOfDeath);
        // Act, assert
        assertEquals(expectedDead, world.item(creature.x, creature.y, creature.z) != null);

    }

    @Test
    void testArmor() {

    }

    @Test
    void testAttackValue() {

    }

    @Test
    void testCanEnter() {

    }

    @Test
    void testCanSee() {

    }

    @Test
    void testCastSpell() {

    }

    @Test
    void testCauseOfDeath() {

    }

    @Test
    void testColor() {

    }

    @Test
    void testCreature() {

    }

    @Test
    void testDefenseValue() {

    }

    @Test
    void testDetails() {

    }

    @Test
    void testDig() {

    }

    @Test
    void testDoAction() {

    }

    @Test
    void testDoAction2() {

    }

    @Test
    void testDrop() {

    }

    @Test
    void testEat() {

    }

    @Test
    void testEffects() {

    }

    @Test
    void testEquip() {

    }

    @Test
    void testFood() {

    }

    @Test
    void testGainXp() {

    }

    @Test
    void testGlyph() {

    }

    @Test
    void testHp() {

    }

    @Test
    void testInventory() {

    }

    @Test
    void testIsPlayer() {

    }

    @Test
    void testItem() {

    }

    @Test
    void testLearnName() {

    }

    @Test
    void testLevel() {

    }

    @Test
    void testMana() {

    }

    @Test
    void testMaxFood() {

    }

    @Test
    void testMaxHp() {

    }

    @Test
    void testMaxMana() {

    }

    @Test
    void testMeleeAttack() {

    }

    @Test
    void testModifyAttackValue() {

    }

    @Test
    void testModifyDefenseValue() {

    }

    @Test
    void testModifyDetectCreatures() {

    }

    @Test
    void testModifyFood() {

    }

    @Test
    void testModifyMana() {

    }

    @Test
    void testModifyMaxHp() {

    }

    @Test
    void testModifyMaxMana() {

    }

    @Test
    void testModifyRegenHpPer1000() {

    }

    @Test
    void testModifyRegenManaPer1000() {

    }

    @Test
    void testModifyVisionRadius() {

    }

    @Test
    void testModifyXp() {

    }

    @Test
    void testMoveBy() {

    }

    @Test
    void testName() {

    }

    @Test
    void testNameOf() {

    }

    @Test
    void testNotify() {

    }

    @Test
    void testPickup() {

    }

    @Test
    void testQuaff() {

    }

    @Test
    void testRangedWeaponAttack() {

    }

    @Test
    void testRealTile() {

    }

    @Test
    void testSetCreatureAi() {

    }

    @Test
    void testSummon() {

    }

    @Test
    void testThrowItem() {

    }

    @Test
    void testTile() {

    }

    @Test
    void testUnequip() {

    }

    @Test
    void testUpdate() {

    }

    @Test
    void testVisionRadius() {

    }

    @Test
    void testWeapon() {

    }

    @Test
    void testXp() {

    }
}
