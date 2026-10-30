package dev.xylonity.olympus.client.item.renderer;

import com.geckolib.animatable.GeoItem;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.CustomBoneTextureGeoLayer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xylonity.olympus.client.item.model.SpearOfAresModel;
import dev.xylonity.olympus.client.texture.SpearDissolveTextures;
import dev.xylonity.olympus.common.entity.projectile.SpearOfAresEntity;
import dev.xylonity.olympus.common.item.SpearOfAresItem;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SpearOfAresItemRenderer extends GeoItemRenderer<SpearOfAresItem> {

    public SpearOfAresItemRenderer() {
        super(new SpearOfAresModel<>());
        withRenderLayer(new CustomBoneTextureGeoLayer<>(this, "cube_outline", SpearOfAresModel.CHARGED_TEXTURE) {
            @Override
            public boolean shouldRenderBone(final GeoRenderState renderState) {
                return SpearOfAresModel.isSpecialAbilityCharged(renderState);
            }

            @Override
            protected @Nullable RenderType getRenderType(final GeoRenderState renderState, final Identifier texture) {
                final ItemDisplayContext context = renderState.getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
                if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    return OlympusRenderTypes.firstPersonInvertedCubesGlow(texture);
                }

                return OlympusRenderTypes.invertedCubesGlow(texture);
            }

            @Override
            protected void renderQuad(GeoQuad quad, final Matrix4f pose, final Vector3f normal, VertexConsumer vertexConsumer, final int packedLight, int packedOverlay, final int renderColor, float widthRatio, final float heightRatio) {
                super.renderQuad(quad, pose, normal, vertexConsumer, LightCoordsUtil.FULL_BRIGHT, packedOverlay, renderColor, widthRatio, heightRatio);
            }

        });

    }

    @Override
    public void addRenderData(final SpearOfAresItem animatable, final @Nullable RenderData renderData, final GeoRenderState renderState, final float partialTick) {
        renderState.addGeckolibData(SpearOfAresModel.SPECIAL_ABILITY_CHARGED, renderData != null && SpearOfAresItem.isSpecialAbilityCharged(renderData.itemStack()));
        final float visibility = renderData != null && renderData.itemOwner() instanceof SpearOfAresEntity spear ? spear.getDissolveVisibility(partialTick) : 1;
        renderState.addGeckolibData(SpearOfAresModel.DISSOLVE_VISIBILITY, visibility);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final ItemDisplayContext context = renderPassInfo.getGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        final long stackId = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, Long.MIN_VALUE);

        if (player != null) {
            if (player.isUsingItem() && GeoItem.getId(player.getUseItem()) == stackId) {
                if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                    renderPassInfo.poseStack().rotate(Axis.XP.rotationDegrees(180));
                    renderPassInfo.poseStack().translate(0, -0.75, -1);
                }
                else if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    renderPassInfo.poseStack().translate(0.5, -0.9, 0.15);
                    renderPassInfo.poseStack().rotate(Axis.XP.rotationDegrees(-25));
                }
                else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                    renderPassInfo.poseStack().translate(-0.5, -0.9, 0.15);
                    renderPassInfo.poseStack().rotate(Axis.XP.rotationDegrees(-25));
                }

            }

        }

        super.adjustRenderPose(renderPassInfo);
    }

    @Override
    public int getRenderColor(final SpearOfAresItem animatable, final @Nullable RenderData renderData, final float partialTick) {
        return 0xFFFFFFFF;
    }

    @Override
    public @Nullable RenderType getRenderType(final GeoRenderState renderState, final Identifier texture) {
        final float visibility = renderState.getOrDefaultGeckolibData(SpearOfAresModel.DISSOLVE_VISIBILITY, 1.0F);
        return RenderTypes.entityCutout(SpearDissolveTextures.textureFor(texture, visibility));
    }

}
