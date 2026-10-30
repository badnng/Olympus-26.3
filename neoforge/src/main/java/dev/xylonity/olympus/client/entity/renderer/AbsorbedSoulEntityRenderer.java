package dev.xylonity.olympus.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.client.entity.model.AbsorbedSoulModel;
import dev.xylonity.olympus.common.entity.projectile.AbsorbedSoulEntity;
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

public final class AbsorbedSoulEntityRenderer extends EntityRenderer<AbsorbedSoulEntity, EntityRenderState> {

    private static final Identifier TEXTURE = Olympus.of("textures/entity/absorbed_soul.png");

    private static final RenderType RENDER_TYPE_BASE = RenderTypes.entityTranslucentEmissive(TEXTURE);
    private static final RenderType RENDER_TYPE_INVERTED_CUBES = OlympusRenderTypes.invertedCubesGlow(TEXTURE);

    private final AbsorbedSoulModel model;

    public AbsorbedSoulEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new AbsorbedSoulModel(context.bakeLayer(AbsorbedSoulModel.LAYER_LOCATION));
        shadowRadius = 0.1F;
        shadowStrength = 0.7F;
    }

    @Override
    protected int getBlockLightLevel(final AbsorbedSoulEntity entity, final BlockPos blockPos) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(final AbsorbedSoulEntity entity, final BlockPos blockPos) {
        return 15;
    }

    @Override
    public void submit(final EntityRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.rotate(Axis.YP.rotationDegrees(state.ageInTicks * 3.0F));
        poseStack.translate(0.0F, -1.35F, 0.0F);

        submitNodeCollector.submitModelPart(model.soul(), poseStack, RENDER_TYPE_BASE, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        submitNodeCollector.submitModelPart(model.cubeOutline(), poseStack, RENDER_TYPE_INVERTED_CUBES, state.lightCoords, OverlayTexture.NO_OVERLAY, null);

        poseStack.popPose();

        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

}