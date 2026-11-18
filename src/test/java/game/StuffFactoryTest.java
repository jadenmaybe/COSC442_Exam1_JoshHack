package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StuffFactoryTest {

    private World world;
    private StuffFactory factory;
    private Creature creature;

    @BeforeEach
    void setUp() {
        world = new World(floorTiles(15, 2));
        factory = new StuffFactory(world);
        creature = new Creature(world, '@', Color.WHITE, "tester", 100, 10, 5);
        creature.x = 7;
        creature.y = 7;
        creature.z = 0;
        new CreatureAi(creature);
        world.add(creature);
    }

    @Test
    void basicItemFactories_createExpectedBreadFruitAndWeapons() {
        Item rock = factory.newRock(0);
        Item bread = factory.newBread(0);
        Item fruit = factory.newFruit(0);
        Item dagger = factory.newDagger(0);
        Item sword = factory.newSword(0);
        Item staff = factory.newStaff(0);
        Item bow = factory.newBow(0);
        Item baguette = factory.newEdibleWeapon(0);

        assertItem(rock, ',', "rock", 0, 0, 0, 6, 0);
        assertItem(bread, '%', "bread", 0, 0, 400, 1, 0);
        assertItem(fruit, '%', "apple", 0, 0, 100, 1, 0);
        assertItem(dagger, ')', "dagger", 5, 0, 0, 6, 0);
        assertItem(sword, ')', "sword", 10, 0, 0, 4, 0);
        assertItem(staff, ')', "staff", 5, 3, 0, 4, 0);
        assertItem(bow, ')', "bow", 1, 0, 0, 1, 5);
        assertItem(baguette, ')', "baguette", 3, 0, 100, 1, 0);
    }

    @ParameterizedTest
    @MethodSource("armorFactories")
    void armorFactories_createExpectedArmor(String name, int defense) {
        Item armor;
        if ("tunic".equals(name))
            armor = factory.newLightArmor(0);
        else if ("chainmail".equals(name))
            armor = factory.newMediumArmor(0);
        else
            armor = factory.newHeavyArmor(0);

        assertEquals('[', armor.glyph());
        assertEquals(name, armor.name());
        assertEquals(defense, armor.defenseValue());
        assertEquals(0, armor.attackValue());
        assertTrue(contains(armor));
    }

    private static Stream<Arguments> armorFactories() {
        return Stream.of(
                Arguments.of("tunic", 2),
                Arguments.of("chainmail", 4),
                Arguments.of("platemail", 6));
    }

    @Test
    void victoryItem_hasExpectedIdentityAndPlacement() {
        Item item = factory.newVictoryItem(1);

        assertEquals('*', item.glyph());
        assertEquals("teddy bear", item.name());
        assertEquals(0, item.foodValue());
        assertTrue(contains(item));
        assertTrue(containsAtDepth(item, 1));
    }

    @Test
    void creatureFactories_createExpectedCreaturesAtRequestedDepth() {
        Creature fungus = factory.newFungus(1);
        Creature bat = factory.newBat(1);
        Creature zombie = factory.newZombie(1, creature);

        assertCreature(fungus, 'f', "fungus", 10, 0, 0);
        assertCreature(bat, 'b', "bat", 15, 5, 0);
        assertCreature(zombie, 'z', "zombie", 50, 10, 10);
        assertEquals(1, fungus.z);
        assertEquals(1, bat.z);
        assertEquals(1, zombie.z);
    }

    @Test
    void newPlayer_wiresPlayerStateAndPlacesPlayer() {
        List<String> messages = new ArrayList<>();
        FieldOfView fov = new FieldOfView(world);

        Creature player = factory.newPlayer(messages, fov);

        assertCreature(player, '@', "player", 100, 20, 5);
        assertEquals(0, player.z);
        assertSame(player, world.creature(player.x, player.y, player.z));
    }

    @Test
    void newGoblin_createsEquippedGoblin() {
        Creature goblin = factory.newGoblin(1, creature);

        assertEquals('g', goblin.glyph());
        assertEquals("goblin", goblin.name());
        assertEquals(66, goblin.maxHp());
        assertEquals(66, goblin.hp());
        assertTrue(goblin.attackValue() >= 15);
        assertTrue(goblin.defenseValue() >= 5);
        assertNotNull(goblin.weapon());
        assertNotNull(goblin.armor());
        assertTrue(contains(goblin));
        assertEquals(1, goblin.z);
    }

    @Test
    void potionOfHealth_restoresDamagedCreatureButNotBeyondMaximum() {
        Item potion = factory.newPotionOfHealth(0);
        creature.modifyHp(-20, "damage");

        potion.quaffEffect().start(creature);

        assertEquals(95, creature.hp());
        potion.quaffEffect().start(creature);
        assertEquals(100, creature.hp());
    }

    @Test
    void potionOfMana_restoresDepletedManaButNotBeyondMaximum() {
        Item potion = factory.newPotionOfMana(0);
        creature.modifyMana(-5);

        potion.quaffEffect().start(creature);

        assertEquals(5, creature.mana());
        creature.modifyMaxMana(10);
        potion.quaffEffect().start(creature);
        assertEquals(15, creature.mana());
    }

    @Test
    void slowHealthPotion_healsOncePerUpdate() {
        Item potion = factory.newPotionOfSlowHealth(0);
        creature.modifyHp(-10, "damage");

        potion.quaffEffect().start(creature);
        potion.quaffEffect().update(creature);

        assertEquals(91, creature.hp());
    }

    @Test
    void poisonPotion_damagesOncePerUpdate() {
        Item potion = factory.newPotionOfPoison(0);
        int startingHp = creature.hp();

        potion.quaffEffect().start(creature);
        potion.quaffEffect().update(creature);

        assertEquals(startingHp - 1, creature.hp());
    }

    @Test
    void warriorPotion_appliesAndRemovesCombatModifiers() {
        Item potion = factory.newPotionOfWarrior(0);
        int startingAttack = creature.attackValue();
        int startingDefense = creature.defenseValue();

        potion.quaffEffect().start(creature);
        assertEquals(startingAttack + 5, creature.attackValue());
        assertEquals(startingDefense + 5, creature.defenseValue());

        potion.quaffEffect().end(creature);
        assertEquals(startingAttack, creature.attackValue());
        assertEquals(startingDefense, creature.defenseValue());
    }

    @Test
    void archerPotion_appliesAndRemovesVisionModifier() {
        Item potion = factory.newPotionOfArcher(0);
        int startingVision = creature.visionRadius();

        potion.quaffEffect().start(creature);
        assertEquals(startingVision + 3, creature.visionRadius());

        potion.quaffEffect().end(creature);
        assertEquals(startingVision, creature.visionRadius());
    }

    @Test
    void experiencePotion_grantsExperience() {
        Item potion = factory.newPotionOfExperience(0);

        potion.quaffEffect().start(creature);

        assertEquals(5, creature.xp());
    }

    @Test
    void whiteSpellbook_containsExpectedSpellsAndCosts() {
        Item book = factory.newWhiteMagesSpellbook(0);

        assertEquals(Arrays.asList("minor heal", "major heal", "slow heal", "inner strength"),
                spellNames(book));
        assertEquals(Arrays.asList(4, 8, 12, 16), spellCosts(book));
        assertTrue(contains(book));
    }

    @Test
    void blueSpellbook_containsExpectedSpellsAndCosts() {
        Item book = factory.newBlueMagesSpellbook(0);

        assertEquals(Arrays.asList("blood to mana", "blink", "summon bats", "detect creatures"),
                spellNames(book));
        assertEquals(Arrays.asList(1, 6, 11, 16), spellCosts(book));
        assertTrue(contains(book));
    }

    @Test
    void spellbookEffects_areActiveForTheirConfiguredDurations() {
        Item whiteBook = factory.newWhiteMagesSpellbook(0);
        Item blueBook = factory.newBlueMagesSpellbook(0);

        assertFalse(whiteBook.writtenSpells().get(0).effect().isDone());
        assertFalse(whiteBook.writtenSpells().get(2).effect().isDone());
        assertFalse(blueBook.writtenSpells().get(3).effect().isDone());
    }

    @ParameterizedTest
    @MethodSource("weaponBranches")
    void randomWeapon_coversEveryBranch(int randomValue, String expectedName) {
        StuffFactory controlledFactory = new StuffFactory(world, new StubRandom(randomValue));

        Item weapon = controlledFactory.randomWeapon(1);

        assertEquals(expectedName, weapon.name());
        assertTrue(containsAtDepth(weapon, 1));
    }

    private static Stream<Arguments> weaponBranches() {
        return Stream.of(
                Arguments.of(0, "dagger"),
                Arguments.of(1, "sword"),
                Arguments.of(2, "bow"),
                Arguments.of(3, "staff"));
    }

    @ParameterizedTest
    @MethodSource("armorBranches")
    void randomArmor_coversEveryBranch(int randomValue, String expectedName) {
        StuffFactory controlledFactory = new StuffFactory(world, new StubRandom(randomValue));

        Item armor = controlledFactory.randomArmor(1);

        assertEquals(expectedName, armor.name());
        assertTrue(containsAtDepth(armor, 1));
    }

    private static Stream<Arguments> armorBranches() {
        return Stream.of(
                Arguments.of(0, "tunic"),
                Arguments.of(1, "chainmail"),
                Arguments.of(2, "platemail"));
    }

    @ParameterizedTest
    @MethodSource("potionBranches")
    void randomPotion_coversEveryBranch(int randomValue, String expectedName) {
        StuffFactory controlledFactory = new StuffFactory(world, new StubRandom(randomValue));

        Item potion = controlledFactory.randomPotion(1);

        assertEquals(expectedName, potion.name());
        assertTrue(containsAtDepth(potion, 1));
    }

    private static Stream<Arguments> potionBranches() {
        return Stream.of(
                Arguments.of(0, "health potion"),
                Arguments.of(1, "health potion"),
                Arguments.of(2, "mana potion"),
                Arguments.of(3, "mana potion"),
                Arguments.of(4, "slow health potion"),
                Arguments.of(5, "poison potion"),
                Arguments.of(6, "warrior's potion"),
                Arguments.of(7, "archers potion"),
                Arguments.of(8, "experience potion"));
    }

    @ParameterizedTest
    @MethodSource("spellBookBranches")
    void randomSpellBook_coversEveryBranch(int randomValue, String expectedName) {
        StuffFactory controlledFactory = new StuffFactory(world, new StubRandom(randomValue));

        Item spellBook = controlledFactory.randomSpellBook(1);

        assertEquals(expectedName, spellBook.name());
        assertTrue(containsAtDepth(spellBook, 1));
    }

    private static Stream<Arguments> spellBookBranches() {
        return Stream.of(
                Arguments.of(0, "white mage's spellbook"),
                Arguments.of(1, "blue mage's spellbook"));
    }

    @Test
    void randomWeapon_returnsOnlySupportedWeaponTypes() {
        Set<String> names = new HashSet<>();

        for (int i = 0; i < 15; i++)
            names.add(factory.randomWeapon(0).name());

        assertTrue(names.stream().allMatch(name ->
                Arrays.asList("dagger", "sword", "bow", "staff").contains(name)));
    }

    @Test
    void randomArmor_returnsOnlySupportedArmorTypes() {
        Set<String> names = new HashSet<>();

        for (int i = 0; i < 15; i++)
            names.add(factory.randomArmor(0).name());

        assertTrue(names.stream().allMatch(name ->
                Arrays.asList("tunic", "chainmail", "platemail").contains(name)));
    }

    @Test
    void randomPotion_returnsOnlySupportedPotionTypes() {
        Set<String> names = new HashSet<>();

        for (int i = 0; i < 25; i++)
            names.add(factory.randomPotion(0).name());

        assertTrue(names.stream().allMatch(name -> Arrays.asList(
                "health potion", "mana potion", "slow health potion", "poison potion",
                "warrior's potion", "archers potion", "experience potion").contains(name)));
    }

    @Test
    void randomSpellBook_returnsOnlySupportedSpellbooks() {
        Set<String> names = new HashSet<>();

        for (int i = 0; i < 15; i++)
            names.add(factory.randomSpellBook(0).name());

        assertTrue(names.stream().allMatch(name ->
                Arrays.asList("white mage's spellbook", "blue mage's spellbook").contains(name)));
    }

    private void assertItem(Item item, char glyph, String name, int attack, int defense, int food,
            int thrown, int ranged) {
        assertEquals(glyph, item.glyph());
        assertEquals(name, item.name());
        assertEquals(attack, item.attackValue());
        assertEquals(defense, item.defenseValue());
        assertEquals(food, item.foodValue());
        assertEquals(thrown, item.thrownAttackValue());
        assertEquals(ranged, item.rangedAttackValue());
        assertTrue(contains(item));
    }

    private void assertCreature(Creature actual, char glyph, String name, int maxHp, int attack, int defense) {
        assertEquals(glyph, actual.glyph());
        assertEquals(name, actual.name());
        assertEquals(maxHp, actual.maxHp());
        assertEquals(maxHp, actual.hp());
        assertEquals(attack, actual.attackValue());
        assertEquals(defense, actual.defenseValue());
        assertTrue(contains(actual));
    }

    private boolean contains(Item expected) {
        for (int x = 0; x < world.width(); x++)
            for (int y = 0; y < world.height(); y++)
                for (int z = 0; z < world.depth(); z++)
                    if (world.item(x, y, z) == expected)
                        return true;
        return false;
    }

    private boolean contains(Creature expected) {
        return world.creature(expected.x, expected.y, expected.z) == expected;
    }

    private boolean containsAtDepth(Item expected, int depth) {
        for (int x = 0; x < world.width(); x++)
            for (int y = 0; y < world.height(); y++)
                if (world.item(x, y, depth) == expected)
                    return true;
        return false;
    }

    private static List<String> spellNames(Item book) {
        List<String> names = new ArrayList<>();
        for (Spell spell : book.writtenSpells())
            names.add(spell.name());
        return names;
    }

    private static List<Integer> spellCosts(Item book) {
        List<Integer> costs = new ArrayList<>();
        for (Spell spell : book.writtenSpells())
            costs.add(spell.manaCost());
        return costs;
    }

    private static Tile[][][] floorTiles(int size, int depth) {
        Tile[][][] tiles = new Tile[size][size][depth];
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                for (int z = 0; z < depth; z++)
                    tiles[x][y][z] = Tile.FLOOR;
        return tiles;
    }

    private static class StubRandom extends java.util.Random {
        private final int value;

        private StubRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
