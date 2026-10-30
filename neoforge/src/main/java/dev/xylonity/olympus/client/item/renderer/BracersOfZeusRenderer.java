package dev.xylonity.olympus.client.item.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.client.item.model.BracersOfZeusModel;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class BracersOfZeusRenderer implements ICurioRenderer.HumanoidRender {

    private static final Identifier TEXTURE = Olympus.of("textures/entity/curio/bracers_of_zeus.png");
    private static final Identifier GLOW_TEXTURE = Olympus.of("textures/entity/curio/bracers_of_zeus_glow.png");

    private static final float THIRD_PERSON_Y_OFFSET = 2 / 16F;
    
    private final BracersOfZeusModel slimModel;
    private final BracersOfZeusModel wideModel;

    public BracersOfZeusRenderer() {
        this.slimModel = new BracersOfZeusModel(Minecraft.getInstance().getEntityModels().bakeLayer(BracersOfZeusModel.SLIM_LAYER_LOCATION));
        this.wideModel = new BracersOfZeusModel(Minecraft.getInstance().getEntityModels().bakeLayer(BracersOfZeusModel.WIDE_LAYER_LOCATION));
    }

    @Override
    public EntityModel<HumanoidRenderState> getModel(final ItemStack stack, final SlotContext slotContext) {
        return wideModel;
    }

    @Override
    public Identifier getModelTexture(final ItemStack stack, final SlotContext slotContext) {
        return TEXTURE;
    }

    @Override
    public void renderFirstPersonHand(final ItemStack stack, final SlotContext slotContext, final HumanoidArm arm, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final AvatarRenderState avatarRenderState, final AbstractClientPlayer clientPlayer, final int packedLight) {
        final BracersOfZeusModel model = modelFor(clientPlayer);
        model.resetPose();

        // Extra tilt so the bracers and the hand point at the same rotation
        model.arm(arm).zRot = arm == HumanoidArm.LEFT ? -0.1F : 0.1F;

        submitArm(model, stack, arm, poseStack, submitNodeCollector, packedLight, avatarRenderState.outlineColor, 0, true);
    }

    @Override
    public void prepareModel(final ItemStack stack, final SlotContext slotContext, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int packedLight, final HumanoidRenderState renderState, final RenderLayerParent<HumanoidRenderState, EntityModel<HumanoidRenderState>> renderLayerParent, final EntityRendererProvider.Context context, final float yRotation, final float xRotation) {
        final BracersOfZeusModel model = modelFor(renderState);
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> parentModel) {
            model.arm(HumanoidArm.LEFT).loadPose(parentModel.leftArm.storePose());
            model.arm(HumanoidArm.RIGHT).loadPose(parentModel.rightArm.storePose());
        }
        else {
            ICurioRenderer.setupHumanoidAnimations(model, renderState);
        }

    }

    @Override
    public void renderModel(final ItemStack stack, final SlotContext slotContext, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int packedLight, final HumanoidRenderState renderState, final RenderLayerParent<HumanoidRenderState, EntityModel<HumanoidRenderState>> renderLayerParent, final EntityRendererProvider.Context context, final float yRotation, final float xRotation) {
        final BracersOfZeusModel model = modelFor(renderState);
        submitArm(model, stack, HumanoidArm.LEFT, poseStack, submitNodeCollector, packedLight, renderState.outlineColor, THIRD_PERSON_Y_OFFSET, false);
        submitArm(model, stack, HumanoidArm.RIGHT, poseStack, submitNodeCollector, packedLight, renderState.outlineColor, THIRD_PERSON_Y_OFFSET, false);
    }

    private void submitArm(final BracersOfZeusModel model, final ItemStack stack, final HumanoidArm arm, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int packedLight, final int outlineColor, final float yOffset, final boolean firstPerson) {
        final ModelPart armPart = model.arm(arm);
        poseStack.pushPose();
        armPart.translateAndRotate(poseStack);
        poseStack.translate(0.0F, yOffset, 0.0F);

        submitNodeCollector.order(1).submitModelPart(
                model.bracer(arm),
                poseStack,
                (stack.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(TEXTURE) : RenderTypes.armorCutoutNoCull(TEXTURE)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                outlineColor
        );
        submitNodeCollector.order(3).submitModelPart(
                model.outline(arm),
                poseStack,
                firstPerson ? OlympusRenderTypes.firstPersonInvertedCubesGlow(GLOW_TEXTURE) : OlympusRenderTypes.invertedCubesGlow(GLOW_TEXTURE),
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                outlineColor
        );

        poseStack.popPose();
    }

    private BracersOfZeusModel modelFor(final HumanoidRenderState renderState) {
        if (renderState instanceof AvatarRenderState avatarRenderState && avatarRenderState.skin != null && avatarRenderState.skin.model() == PlayerModelType.SLIM) {
            return slimModel;
        }

        return wideModel;
    }

    private BracersOfZeusModel modelFor(final AbstractClientPlayer player) {
        return player.getSkin().model() == PlayerModelType.SLIM ? slimModel : wideModel;
    }

}
