package dev.xylonity.olympus.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.NonNull;

public final class PentelicMarbleColumnBlock extends RotatedPillarBlock {

    public static final EnumProperty<ColumnPart> PART = EnumProperty.create("part", ColumnPart.class);
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");

    public PentelicMarbleColumnBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(PART, ColumnPart.SINGLE).setValue(REVERSED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, REVERSED);
    }

    @Override
    public @NonNull BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction placementDirection = context.getClickedFace();
        final boolean reversed = placementDirection.getAxis() != Direction.Axis.Y && placementDirection.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
        final BlockState state = super.getStateForPlacement(context).setValue(REVERSED, reversed);
        return updatePart(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected @NonNull BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour.getAxis() != state.getValue(AXIS)) {
            return state;
        }

        return updatePart(state, level, pos);
    }

    private static BlockState updatePart(final BlockState state, final LevelReader level, final BlockPos pos) {
        final Direction.Axis axis = state.getValue(AXIS);
        final Direction forward = Direction.fromAxisAndDirection(axis, state.getValue(REVERSED) ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
        final Direction backward = forward.getOpposite();
        final boolean connectedBackward = connects(level.getBlockState(pos.relative(backward)), axis);
        final boolean connectedForward = connects(level.getBlockState(pos.relative(forward)), axis);
        final ColumnPart part;

        if (connectedBackward && connectedForward) {
            part = ColumnPart.MIDDLE;
        }
        else if (connectedForward) {
            part = ColumnPart.BASE;
        }
        else if (connectedBackward) {
            part = ColumnPart.CAPITAL;
        }
        else {
            part = ColumnPart.SINGLE;
        }

        return state.setValue(PART, part);
    }

    private static boolean connects(final BlockState state, final Direction.Axis axis) {
        return state.getBlock() instanceof PentelicMarbleColumnBlock && state.getValue(AXIS) == axis;
    }

    public enum ColumnPart implements StringRepresentable {
        SINGLE("single"),
        BASE("base"),
        MIDDLE("middle"),
        CAPITAL("capital");

        private final String name;

        ColumnPart(final String name) {
            this.name = name;
        }

        @Override
        public @NonNull String getSerializedName() {
            return this.name;
        }

    }

}
