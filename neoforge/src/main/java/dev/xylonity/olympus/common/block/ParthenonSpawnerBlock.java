package dev.xylonity.olympus.common.block;

import dev.xylonity.olympus.common.block.entity.ParthenonSpawnerBlockEntity;
import dev.xylonity.olympus.registry.OlympusBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ParthenonSpawnerBlock extends BaseEntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty REWARD = BooleanProperty.create("reward");

    public ParthenonSpawnerBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false).setValue(REWARD, false));
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ParthenonSpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, OlympusBlockEntities.PARTHENON_SPAWNER.get(),
                (tickerLevel, pos, tickerState, spawner) -> ParthenonSpawnerBlockEntity.serverTick((ServerLevel) tickerLevel, pos, tickerState, spawner));
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (!state.getValue(ACTIVE) && !state.getValue(REWARD)) {
            return;
        }

        final double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.7D;
        final double y = pos.getY() + 0.35D + random.nextDouble() * 0.65D;
        final double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.7D;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.015, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.01, 0);
        }

        if (random.nextInt(75) == 0) {
            level.playLocalSound(pos, SoundEvents.TRIAL_SPAWNER_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }

    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, REWARD);
    }

}