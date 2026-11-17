package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CreatureTest {

    private World world;
    private Creature creature;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[5][5][1];

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }

        world = new World(tiles);
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 2;
        creature.y = 2;
        creature.z = 0;
        new CreatureAi(creature);
    }

    @ParameterizedTest
    @CsvSource({
            "-101, ouch, -1, true",
            "-100, ouch, 0, true",
            "1, none, 100, false",
            "-5, hit, 95, false"
    })
    void modifyHp_handlesHealthAndDeathBoundaries(int amount, String causeOfDeath, int expected,
            boolean expectedDead) {
        creature.modifyHp(amount, causeOfDeath);

        assertEquals(expected, creature.hp());
        assertEquals(expectedDead, CombatRules.isDead(creature.hp()));
        assertEquals(causeOfDeath, creature.causeOfDeath());
    }

    @Test
    void constructor_initializesCoreState() {
        assertEquals('@', creature.glyph());
        assertEquals(Color.GRAY, creature.color());
        assertEquals("josh", creature.name());
        assertEquals(100, creature.maxHp());
        assertEquals(100, creature.hp());
        assertEquals(10, creature.attackValue());
        assertEquals(5, creature.defenseValue());
        assertEquals(1, creature.level());
        assertEquals(5, creature.maxMana());
        assertEquals(5, creature.mana());
        assertEquals(666, creature.food());
        assertNotNull(creature.inventory());
        assertNotNull(creature.effects());
    }

    @Test
    void modifyHp_recordsCauseOfDeath() {
        creature.modifyHp(-5, "test damage");

        assertEquals("test damage", creature.causeOfDeath());
    }

    @Test
    void modifyMaxHpAndStats_changeReportedValues() {
        creature.modifyMaxHp(20);
        creature.modifyAttackValue(3);
        creature.modifyDefenseValue(2);
        creature.modifyVisionRadius(4);

        assertEquals(120, creature.maxHp());
        assertEquals(13, creature.attackValue());
        assertEquals(7, creature.defenseValue());
        assertEquals(13, creature.visionRadius());
    }

    @Test
    void combatValues_includeBothEquippedItems() {
        Item weapon = new Item(')', Color.WHITE, "weapon", null);
        weapon.modifyAttackValue(4);
        weapon.modifyDefenseValue(1);
        Item armor = new Item('[', Color.WHITE, "armor", null);
        armor.modifyAttackValue(2);
        armor.modifyDefenseValue(8);
        creature.inventory().add(weapon);
        creature.inventory().add(armor);

        creature.equip(weapon);
        creature.equip(armor);

        assertEquals(16, creature.attackValue());
        assertEquals(14, creature.defenseValue());
    }

    @ParameterizedTest
    @CsvSource({
            "10, 5",
            "-10, 0",
            "100, 5"
    })
    void modifyMana_clampsBetweenZeroAndMaximum(int amount, int expected) {
        creature.modifyMana(amount);

        assertEquals(expected, creature.mana());
    }

    @Test
    void modifyFood_starvesPlayerWhenFoodReachesZero() {
        world.add(creature);

        creature.modifyFood(-666);

        assertEquals(-900, creature.hp());
        assertNull(world.creature(creature.x, creature.y, creature.z));
    }

    @Test
    void modifyFood_doesNotStarveNonPlayerAtZero() {
        Creature monster = new Creature(world, 'g', Color.GREEN, "goblin", 100, 10, 5);
        monster.x = 2;
        monster.y = 2;
        monster.z = 0;
        new CreatureAi(monster);
        world.add(monster);

        monster.modifyFood(-666);

        assertEquals(100, monster.hp());
        assertSame(monster, world.creature(2, 2, 0));
    }

    @Test
    void equip_weaponUpdatesAttackValueAndWeaponSlot() {
        Item sword = new Item(')', Color.WHITE, "sword", null);
        sword.modifyAttackValue(10);
        creature.inventory().add(sword);

        creature.equip(sword);

        assertSame(sword, creature.weapon());
        assertEquals(20, creature.attackValue());
        assertNull(creature.armor());
    }

    @Test
    void equip_armorUpdatesDefenseValueAndArmorSlot() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item armor = new Item('[', Color.WHITE, "armor", null);
        armor.modifyDefenseValue(8);
        creature.inventory().add(armor);

        creature.equip(armor);

        assertSame(armor, creature.armor());
        assertEquals(13, creature.defenseValue());
        assertNull(creature.weapon());
        assertEquals("You put on a armor.", ai.lastMessage);
    }

    @Test
    void unequip_clearsEquippedItem() {
        Item sword = new Item(')', Color.WHITE, "sword", null);
        sword.modifyAttackValue(10);
        creature.inventory().add(sword);
        creature.equip(sword);

        creature.unequip(sword);

        assertNull(creature.weapon());
        assertEquals(10, creature.attackValue());
    }

    @Test
    void pickup_movesWorldItemIntoInventory() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item item = new Item('!', Color.RED, "potion", null);
        world.addAtEmptySpace(item, creature.x, creature.y, creature.z);

        creature.pickup();

        assertSame(item, creature.inventory().get(0));
        assertNull(world.item(creature.x, creature.y, creature.z));
        assertEquals("You pickup a potion.", ai.lastMessage);
    }

    @Test
    void drop_movesInventoryItemIntoWorld() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item item = new Item('!', Color.RED, "potion", null);
        creature.inventory().add(item);

        creature.drop(item);

        assertFalse(creature.inventory().contains(item));
        assertNotNull(world.item(creature.x, creature.y, creature.z));
        assertSame(item, world.item(creature.x, creature.y, creature.z));
        assertEquals("You drop a potion.", ai.lastMessage);
    }

    @Test
    void drop_equippedItemClearsWeaponSlot() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item weapon = new Item(')', Color.WHITE, "weapon", null);
        weapon.modifyAttackValue(5);
        creature.inventory().add(weapon);
        creature.equip(weapon);

        creature.drop(weapon);

        assertNull(creature.weapon());
        assertSame(weapon, world.item(creature.x, creature.y, creature.z));
        assertTrue(ai.messages.contains("You drop a weapon."));
    }

    @Test
    void moveBy_movesToGroundAndConsumesFood() {
        RecordingAi ai = new RecordingAi(creature);
        int startingFood = creature.food();
        world.add(creature);

        creature.moveBy(1, 0, 0);

        assertEquals(3, creature.x);
        assertEquals(2, creature.y);
        assertEquals(startingFood - 1, creature.food());
        assertEquals(1, ai.enterCount);
    }

    @Test
    void moveBy_doesNotMoveIntoWall() {
        Tile[][][] tiles = new Tile[5][5][1];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }
        tiles[3][2][0] = Tile.WALL;
        world = new World(tiles);
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 2;
        creature.y = 2;
        creature.z = 0;
        new CreatureAi(creature);

        creature.moveBy(1, 0, 0);

        assertEquals(2, creature.x);
        assertEquals(2, creature.y);
    }

    @Test
    void eat_consumesFoodAndRemovesItem() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item bread = new Item('%', Color.YELLOW, "bread", null);
        bread.modifyFoodValue(100);
        creature.inventory().add(bread);

        creature.eat(bread);

        assertEquals(766, creature.food());
        assertFalse(creature.inventory().contains(bread));
        assertEquals("You eat a bread.", ai.lastMessage);
    }

    @Test
    void quaff_startsEffectAndAddsItToActiveEffects() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item potion = new Item('!', Color.RED, "potion", null);
        potion.setQuaffEffect(new Effect(2) {
            @Override
            public void start(Creature target) {
                target.modifyAttackValue(5);
            }
        });
        creature.inventory().add(potion);

        creature.quaff(potion);

        assertEquals(15, creature.attackValue());
        assertEquals(1, creature.effects().size());
        assertFalse(creature.inventory().contains(potion));
        assertEquals("You quaff a potion.", ai.lastMessage);
    }

    @Test
    void modifyHp_onDeathRemovesCreatureAndLeavesCorpse() {
        world.add(creature);

        creature.modifyHp(-100, "defeated");

        assertNull(world.creature(creature.x, creature.y, creature.z));
        assertNotNull(world.item(creature.x, creature.y, creature.z));
        assertEquals("josh corpse", world.item(creature.x, creature.y, creature.z).name());
    }

    @Test
    void details_reportsCurrentLevelCombatAndHealth() {
        assertEquals("  level:1  attack:10  defense:5  hp:100", creature.details());
    }

    @Test
    void accessors_reportCurrentState() {
        assertEquals(1000, creature.maxFood());
        assertEquals(0, creature.xp());
        assertEquals(9, creature.visionRadius());
        assertNull(creature.weapon());
        assertNull(creature.armor());
    }

    @Test
    void setCreatureAi_replacesAiUsedByNotifications() {
        RecordingAi ai = new RecordingAi(creature);

        creature.notify("hello %s", "world");

        assertEquals("hello world", ai.lastMessage);
    }

    @Test
    void canEnter_requiresGroundAndEmptyLocation() {
        world.add(creature);
        Creature other = new Creature(world, 'g', Color.GREEN, "goblin", 10, 1, 1);
        other.x = 3;
        other.y = 2;
        other.z = 0;
        world.add(other);

        assertTrue(creature.canEnter(2, 3, 0));
        assertFalse(creature.canEnter(3, 2, 0));
    }

    @Test
    void visibility_respectsRadiusAndDetection() {
        Tile[][][] tiles = floorTiles(6);
        tiles[3][2][0] = Tile.WALL;
        world = new World(tiles);
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 2;
        creature.y = 2;
        creature.z = 0;
        new CreatureAi(creature);
        Creature other = new Creature(world, 'g', Color.GREEN, "goblin", 10, 1, 1);
        other.x = 4;
        other.y = 2;
        other.z = 0;
        world.add(other);

        assertFalse(creature.canSee(4, 2, 0));
        assertFalse(creature.canSee(2, 2, 1));
        assertFalse(creature.canSee(20, 20, 0));

        creature.modifyDetectCreatures(1);
        assertSame(other, creature.creature(4, 2, 0));
        assertTrue(creature.canSee(3, 2, 0));
        creature.modifyDetectCreatures(-1);
        assertNull(creature.creature(4, 2, 0));
    }

    @Test
    void realTile_readsWorldTileWhileTileUsesAiMemoryWhenHidden() {
        assertSame(Tile.FLOOR, creature.realTile(1, 1, 0));
        assertSame(Tile.FLOOR, creature.tile(1, 1, 0));
    }

    @Test
    void dig_changesWallToFloorAndConsumesFood() {
        Tile[][][] tiles = floorTiles(5);
        tiles[3][2][0] = Tile.WALL;
        world = new World(tiles);
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 2;
        creature.y = 2;
        new RecordingAi(creature);
        int startingFood = creature.food();

        creature.dig(3, 2, 0);

        assertSame(Tile.FLOOR, world.tile(3, 2, 0));
        assertEquals(startingFood - 10, creature.food());
    }

    @Test
    void meleeAttack_damagesTargetWithinExpectedRange() {
        RecordingAi ai = new RecordingAi(creature);
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 50, 1, 0);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.meleeAttack(target);

        assertTrue(target.hp() >= 40 && target.hp() <= 50);
        assertEquals(664, creature.food());
        assertTrue(ai.lastMessage.matches("You attack the goblin for [1-9][0-9]? damage\\."));
    }

    @Test
    void rangedWeaponAttack_usesEquippedRangedWeapon() {
        RecordingAi ai = new RecordingAi(creature);
        Item bow = new Item(')', Color.WHITE, "bow", null);
        bow.modifyRangedAttackValue(5);
        creature.inventory().add(bow);
        creature.equip(bow);
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 50, 1, 0);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.rangedWeaponAttack(target);

        assertTrue(target.hp() < 50);
        assertEquals(664, creature.food());
        assertTrue(ai.lastMessage.matches("You fire a bow at the goblin for [1-9][0-9]? damage\\."));
    }

    @Test
    void meleeAttack_withGreaterDefenseDealsMinimumDamage() {
        Creature target = new Creature(world, 'g', Color.GREEN, "armored goblin", 50, 1, 20);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        for (int i = 0; i < 20; i++)
            creature.meleeAttack(target);

        assertEquals(30, target.hp());
    }

    @Test
    void rangedWeaponAttack_usesHalfBaseAttackPlusRangedValue() {
        Item bow = new Item(')', Color.WHITE, "bow", null);
        bow.modifyRangedAttackValue(5);
        creature.inventory().add(bow);
        creature.equip(bow);
        Creature target = new Creature(world, 'g', Color.GREEN, "armored goblin", 50, 1, 20);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        for (int i = 0; i < 20; i++)
            creature.rangedWeaponAttack(target);

        assertEquals(30, target.hp());
    }

    @Test
    void meleeAttack_defeatingTargetAwardsExperience() {
        RecordingAi ai = new RecordingAi(creature);
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 1, 1, 0);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.meleeAttack(target);

        assertTrue(CombatRules.isDead(target.hp()));
        assertTrue(creature.xp() > 0);
        assertTrue(ai.messages.stream().anyMatch(message -> message.startsWith("You attack the goblin for ")));
    }

    @Test
    void gainXp_increasesExperienceForDefeatedCreature() {
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 50, 1, 1);

        creature.gainXp(target);

        assertEquals(51, creature.xp());
    }

    @Test
    void modifyXp_reportsGainAndAdvancesOnlyAboveThreshold() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);

        creature.modifyXp(25);
        assertEquals(1, creature.level());
        assertEquals("You gain 25 xp.", ai.messages.get(0));

        creature.modifyXp(1);

        assertEquals(2, creature.level());
        assertEquals(26, creature.xp());
        assertEquals("You gain 1 xp.", ai.messages.get(1));
        assertEquals("You advance to level 2.", ai.messages.get(2));
        assertEquals(1, ai.gainLevelCount);
    }

    @ParameterizedTest
    @CsvSource({
            "-5, 'You lose -5 xp.'",
            "0, 'You gain 0 xp.'"
    })
    void modifyXp_reportsExactDirection(int amount, String expected) {
        RecordingAi ai = new RecordingAi(creature);

        creature.modifyXp(amount);

        assertEquals(expected, ai.lastMessage);
    }

    @Test
    void modifyXp_appliesLevelUpHealthIncrease() {
        RecordingAi ai = new RecordingAi(creature);
        creature.modifyHp(-20, "damage");

        creature.modifyXp(26);

        assertEquals(84, creature.hp());
        assertEquals(1, ai.gainLevelCount);
    }

    @Test
    void update_expiresCompletedEffectsAndCallsAi() {
        RecordingAi ai = new RecordingAi(creature);
        creature.effects().add(new Effect(1));

        creature.update();

        assertTrue(creature.effects().isEmpty());
        assertEquals(1, ai.updateCount);
    }

    @Test
    void summon_addsCreatureToWorld() {
        Creature summoned = new Creature(world, 's', Color.WHITE, "summoned", 5, 1, 1);
        summoned.x = 1;
        summoned.y = 1;
        summoned.z = 0;

        creature.summon(summoned);

        assertSame(summoned, world.creature(1, 1, 0));
    }

    @Test
    void throwItem_placesNonPotionItemAtLastVisibleLocation() {
        Item rock = new Item(',', Color.YELLOW, "rock", null);
        creature.inventory().add(rock);

        creature.throwItem(rock, 3, 2, 0);

        assertFalse(creature.inventory().contains(rock));
        assertSame(rock, world.item(3, 2, 0));
    }

    @Test
    void throwItem_consumesPotionWhenItHitsCreature() {
        Item potion = new Item('!', Color.RED, "potion", null);
        potion.setQuaffEffect(new Effect(1));
        creature.inventory().add(potion);
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 50, 1, 0);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.throwItem(potion, 3, 2, 0);

        assertFalse(creature.inventory().contains(potion));
        assertTrue(target.effects().size() == 1);
    }

    @Test
    void throwItem_placesPotionWhenItMissesCreature() {
        Item potion = new Item('!', Color.RED, "potion", "red potion");
        potion.setQuaffEffect(new Effect(1));
        creature.inventory().add(potion);

        creature.throwItem(potion, 3, 2, 0);

        assertFalse(creature.inventory().contains(potion));
        assertSame(potion, world.item(3, 2, 0));
    }

    @Test
    void throwItem_placesNonPotionWhenItHitsCreature() {
        Item rock = new Item(',', Color.YELLOW, "rock", null);
        creature.inventory().add(rock);
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 50, 1, 0);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.throwItem(rock, 3, 2, 0);

        assertFalse(creature.inventory().contains(rock));
        assertSame(rock, world.item(3, 2, 0));
    }

    @Test
    void castSpell_requiresManaAndTarget() {
        RecordingAi ai = new RecordingAi(creature);
        Spell spell = new Spell("bolt", 3, new Effect(2));
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 50, 1, 0);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.castSpell(spell, target.x, target.y);

        assertEquals(2, creature.mana());
        assertEquals(1, target.effects().size());

        creature.castSpell(new Spell("expensive", 10, new Effect(1)), target.x, target.y);
        assertTrue(ai.lastMessage.contains("nothing happens"));
    }

    @Test
    void castSpell_allowsCostExactlyEqualToCurrentMana() {
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 50, 1, 0);
        target.x = 3;
        target.y = 2;
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.castSpell(new Spell("bolt", 5, new Effect(2)), target.x, target.y);

        assertEquals(0, creature.mana());
        assertEquals(1, target.effects().size());
    }

    @Test
    void castSpell_withoutTargetLeavesManaAndEffectsUnchanged() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        creature.castSpell(new Spell("bolt", 3, new Effect(2)), 3, 2);

        assertEquals(5, creature.mana());
        assertTrue(creature.effects().isEmpty());
        assertEquals("You point and mumble at nothing.", ai.lastMessage);
    }

    @Test
    void item_returnsWorldItemWhenVisibleAndNullWhenHidden() {
        Item item = new Item('!', Color.RED, "potion", null);
        world.addAtEmptySpace(item, 3, 2, 0);

        assertSame(item, creature.item(3, 2, 0));

        world = new World(floorTiles(5));
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 2;
        creature.y = 2;
        new HiddenAi(creature);
        world.addAtEmptySpace(item, 3, 2, 0);

        assertNull(creature.item(3, 2, 0));
    }

    @Test
    void learnName_changesUnknownItemAppearanceToKnownName() {
        Item item = new Item('!', Color.RED, "health potion", "red potion");
        RecordingAi ai = new RecordingAi(creature);

        assertEquals("red potion", creature.nameOf(item));
        creature.learnName(item);

        assertEquals("health potion", creature.nameOf(item));
        assertEquals("The red potion is a health potion!", ai.lastMessage);
    }

    @Test
    void isPlayer_dependsOnGlyph() {
        assertTrue(creature.isPlayer());
        Creature monster = new Creature(world, 'g', Color.GREEN, "goblin", 10, 1, 1);

        assertFalse(monster.isPlayer());
    }

    @ParameterizedTest
    @CsvSource({
            "wave, You wave.",
            "'attack the %s for %d damage', 'You attack the goblin for 7 damage.'"
    })
    void doAction_notifiesActorWithExactMessage(String action, String expected) {
        RecordingAi actorAi = new RecordingAi(creature);
        Creature target = new Creature(world, 'g', Color.GREEN, "goblin", 10, 1, 1);
        new CreatureAi(target);
        world.add(creature);
        world.add(target);

        creature.doAction(action, "goblin", 7);

        assertEquals(expected, actorAi.lastMessage);
    }

    @ParameterizedTest
    @CsvSource({
            "wave, The josh waves.",
            "'attack the %s for %d damage', 'The josh attacks the goblin for 7 damage.'"
    })
    void doAction_notifiesVisibleObserverWithExactSecondPersonMessage(String action, String expected) {
        RecordingAi observerAi = new RecordingAi(new Creature(world, 'g', Color.GREEN, "observer", 10, 1, 1));
        Creature observer = observerAi.creature;
        observer.x = 3;
        observer.y = 2;
        world.add(creature);
        world.add(observer);

        creature.doAction(action, "goblin", 7);

        assertEquals(expected, observerAi.lastMessage);
    }

    @Test
    void doAction_includesCreaturesAtVisionRadiusButNotBeyondIt() {
        world = new World(floorTiles(25));
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 12;
        creature.y = 12;
        new CreatureAi(creature);
        RecordingAi edgeAi = new RecordingAi(new Creature(world, 'g', Color.GREEN, "edge", 10, 1, 1));
        Creature edge = edgeAi.creature;
        edge.x = creature.x + 9;
        edge.y = creature.y;
        RecordingAi outsideAi = new RecordingAi(new Creature(world, 'g', Color.GREEN, "outside", 10, 1, 1));
        Creature outside = outsideAi.creature;
        outside.x = creature.x + 10;
        outside.y = creature.y;
        world.add(creature);
        world.add(edge);
        world.add(outside);

        creature.doAction("wave");

        assertEquals("The josh waves.", edgeAi.lastMessage);
        assertTrue(outsideAi.messages.isEmpty());
    }

    @Test
    void doAction_withItemNotifiesAndTeachesName() {
        RecordingAi observerAi = new RecordingAi(new Creature(world, 'g', Color.GREEN, "observer", 10, 1, 1));
        Creature observer = observerAi.creature;
        observer.x = 3;
        observer.y = 2;
        Item potion = new Item('!', Color.RED, "health potion", "red potion");
        world.add(creature);
        world.add(observer);

        creature.doAction(potion, "quaff a %s", "potion");

        assertTrue(observerAi.messages.contains("The josh quaffs a potion."));
        assertEquals("health potion", observer.nameOf(potion));
    }

    @Test
    void foodAndManaModifiers_updateMaximums() {
        creature.modifyMaxMana(3);
        creature.modifyFood(10);

        assertEquals(8, creature.maxMana());
        assertEquals(676, creature.food());
    }

    @Test
    void doAction_withItemDoesNothingForDeadCreature() {
        RecordingAi observerAi = new RecordingAi(new Creature(world, 'g', Color.GREEN, "observer", 10, 1, 1));
        Creature observer = observerAi.creature;
        observer.x = 3;
        observer.y = 2;
        Item potion = new Item('!', Color.RED, "health potion", "red potion");
        world.add(creature);
        world.add(observer);
        creature.modifyHp(-100, "defeated");
        observerAi.messages.clear();

        creature.doAction(potion, "quaff a %s", "potion");

        assertTrue(observerAi.messages.isEmpty());
        assertEquals("red potion", observer.nameOf(potion));
    }

    @Test
    void moveBy_zeroDeltaDoesNothing() {
        int startingFood = creature.food();

        creature.moveBy(0, 0, 0);

        assertEquals(2, creature.x);
        assertEquals(2, creature.y);
        assertEquals(startingFood, creature.food());
    }

    @Test
    void moveBy_stairDirectionsAllowValidTransitions() {
        Tile[][][] tiles = floorTiles(2, 2);
        tiles[1][0][0] = Tile.STAIRS_DOWN;
        tiles[1][0][1] = Tile.STAIRS_UP;
        world = new World(tiles);
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 0;
        creature.y = 0;
        creature.z = 0;
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);

        creature.moveBy(1, 0, 0);
        creature.moveBy(0, 0, 1);
        creature.moveBy(0, 0, -1);

        assertEquals(1, creature.x);
        assertEquals(0, creature.y);
        assertEquals(0, creature.z);
        assertTrue(ai.enterCount >= 2);
        assertTrue(ai.messages.contains("You walk down the stairs to level 2."));
        assertTrue(ai.messages.contains("You walk up the stairs to level 1."));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 'You try to go down but are stopped by the cave floor.'",
            "-1, 'You try to go up but are stopped by the cave ceiling.'"
    })
    void moveBy_invalidStairDirectionDoesNotConsumeFoodOrMove(int mz, String expectedMessage) {
        Tile[][][] tiles = floorTiles(3, 2);
        world = new World(tiles);
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 1;
        creature.y = 1;
        creature.z = mz == 1 ? 0 : 1;
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        int startingFood = creature.food();

        creature.moveBy(0, 0, mz);

        assertEquals(1, creature.x);
        assertEquals(1, creature.y);
        assertEquals(mz == 1 ? 0 : 1, creature.z);
        assertEquals(startingFood, creature.food());
        assertEquals(expectedMessage, ai.lastMessage);
    }

    @Test
    void hiddenLocationsUseAiRememberedTileAndHideCreatures() {
        HiddenAi ai = new HiddenAi(creature);
        Creature other = new Creature(world, 'g', Color.GREEN, "goblin", 10, 1, 1);
        other.x = 3;
        other.y = 2;
        other.z = 0;
        world.add(other);

        assertSame(Tile.UNKNOWN, creature.tile(3, 2, 0));
        assertNull(creature.creature(3, 2, 0));
        assertEquals(1, ai.rememberedTileCalls);
    }

    @Test
    void pickup_withoutItemLeavesInventoryUnchanged() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        creature.pickup();

        assertTrue(creature.inventory().get(0) == null);
        assertEquals("You grab at the ground.", ai.lastMessage);
    }

    @Test
    void pickup_withFullInventoryLeavesWorldItemInPlace() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        for (int i = 0; i < 20; i++)
            creature.inventory().add(new Item('i', Color.WHITE, "item" + i, null));
        Item item = new Item('!', Color.RED, "potion", null);
        world.addAtEmptySpace(item, creature.x, creature.y, creature.z);

        creature.pickup();

        assertSame(item, world.item(creature.x, creature.y, creature.z));
        assertEquals("You grab at the ground.", ai.lastMessage);
    }

    @Test
    void drop_whenNoSpaceLeavesItemInInventoryAndNotifies() {
        Tile[][][] tiles = floorTiles(1);
        world = new World(tiles);
        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 0;
        creature.y = 0;
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item blocker = new Item('!', Color.RED, "blocker", null);
        world.addAtEmptySpace(blocker, 0, 0, 0);
        Item item = new Item('!', Color.RED, "potion", null);
        creature.inventory().add(item);

        creature.drop(item);

        assertTrue(creature.inventory().contains(item));
        assertSame(blocker, world.item(0, 0, 0));
        assertEquals("There's nowhere to drop the potion.", ai.lastMessage);
    }

    @Test
    void foodOverflowRaisesMaximumAndDamagesCreature() {
        RecordingAi ai = new RecordingAi(creature);
        int startingMaxFood = creature.maxFood();

        creature.modifyFood(400);

        assertEquals(1033, creature.maxFood());
        assertEquals(1033, creature.food());
        assertEquals(99, creature.hp());
        assertTrue(creature.maxFood() > startingMaxFood);
        assertEquals("You can't belive your stomach can hold that much!", ai.lastMessage);
    }

    @Test
    void modifyFood_atMaximumDoesNotTriggerOvereating() {
        RecordingAi ai = new RecordingAi(creature);

        creature.modifyFood(334);

        assertEquals(1000, creature.food());
        assertEquals(1000, creature.maxFood());
        assertEquals(100, creature.hp());
        assertTrue(ai.messages.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "-1, true",
            "0, false"
    })
    void consume_reportsGrossOnlyForNegativeFood(int foodValue, boolean expectsGross) {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item item = new Item('%', Color.YELLOW, "ration", null);
        item.modifyFoodValue(foodValue);
        creature.inventory().add(item);

        creature.eat(item);

        assertEquals(expectsGross, ai.messages.contains("Gross!"));
        assertTrue(ai.messages.contains("You eat a ration."));
    }

    @Test
    void quaffingEquippedItemClearsItsEquipmentSlot() {
        Item weapon = new Item(')', Color.WHITE, "weapon", null);
        weapon.modifyAttackValue(5);
        weapon.setQuaffEffect(new Effect(1));
        creature.inventory().add(weapon);
        creature.equip(weapon);

        creature.quaff(weapon);

        assertNull(creature.weapon());
        assertFalse(creature.inventory().contains(weapon));
    }

    @Test
    void update_regeneratesHealthAndManaWhenBelowMaximum() {
        creature.modifyHp(-10, "damage");
        creature.modifyMana(-2);
        int startingFood = creature.food();

        creature.update();

        assertEquals(91, creature.hp());
        assertEquals(4, creature.mana());
        assertEquals(startingFood - 3, creature.food());
    }

    @Test
    void regenerationModifiersAffectNextHealthAndManaUpdate() {
        creature.modifyHp(-10, "damage");
        creature.modifyMana(-2);
        creature.modifyRegenHpPer1000(990);
        creature.modifyRegenManaPer1000(980);
        int startingFood = creature.food();

        creature.update();

        assertEquals(91, creature.hp());
        assertEquals(4, creature.mana());
        assertEquals(startingFood - 3, creature.food());
    }

    @Test
    void update_doesNotRegenerateFullResources() {
        int startingFood = creature.food();

        creature.update();

        assertEquals(100, creature.hp());
        assertEquals(5, creature.mana());
        assertEquals(startingFood - 1, creature.food());
    }

    @Test
    void equip_externalItemAddsItToInventoryAndWorldItemIsRemoved() {
        Item sword = new Item(')', Color.WHITE, "sword", null);
        sword.modifyAttackValue(5);
        world.addAtEmptySpace(sword, creature.x, creature.y, creature.z);

        creature.equip(sword);

        assertSame(sword, creature.weapon());
        assertTrue(creature.inventory().contains(sword));
        assertEquals(15, creature.attackValue());
        assertNull(world.item(creature.x, creature.y, creature.z));
    }

    @Test
    void equip_fullInventoryRejectsExternalItem() {
        RecordingAi ai = new RecordingAi(creature);
        for (int i = 0; i < 20; i++)
            creature.inventory().add(new Item('i', Color.WHITE, "item" + i, null));
        Item sword = new Item(')', Color.WHITE, "sword", null);
        sword.modifyAttackValue(5);

        creature.equip(sword);

        assertNull(creature.weapon());
        assertFalse(creature.inventory().contains(sword));
        assertEquals("Can't equip sword since you're holding too much stuff.", ai.lastMessage);
    }

    @Test
    void equip_replacesExistingWeaponAndArmor() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item firstWeapon = new Item(')', Color.WHITE, "first", null);
        firstWeapon.modifyAttackValue(3);
        Item secondWeapon = new Item(')', Color.WHITE, "second", null);
        secondWeapon.modifyAttackValue(7);
        creature.inventory().add(firstWeapon);
        creature.inventory().add(secondWeapon);
        creature.equip(firstWeapon);
        creature.equip(secondWeapon);

        assertSame(secondWeapon, creature.weapon());
        assertEquals(17, creature.attackValue());
        assertFalse(creature.weapon() == firstWeapon);
        assertTrue(ai.messages.contains("You put away a first."));
        assertTrue(ai.messages.contains("You wield a second."));
    }

    @Test
    void equip_replacesExistingArmorAndReportsBothActions() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item firstArmor = new Item('[', Color.WHITE, "first armor", null);
        firstArmor.modifyDefenseValue(3);
        Item secondArmor = new Item('[', Color.WHITE, "second armor", null);
        secondArmor.modifyDefenseValue(7);
        creature.inventory().add(firstArmor);
        creature.inventory().add(secondArmor);

        creature.equip(firstArmor);
        creature.equip(secondArmor);

        assertSame(secondArmor, creature.armor());
        assertEquals(12, creature.defenseValue());
        assertTrue(ai.messages.contains("You remove a first armor."));
        assertTrue(ai.messages.contains("You put on a second armor."));
    }

    @ParameterizedTest
    @CsvSource({
            "-100",
            "-101"
    })
    void unequip_nonPositiveHealthClearsItemWithoutNotification(int damage) {
        RecordingAi ai = new RecordingAi(creature);
        Item weapon = new Item(')', Color.WHITE, "weapon", null);
        weapon.modifyAttackValue(5);
        creature.inventory().add(weapon);
        creature.equip(weapon);
        ai.messages.clear();
        creature.modifyHp(damage, "defeated");

        creature.unequip(weapon);

        assertNull(creature.weapon());
        assertTrue(ai.messages.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "2, 2, true",
            "1, 2, false"
    })
    void equip_classifiesItemByAttackAndDefense(int attack, int defense, boolean expectedWeapon) {
        Item item = new Item(')', Color.WHITE, "item", null);
        item.modifyAttackValue(attack);
        item.modifyDefenseValue(defense);
        creature.inventory().add(item);

        creature.equip(item);

        assertEquals(expectedWeapon, creature.weapon() == item);
        assertEquals(!expectedWeapon, creature.armor() == item);
    }

    @Test
    void throwItem_unequipsEquippedItemWhenPlaced() {
        Item weapon = new Item(')', Color.WHITE, "weapon", null);
        weapon.modifyAttackValue(5);
        creature.inventory().add(weapon);
        creature.equip(weapon);

        creature.throwItem(weapon, 3, 2, 0);

        assertNull(creature.weapon());
        assertFalse(creature.inventory().contains(weapon));
        assertSame(weapon, world.item(3, 2, 0));
    }

    @Test
    void throwItem_notifiesActorWhenNoCreatureIsHit() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        Item rock = new Item(',', Color.YELLOW, "rock", null);
        creature.inventory().add(rock);

        creature.throwItem(rock, 3, 2, 0);

        assertEquals("You throw a rock.", ai.lastMessage);
    }

    @Test
    void consume_withoutEffectDoesNotAddActiveEffect() {
        Item food = new Item('%', Color.YELLOW, "bread", null);
        food.modifyFoodValue(10);
        creature.inventory().add(food);

        creature.eat(food);

        assertTrue(creature.effects().isEmpty());
        assertFalse(creature.inventory().contains(food));
    }

    private static Tile[][][] floorTiles(int size) {
        return floorTiles(size, 1);
    }

    private static Tile[][][] floorTiles(int size, int depth) {
        Tile[][][] tiles = new Tile[size][size][depth];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < depth; z++)
                    tiles[x][y][z] = Tile.FLOOR;
            }
        }
        return tiles;
    }

    private static class RecordingAi extends CreatureAi {
        private String lastMessage = "";
        private final List<String> messages = new ArrayList<>();
        private int updateCount;
        private int enterCount;
        private int gainLevelCount;

        private RecordingAi(Creature creature) {
            super(creature);
        }

        @Override
        public void onNotify(String message) {
            lastMessage = message;
            messages.add(message);
        }

        @Override
        public void onUpdate() {
            updateCount++;
        }

        @Override
        public void onEnter(int x, int y, int z, Tile tile) {
            enterCount++;
            super.onEnter(x, y, z, tile);
        }

        @Override
        public void onGainLevel() {
            gainLevelCount++;
        }
    }

    private static class HiddenAi extends RecordingAi {
        private int rememberedTileCalls;

        private HiddenAi(Creature creature) {
            super(creature);
        }

        @Override
        public boolean canSee(int wx, int wy, int wz) {
            return false;
        }

        @Override
        public Tile rememberedTile(int wx, int wy, int wz) {
            rememberedTileCalls++;
            return Tile.UNKNOWN;
        }
    }
}
