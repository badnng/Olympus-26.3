package dev.xylonity.olympus.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xylonity.olympus.common.entity.projectile.SpearOfAresEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.NonNull;

public final class SpearOfAresEntityRenderer extends EntityRenderer<SpearOfAresEntity, SpearOfAresEntityRenderer.RenderState> {

    private final ItemModelResolver itemModelResolver;

    public SpearOfAresEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public void submit(final RenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.rotate(Axis.YP.rotationDegrees(state.yRot + 90));
        poseStack.rotate(Axis.ZP.rotationDegrees(90 - state.xRot));

        if (state.shake > 0) {
            poseStack.rotate(Axis.ZP.rotationDegrees(-Mth.sin(state.shake * 3.0F) * state.shake));
        }

        poseStack.rotate(Axis.YP.rotationDegrees(90));
        poseStack.translate(0, -22 / 16F, 0);

        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();

        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected int getBlockLightLevel(final SpearOfAresEntity entity, final @NonNull BlockPos blockPos) {
        // No black overlay
        return entity.isInWall() ? LightCoordsUtil.FULL_SKY : super.getBlockLightLevel(entity, blockPos);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(final SpearOfAresEntity entity, final RenderState state, final float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.shake = entity.shakeTime - partialTick;
        itemModelResolver.updateForNonLiving(state.item, entity.getWeaponItem(), ItemDisplayContext.NONE, entity);
    }

    public static final class RenderState extends ThrownItemRenderState {
        private float xRot;
        private float yRot;
        private float shake;
    }

}