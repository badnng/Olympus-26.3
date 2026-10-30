package dev.xylonity.olympus.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.client.entity.model.HarpyProjectileModel;
import dev.xylonity.olympus.common.entity.projectile.HarpyProjectileEntity;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class HarpyProjectileEntityRenderer extends EntityRenderer<HarpyProjectileEntity, EntityRenderState> {

    private static final Identifier TEXTURE = Olympus.of("textures/entity/harpy_projectile.png");

    private static final RenderType RENDER_TYPE_BASE = RenderTypes.entityTranslucentEmissive(TEXTURE);
    private static final RenderType RENDER_TYPE_INVERTED_CUBES = OlympusRenderTypes.invertedCubesGlow(TEXTURE);

    private final HarpyProjectileModel model;

    public HarpyProjectileEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new HarpyProjectileModel(context.bakeLayer(HarpyProjectileModel.LAYER_LOCATION));
        shadowRadius = 0.1F;
        shadowStrength = 0.7F;
    }

    @Override
    protected int getBlockLightLevel(final HarpyProjectileEntity entity, final BlockPos blockPos) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(final HarpyProjectileEntity entity, final BlockPos blockPos) {
        return 15;
    }

    @Override
    public void submit(final EntityRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.translate(0, 0.15F, 0);
        poseStack.rotate(Axis.YP.rotationDegrees(state.ageInTicks + 8));
        poseStack.rotate(Axis.XP.rotationDegrees(state.ageInTicks + 8));
        poseStack.scale(1.5F, 1.5F, 1.5F);

        submitNodeCollector.submitModelPart(model.projectile(), poseStack, RENDER_TYPE_BASE, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        submitNodeCollector.submitModelPart(model.cubeOutline(), poseStack, RENDER_TYPE_INVERTED_CUBES, state.lightCoords, OverlayTexture.NO_OVERLAY, null);

        poseStack.popPose();

        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

}
