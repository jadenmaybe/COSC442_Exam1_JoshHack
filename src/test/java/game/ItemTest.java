package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ItemTest {

    @Test
    void testAddWrittenSpell() {
        Item book = new Item('+', Color.WHITE, "spellbook", null);
        Effect effect = new Effect(3);

        book.addWrittenSpell("minor heal", 4, effect);

        assertEquals(1, book.writtenSpells().size());
        assertEquals("minor heal", book.writtenSpells().get(0).name());
        assertEquals(4, book.writtenSpells().get(0).manaCost());
        assertFalse(book.writtenSpells().get(0).effect().isDone());
    }

    @Test
    void testAppearance() {
        Item explicit = new Item('!', Color.RED, "health potion", "red potion");
        Item defaultAppearance = new Item('!', Color.RED, "health potion", null);

        assertEquals("red potion", explicit.appearance());
        assertEquals("health potion", defaultAppearance.appearance());
    }

    @Test
    void testAttackValue() {
        Item item = new Item(')', Color.WHITE, "sword", null);

        assertEquals(0, item.attackValue());
        item.modifyAttackValue(7);
        assertEquals(7, item.attackValue());
    }

    @Test
    void testColor() {
        Color color = Color.CYAN;
        Item item = new Item('!', color, "potion", null);

        assertSame(color, item.color());
    }

    @Test
    void testDefenseValue() {
        Item item = new Item('[', Color.WHITE, "armor", null);

        assertEquals(0, item.defenseValue());
        item.modifyDefenseValue(4);
        assertEquals(4, item.defenseValue());
    }

    @ParameterizedTest
    @MethodSource("detailCases")
    void testDetails(int attack, int thrown, int ranged, int defense, int food, String expected) {
        Item item = new Item('?', Color.WHITE, "item", null);
        item.modifyAttackValue(attack);
        item.modifyThrownAttackValue(thrown - 1);
        item.modifyRangedAttackValue(ranged);
        item.modifyDefenseValue(defense);
        item.modifyFoodValue(food);

        assertEquals(expected, item.details());
    }

    private static Stream<Arguments> detailCases() {
        return Stream.of(
                Arguments.of(0, 1, 0, 0, 0, ""),
                Arguments.of(2, 1, 0, 0, 0, "  attack:2"),
                Arguments.of(0, 3, 0, 0, 0, "  thrown:3"),
                Arguments.of(0, 1, 4, 0, 0, "  ranged:4"),
                Arguments.of(0, 1, 0, 2, 0, "  defense:2"),
                Arguments.of(0, 1, 0, 0, 10, "  food:10"),
                Arguments.of(5, 3, 4, 2, 10,
                        "  attack:5  thrown:3  ranged:4  defense:2  food:10"),
                Arguments.of(-2, -1, -1, -3, -4,
                        "  attack:-2  thrown:-1  defense:-3  food:-4"));
    }

    @ParameterizedTest
    @MethodSource("modifierCases")
    void modifierMethods_accumulateChanges(BiConsumer<Item, Integer> modifier,
            Function<Item, Integer> getter, int expected) {
        Item item = new Item('?', Color.WHITE, "item", null);

        modifier.accept(item, 5);
        modifier.accept(item, -2);

        assertEquals(expected, getter.apply(item));
    }

    private static Stream<Arguments> modifierCases() {
        return Stream.of(
                Arguments.of((BiConsumer<Item, Integer>) Item::modifyAttackValue,
                        (Function<Item, Integer>) Item::attackValue, 3),
                Arguments.of((BiConsumer<Item, Integer>) Item::modifyDefenseValue,
                        (Function<Item, Integer>) Item::defenseValue, 3),
                Arguments.of((BiConsumer<Item, Integer>) Item::modifyFoodValue,
                        (Function<Item, Integer>) Item::foodValue, 3),
                Arguments.of((BiConsumer<Item, Integer>) Item::modifyRangedAttackValue,
                        (Function<Item, Integer>) Item::rangedAttackValue, 3),
                Arguments.of((BiConsumer<Item, Integer>) Item::modifyThrownAttackValue,
                        (Function<Item, Integer>) Item::thrownAttackValue, 4));
    }

    @Test
    void testFoodValue() {
        Item item = new Item('%', Color.YELLOW, "bread", null);

        assertEquals(0, item.foodValue());
        item.modifyFoodValue(400);
        assertEquals(400, item.foodValue());
    }

    @Test
    void testGlyph() {
        Item item = new Item('*', Color.WHITE, "teddy bear", null);

        assertEquals('*', item.glyph());
    }

    @Test
    void testModifyAttackValue() {
        Item item = new Item(')', Color.WHITE, "weapon", null);

        item.modifyAttackValue(-3);

        assertEquals(-3, item.attackValue());
    }

    @Test
    void testModifyDefenseValue() {
        Item item = new Item('[', Color.WHITE, "armor", null);

        item.modifyDefenseValue(-3);

        assertEquals(-3, item.defenseValue());
    }

    @Test
    void testModifyFoodValue() {
        Item item = new Item('%', Color.YELLOW, "food", null);

        item.modifyFoodValue(-5);

        assertEquals(-5, item.foodValue());
    }

    @Test
    void testModifyRangedAttackValue() {
        Item item = new Item(')', Color.WHITE, "bow", null);

        item.modifyRangedAttackValue(6);

        assertEquals(6, item.rangedAttackValue());
    }

    @Test
    void testModifyThrownAttackValue() {
        Item item = new Item(',', Color.WHITE, "rock", null);

        item.modifyThrownAttackValue(4);

        assertEquals(5, item.thrownAttackValue());
    }

    @Test
    void testName() {
        Item item = new Item('?', Color.WHITE, "mystery item", null);

        assertEquals("mystery item", item.name());
    }

    @Test
    void testQuaffEffect() {
        Item item = new Item('!', Color.RED, "potion", null);

        assertNull(item.quaffEffect());
    }

    @Test
    void testRangedAttackValue() {
        Item item = new Item(')', Color.WHITE, "bow", null);

        item.modifyRangedAttackValue(5);

        assertEquals(5, item.rangedAttackValue());
    }

    @Test
    void testSetQuaffEffect() {
        Item item = new Item('!', Color.RED, "potion", null);
        Effect first = new Effect(1);
        Effect second = new Effect(2);

        item.setQuaffEffect(first);
        assertSame(first, item.quaffEffect());
        item.setQuaffEffect(second);
        assertSame(second, item.quaffEffect());
        item.setQuaffEffect(null);
        assertNull(item.quaffEffect());
    }

    @Test
    void testThrownAttackValue() {
        Item item = new Item(',', Color.WHITE, "rock", null);

        assertEquals(1, item.thrownAttackValue());
    }

    @Test
    void testWrittenSpells() {
        Item book = new Item('+', Color.WHITE, "spellbook", null);

        assertTrue(book.writtenSpells().isEmpty());
    }
}
