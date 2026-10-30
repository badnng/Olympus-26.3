package dev.xylonity.olympus.client.item.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.client.item.model.HelmetOfHadesModel;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class HelmetOfHadesRenderer implements ICurioRenderer.HumanoidRender {

    private static final Identifier TEXTURE = Olympus.of("textures/entity/curio/helmet_of_hades.png");
    private static final Identifier OUTLINE_TEXTURE = Olympus.of("textures/entity/curio/helmet_of_hades_luminous_outline.png");

    private final HelmetOfHadesModel model;

    public HelmetOfHadesRenderer() {
        model = new HelmetOfHadesModel(Minecraft.getInstance().getEntityModels().bakeLayer(HelmetOfHadesModel.LAYER_LOCATION));
    }

    @Override
    public EntityModel<HumanoidRenderState> getModel(final ItemStack stack, final SlotContext slotContext) {
        return model;
    }

    @Override
    public Identifier getModelTexture(final ItemStack stack, final SlotContext slotContext) {
        return TEXTURE;
    }

    @Override
    public void prepareModel(final ItemStack stack, final SlotContext slotContext, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int packedLight, final HumanoidRenderState renderState, final RenderLayerParent<HumanoidRenderState, EntityModel<HumanoidRenderState>> renderLayerParent, final EntityRendererProvider.Context context, final float yRotation, final float xRotation) {
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> parentModel) {
            model.headPart().loadPose(parentModel.head.storePose());
        }
        else {
            ICurioRenderer.setupHumanoidAnimations(model, renderState);
        }

    }

    @Override
    public void renderModel(final ItemStack stack, final SlotContext slotContext, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int packedLight, final HumanoidRenderState renderState, final RenderLayerParent<HumanoidRenderState, EntityModel<HumanoidRenderState>> renderLayerParent, final EntityRendererProvider.Context context, final float yRotation, final float xRotation) {
        poseStack.pushPose();
        model.headPart().translateAndRotate(poseStack);

        submitNodeCollector.order(1).submitModelPart(
                model.helmet(),
                poseStack,
                OlympusRenderTypes.translucentCurio(TEXTURE, stack.hasFoil()),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                renderState.outlineColor
        );
        submitNodeCollector.order(3).submitModelPart(
                model.outline(),
                poseStack,
                OlympusRenderTypes.invertedCubesGlow(OUTLINE_TEXTURE),
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                renderState.outlineColor
        );

        poseStack.popPose();
    }

}