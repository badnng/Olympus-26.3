package dev.xylonity.olympus.common.block;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.registry.OlympusParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

public final class LockedChestBlock extends HorizontalDirectionalBlock {

    private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, Olympus.of("chests/parthenon_chest"));

    public LockedChestBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NonNull BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NonNull InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!stack.is(OlympusItems.PARTHENON_KEY.get())) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            unlock(serverLevel, pos, state, player, stack);
        }

        return InteractionResult.SUCCESS;
    }

    private static void unlock(final ServerLevel level, final BlockPos pos, final BlockState state, final Player player, final ItemStack key) {
        // Permutes this block with an actual chest block
        final BlockState chestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, state.getValue(FACING));
        level.setBlock(pos, chestState, Block.UPDATE_ALL);

        // Sets random loot
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(LOOT_TABLE);
            chest.setLootTableSeed(level.getRandom().nextLong());
            chest.setChanged();
        }

        key.consume(1, player);

        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1.0F, 1.0F);

        final RandomSource random = level.getRandom();
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, 28, 0.42D, 0.35D, 0.42D, 0.08D);
        for (int i = 0; i < 18; i++) {
            level.sendParticles(OlympusParticles.LIGHTNING_SPARKS.get(), pos.getX() + 0.2D + random.nextDouble() * 0.6D, pos.getY() + 0.2D + random.nextDouble() * 0.8D, pos.getZ() + 0.2D + random.nextDouble() * 0.6D, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}