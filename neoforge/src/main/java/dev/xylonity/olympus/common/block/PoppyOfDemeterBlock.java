package dev.xylonity.olympus.common.block;

import dev.xylonity.olympus.common.block.entity.PoppyOfDemeterBlockEntity;
import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.registry.OlympusParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PoppyOfDemeterBlock extends FlowerBlock implements EntityBlock {

    public PoppyOfDemeterBlock(final BlockBehaviour.Properties properties) {
        super(SuspiciousStewEffects.EMPTY, properties);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new PoppyOfDemeterBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }

    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (level.getBlockEntity(pos) instanceof PoppyOfDemeterBlockEntity poppy) {
            updateBlockCycle(level, pos, random, poppy);
        }

        level.scheduleTick(pos, this, 4);
    }

    private static void updateBlockCycle(final ServerLevel level, final BlockPos origin, final RandomSource random, final PoppyOfDemeterBlockEntity poppy) {
        BlockPos target = poppy.getTargetPos();
        boolean newlyAttached = false;
        // Finds a valid block
        if (!isValidBlock(level, origin, target)) {
            target = findRandomBlock(level, origin, random, target);
            poppy.attachTo(target);
            newlyAttached = target != null;
        }

        if (target == null) {
            return;
        }

        // Performs bone meal acceleration once the cycle is reached and looks for a new block
        final int growthIntervalTicks = Math.max(4, OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.demeterPoppyGrowthIntervalSeconds.get()));
        if (!newlyAttached && poppy.advanceCycle(4) >= growthIntervalTicks) {
            poppy.resetCycle();
            final BlockState targetState = level.getBlockState(target);
            final BonemealableBlock bonemealable = (BonemealableBlock) targetState.getBlock();
            bonemealable.performBonemeal(level, random, target, targetState, BonemealSource.INTERACTION);

            if (poppy.recordAcceleration() >= OlympusConfig.INSTANCE.demeterPoppyGrowthsPerTarget.get() || !isValidBlock(level, origin, target)) {
                target = findRandomBlock(level, origin, random, target);
                poppy.attachTo(target);
            }

        }

        // Particles
        if (target != null) {
            final BlockState targetState = level.getBlockState(target);
            final BonemealableBlock bonemealable = (BonemealableBlock) targetState.getBlock();
            spawnGrowthParticle(level, target, targetState, bonemealable, random);
        }

    }

    private static @Nullable BlockPos findRandomBlock(final ServerLevel level, final BlockPos origin, final RandomSource random, final @Nullable BlockPos excludedTarget) {
        final int radius = OlympusConfig.INSTANCE.demeterPoppyRadius.get();
        final double radiusSqr = radius * radius;
        final BlockPos min = origin.offset(-radius, -radius, -radius);
        final BlockPos max = origin.offset(radius, radius, radius);
        final List<BlockPos> possiblePositions = new ArrayList<>();

        for (final BlockPos target : BlockPos.betweenClosed(min, max)) {
            if (origin.distSqr(target) > radiusSqr || !level.hasChunkAt(target)) {
                continue;
            }

            final BlockState targetState = level.getBlockState(target);
            if (targetState.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(level, target, targetState, BonemealSource.INTERACTION)) {
                possiblePositions.add(target.immutable());
            }

        }

        if (possiblePositions.isEmpty()) {
            return null;
        }

        if (excludedTarget != null && possiblePositions.size() > 1) {
            possiblePositions.remove(excludedTarget);
        }

        return possiblePositions.get(random.nextInt(possiblePositions.size()));
    }

    private static boolean isValidBlock(final ServerLevel level, final BlockPos origin, final @Nullable BlockPos target) {
        final int radius = OlympusConfig.INSTANCE.demeterPoppyRadius.get();
        if (target == null || origin.distSqr(target) > radius * radius || !level.hasChunkAt(target)) {
            return false;
        }

        final BlockState state = level.getBlockState(target);
        return state.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(level, target, state, BonemealSource.INTERACTION);
    }

    private static void spawnGrowthParticle(final ServerLevel level, final BlockPos target, final BlockState state, final BonemealableBlock bonemealable, final RandomSource random) {
        final BlockPos particlePos = bonemealable.getParticlePos(target);
        final double y;
        if (!particlePos.equals(target)) {
            y = particlePos.getY() + 0.15D;
        }
        else {
            final VoxelShape shape = state.getShape(level, target);
            final double blockHeight = shape.isEmpty() ? 0.5D : shape.max(Direction.Axis.Y);
            y = target.getY() + Math.max(0.25D, blockHeight) + 0.1D;
        }

        level.sendParticles(OlympusParticles.POPPY_GROWTH.get(), target.getX() + random.nextDouble(), y, target.getZ() + random.nextDouble(), 1, 0, 0, 0, 0);
    }

}
