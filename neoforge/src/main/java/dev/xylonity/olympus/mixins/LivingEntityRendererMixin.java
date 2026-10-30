package dev.xylonity.olympus.mixins;

import dev.xylonity.olympus.client.util.HadesInvisibilityRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void olympus$useTranslucentRenderType(LivingEntityRenderState renderState, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> callback) {
        if (renderState instanceof AvatarRenderState avatarRenderState && avatarRenderState.skin != null && HadesInvisibilityRenderState.isActive(avatarRenderState)) {
            callback.setReturnValue(RenderTypes.entityTranslucentCull(avatarRenderState.skin.body().texturePath()));
        }

    }

    @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
    private void olympus$applyHadesInvisibilityAlpha(LivingEntityRenderState renderState, CallbackInfoReturnable<Integer> callback) {
        if (renderState instanceof AvatarRenderState avatarRenderState && HadesInvisibilityRenderState.isActive(avatarRenderState)) {
            callback.setReturnValue(0x80FFFFFF);
        }

    }

}