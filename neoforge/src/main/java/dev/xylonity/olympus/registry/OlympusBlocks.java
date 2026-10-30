package dev.xylonity.olympus.registry;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.common.block.AirCloudBlock;
import dev.xylonity.olympus.common.block.ClimbingRoseBlock;
import dev.xylonity.olympus.common.block.OlympusStairBlock;
import dev.xylonity.olympus.common.block.PentelicMarbleColumnBlock;
import dev.xylonity.olympus.common.block.PoppyOfDemeterBlock;
import dev.xylonity.olympus.common.block.LockedChestBlock;
import dev.xylonity.olympus.common.block.ParthenonSpawnerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OlympusBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Olympus.MOD_ID);

    public static final DeferredBlock<PoppyOfDemeterBlock> POPPY_OF_DEMETER = BLOCKS.registerBlock("poppy_of_demeter", PoppyOfDemeterBlock::new,
            properties -> properties
                .mapColor(MapColor.PLANT)
                .noCollision()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.POPPED)
    );

    public static final DeferredBlock<Block> PENTELIC_MARBLE = BLOCKS.registerBlock("pentelic_marble", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<OlympusStairBlock> PENTELIC_MARBLE_STAIRS = BLOCKS.registerBlock("pentelic_marble_stairs", properties -> new OlympusStairBlock(PENTELIC_MARBLE.get().defaultBlockState(), properties), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<SlabBlock> PENTELIC_MARBLE_SLAB = BLOCKS.registerBlock("pentelic_marble_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<WallBlock> PENTELIC_MARBLE_WALL = BLOCKS.registerBlock("pentelic_marble_wall", WallBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).forceSolidOn());
    public static final DeferredBlock<Block> POLISHED_PENTELIC_MARBLE = BLOCKS.registerBlock("polished_pentelic_marble", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<OlympusStairBlock> POLISHED_PENTELIC_MARBLE_STAIRS = BLOCKS.registerBlock("polished_pentelic_marble_stairs", properties -> new OlympusStairBlock(POLISHED_PENTELIC_MARBLE.get().defaultBlockState(), properties), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<SlabBlock> POLISHED_PENTELIC_MARBLE_SLAB = BLOCKS.registerBlock("polished_pentelic_marble_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<Block> PENTELIC_MARBLE_BRICK = BLOCKS.registerBlock("pentelic_marble_brick", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<OlympusStairBlock> PENTELIC_MARBLE_BRICK_STAIRS = BLOCKS.registerBlock("pentelic_marble_brick_stairs", properties -> new OlympusStairBlock(PENTELIC_MARBLE_BRICK.get().defaultBlockState(), properties), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<SlabBlock> PENTELIC_MARBLE_BRICK_SLAB = BLOCKS.registerBlock("pentelic_marble_brick_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<WallBlock> PENTELIC_MARBLE_BRICK_WALL = BLOCKS.registerBlock("pentelic_marble_brick_wall", WallBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).forceSolidOn());
    public static final DeferredBlock<Block> CRACKED_PENTELIC_MARBLE_BRICK = BLOCKS.registerBlock("cracked_pentelic_marble_brick", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));
    public static final DeferredBlock<PentelicMarbleColumnBlock> PENTELIC_MARBLE_COLUMN = BLOCKS.registerBlock("pentelic_marble_column", PentelicMarbleColumnBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE));

    public static final DeferredBlock<Block> PARTHENON_TERRACOTTA_TILES = BLOCKS.registerBlock("parthenon_terracotta_tiles", Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).sound(SoundType.DEEPSLATE_TILES));
    public static final DeferredBlock<OlympusStairBlock> PARTHENON_TERRACOTTA_TILE_STAIRS = BLOCKS.registerBlock("parthenon_terracotta_tile_stairs", properties -> new OlympusStairBlock(PARTHENON_TERRACOTTA_TILES.get().defaultBlockState(), properties), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).sound(SoundType.DEEPSLATE_TILES));
    public static final DeferredBlock<SlabBlock> PARTHENON_TERRACOTTA_TILE_SLAB = BLOCKS.registerBlock("parthenon_terracotta_tile_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).sound(SoundType.DEEPSLATE_TILES));

    public static final DeferredBlock<ClimbingRoseBlock> CLIMBING_ROSE = BLOCKS.registerBlock(
            "climbing_rose",
            ClimbingRoseBlock::new,
            properties -> properties
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.AZALEA_LEAVES)
                    .ignitedByLava()
                    .pushReaction(PushReaction.POPPED)
    );

    public static final DeferredBlock<AirCloudBlock> AIR_CLOUD_BLOCK = BLOCKS.registerBlock("air_cloud_block", AirCloudBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POWDER_SNOW));

    public static final DeferredBlock<ParthenonSpawnerBlock> PARTHENON_SPAWNER = BLOCKS.registerBlock("parthenon_spawner", ParthenonSpawnerBlock::new,
            properties -> properties
                    .mapColor(MapColor.STONE)
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.TRIAL_SPAWNER)
                    .lightLevel(state -> state.getValue(ParthenonSpawnerBlock.ACTIVE) || state.getValue(ParthenonSpawnerBlock.REWARD) ? 8 : 0)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos, nearPlaneBox) -> false));

    public static final DeferredBlock<LockedChestBlock> LOCKED_CHEST = BLOCKS.registerBlock("locked_chest", LockedChestBlock::new,
            properties -> properties
                    .mapColor(MapColor.WOOD)
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

}
