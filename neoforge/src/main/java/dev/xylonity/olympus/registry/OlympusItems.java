package dev.xylonity.olympus.registry;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.common.item.BracersOfZeusItem;
import dev.xylonity.olympus.common.item.ArtemisBowItem;
import dev.xylonity.olympus.common.item.AphroditeLyreItem;
import dev.xylonity.olympus.common.item.HelmetOfHadesItem;
import dev.xylonity.olympus.common.item.HermesSandalsItem;
import dev.xylonity.olympus.common.item.InstrumentsOfHephaestusItem;
import dev.xylonity.olympus.common.item.PersephoneCupItem;
import dev.xylonity.olympus.common.item.PoppyOfDemeterItem;
import dev.xylonity.olympus.common.item.PoseidonTridentItem;
import dev.xylonity.olympus.common.item.SpearOfAresItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Optional;

public class OlympusItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Olympus.MOD_ID);

    public static final DeferredItem<SpawnEggItem> HARPY_SPAWN_EGG = ITEMS.registerItem("harpy_spawn_egg", SpawnEggItem::new, properties -> properties.spawnEgg(OlympusEntities.HARPY.get()));
    public static final DeferredItem<SpawnEggItem> ELITE_HARPY_SPAWN_EGG = ITEMS.registerItem("elite_harpy_spawn_egg", SpawnEggItem::new, properties -> properties.spawnEgg(OlympusEntities.ELITE_HARPY.get()));
    public static final DeferredItem<Item> PARTHENON_KEY = ITEMS.registerSimpleItem("parthenon_key", properties -> properties.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> PARTHENON_SPAWNER = ITEMS.registerSimpleBlockItem(OlympusBlocks.PARTHENON_SPAWNER);
    public static final DeferredItem<BlockItem> LOCKED_CHEST = ITEMS.registerSimpleBlockItem(OlympusBlocks.LOCKED_CHEST);
    public static final DeferredItem<BracersOfZeusItem> BRACERS_OF_ZEUS = ITEMS.registerItem("bracers_of_zeus", BracersOfZeusItem::new, properties -> properties.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<ArtemisBowItem> BOW_OF_ARTEMIS = ITEMS.registerItem("bow_of_artemis", ArtemisBowItem::new, properties -> properties.durability(384).enchantable(1).rarity(Rarity.EPIC));
    public static final DeferredItem<AphroditeLyreItem> APHRODITE_LYRE = ITEMS.registerItem("aphrodite_lyre", AphroditeLyreItem::new, properties -> properties.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<HelmetOfHadesItem> HELMET_OF_HADES = ITEMS.registerItem("helmet_of_hades", HelmetOfHadesItem::new, properties -> properties.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<HermesSandalsItem> HERMES_SANDALS = ITEMS.registerItem("hermes_sandals", HermesSandalsItem::new, properties -> properties.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<InstrumentsOfHephaestusItem> INSTRUMENTS_OF_HEPHAESTUS = ITEMS.registerItem("instruments_of_hephaestus", InstrumentsOfHephaestusItem::new, properties -> properties.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<PersephoneCupItem> PERSEPHONE_CUP = ITEMS.registerItem("persephone_cup", PersephoneCupItem::new, properties -> properties.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<PoppyOfDemeterItem> POPPY_OF_DEMETER = ITEMS.registerItem("poppy_of_demeter", properties -> new PoppyOfDemeterItem(OlympusBlocks.POPPY_OF_DEMETER.get(), properties), properties -> properties.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_STAIRS = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_STAIRS);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_SLAB = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_SLAB);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_WALL = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_WALL);
    public static final DeferredItem<BlockItem> POLISHED_PENTELIC_MARBLE = ITEMS.registerSimpleBlockItem(OlympusBlocks.POLISHED_PENTELIC_MARBLE);
    public static final DeferredItem<BlockItem> POLISHED_PENTELIC_MARBLE_STAIRS = ITEMS.registerSimpleBlockItem(OlympusBlocks.POLISHED_PENTELIC_MARBLE_STAIRS);
    public static final DeferredItem<BlockItem> POLISHED_PENTELIC_MARBLE_SLAB = ITEMS.registerSimpleBlockItem(OlympusBlocks.POLISHED_PENTELIC_MARBLE_SLAB);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_BRICK = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_BRICK);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_BRICK_SLAB = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_BRICK_WALL = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_BRICK_WALL);
    public static final DeferredItem<BlockItem> CRACKED_PENTELIC_MARBLE_BRICK = ITEMS.registerSimpleBlockItem(OlympusBlocks.CRACKED_PENTELIC_MARBLE_BRICK);
    public static final DeferredItem<BlockItem> PENTELIC_MARBLE_COLUMN = ITEMS.registerSimpleBlockItem(OlympusBlocks.PENTELIC_MARBLE_COLUMN);
    public static final DeferredItem<BlockItem> PARTHENON_TERRACOTTA_TILES = ITEMS.registerSimpleBlockItem(OlympusBlocks.PARTHENON_TERRACOTTA_TILES);
    public static final DeferredItem<BlockItem> PARTHENON_TERRACOTTA_TILE_STAIRS = ITEMS.registerSimpleBlockItem(OlympusBlocks.PARTHENON_TERRACOTTA_TILE_STAIRS);
    public static final DeferredItem<BlockItem> PARTHENON_TERRACOTTA_TILE_SLAB = ITEMS.registerSimpleBlockItem(OlympusBlocks.PARTHENON_TERRACOTTA_TILE_SLAB);
    public static final DeferredItem<BlockItem> CLIMBING_ROSE = ITEMS.registerSimpleBlockItem(OlympusBlocks.CLIMBING_ROSE);
    public static final DeferredItem<BlockItem> AIR_CLOUD_BLOCK = ITEMS.registerSimpleBlockItem(OlympusBlocks.AIR_CLOUD_BLOCK);
    public static final DeferredItem<PoseidonTridentItem> POSEIDON_TRIDENT = ITEMS.registerItem("poseidon_trident", PoseidonTridentItem::new, properties -> properties
                .rarity(Rarity.EPIC)
                .durability(2000)
                .attributes(PoseidonTridentItem.createAttributes())
                .component(DataComponents.TOOL, TridentItem.createToolProperties())
                .enchantable(1)
                .component(DataComponents.WEAPON, new Weapon(1))
    );
    public static final DeferredItem<SpearOfAresItem> SPEAR_OF_ARES = ITEMS.registerItem("spear_of_ares", SpearOfAresItem::new, properties -> properties.rarity(Rarity.EPIC)
            .durability(2000)
            // Base att modifiers
            .attributes(
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.7, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .build())
            .component(DataComponents.TOOL, TridentItem.createToolProperties())
            .enchantable(1)
            // Spear-type item damage
            .delayedHolderComponent(DataComponents.DAMAGE_TYPE, DamageTypes.SPEAR)
            // Keeps the exact forward attack (client transforms) used by vanilla spears without enabling their charged attack
            .component(DataComponents.KINETIC_WEAPON,
                    new KineticWeapon(10, 0, Optional.empty(), Optional.empty(),
                    Optional.empty(), 0.38F, 0.7F, Optional.empty(), Optional.empty()
            ))
            .component(DataComponents.PIERCING_WEAPON, new PiercingWeapon(
                    true, false, Optional.of(SoundEvents.SPEAR_ATTACK), Optional.of(SoundEvents.SPEAR_HIT)
            ))
            .component(DataComponents.ATTACK_RANGE, new AttackRange(2, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F))
            .component(DataComponents.MINIMUM_ATTACK_CHARGE, 1f)
            .component(DataComponents.ATTACK_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 10))
            .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1))
            .component(DataComponents.WEAPON, new Weapon(1)));

}
