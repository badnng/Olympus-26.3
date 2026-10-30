package dev.xylonity.olympus.common.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xylonity.olympus.registry.OlympusStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public final class CelestialParthenonStructure extends Structure {

    public static final MapCodec<CelestialParthenonStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(structure -> structure.maxDepth),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(structure -> structure.expansion),
            JigsawStructure.MaxDistance.CODEC.fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
    ).apply(instance, CelestialParthenonStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean expansion;
    private final JigsawStructure.MaxDistance maxDistanceFromCenter;

    public CelestialParthenonStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth, HeightProvider startHeight, boolean expansion, JigsawStructure.MaxDistance maxDistanceFromCenter) {
        super(settings);
        this.startPool = startPool;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.expansion = expansion;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
    }

    @Override
    protected @NonNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        final ChunkPos chunkPos = context.chunkPos();
        final int y = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        final BlockPos start = new BlockPos(chunkPos.getMinBlockX(), y, chunkPos.getMinBlockZ());
        return JigsawPlacement.addPieces(context, this.startPool, Optional.empty(), this.maxDepth, start, this.expansion, Optional.empty(), this.maxDistanceFromCenter, PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.APPLY_WATERLOGGING);
    }

    @Override
    public @NonNull Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        final ChunkPos chunkPos = context.chunkPos();
        final int x = chunkPos.getMiddleBlockX();
        final int z = chunkPos.getMiddleBlockZ();

        final Holder<Biome> surfaceBiome = context.biomeSource().createUncachedResolver(context.randomState()).getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(context.chunkGenerator().getSeaLevel()), QuartPos.fromBlock(z));
        return context.validBiome().test(surfaceBiome) ? this.findGenerationPoint(context) : Optional.empty();
    }

    @Override
    public @NonNull StructureType<?> type() {
        return OlympusStructureTypes.CELESTIAL_PARTHENON.get();
    }

}
