package dev.xylonity.olympus.client.item.renderer;

import com.geckolib.animatable.GeoItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import dev.xylonity.olympus.client.item.model.PoseidonTridentModel;
import dev.xylonity.olympus.common.item.PoseidonTridentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemDisplayContext;

public final class PoseidonTridentItemRenderer extends GeoItemRenderer<PoseidonTridentItem> {

    public PoseidonTridentItemRenderer() {
        super(new PoseidonTridentModel<>());
    }

    @Override
    public void adjustRenderPose(final RenderPassInfo<GeoRenderState> renderPassInfo) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final ItemDisplayContext context = renderPassInfo.getGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        final long stackId = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, Long.MIN_VALUE);

        if (player != null) {
            if (player.isUsingItem() && GeoItem.getId(player.getUseItem()) == stackId) {
                if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                    renderPassInfo.poseStack().rotate(Axis.XP.rotationDegrees(180));
                    renderPassInfo.poseStack().translate(0, -1.25, -1);
                }
                else if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    renderPassInfo.poseStack().translate(0.5, -1, 0.55);
                    renderPassInfo.poseStack().rotate(Axis.XP.rotationDegrees(-35));
                }
                else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                    renderPassInfo.poseStack().translate(-0.5, -1, 0.55);
                    renderPassInfo.poseStack().rotate(Axis.XP.rotationDegrees(-35));
                }

            }

        }

        super.adjustRenderPose(renderPassInfo);
    }

}