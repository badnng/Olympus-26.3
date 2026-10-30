package dev.xylonity.olympus.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public final class AirCloudBlock extends HalfTransparentBlock {

    public AirCloudBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier, final boolean isPrecise) {
        entity.makeStuckInBlock(state, new Vec3(0.9225D, 0.3225D, 0.9225D));
    }

    @Override
    public void fallOn(final Level level, final BlockState state, final BlockPos pos, final Entity entity, final double fallDistance) {
        entity.resetFallDistance();
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (!context.isPlacement() && context instanceof EntityCollisionContext entityContext) {
            final Entity entity = entityContext.getEntity();
            if (entity != null && entity.fallDistance > 2.5) {
                return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.9D, 1.0D);
            }

        }

        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return Shapes.empty();
    }

}