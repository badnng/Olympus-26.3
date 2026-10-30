package dev.xylonity.olympus.client;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.client.entity.model.AbsorbedSoulModel;
import dev.xylonity.olympus.client.entity.model.HarpyProjectileModel;
import dev.xylonity.olympus.client.entity.renderer.AbsorbedSoulEntityRenderer;
import dev.xylonity.olympus.client.entity.renderer.HarpyEntityRenderer;
import dev.xylonity.olympus.client.entity.renderer.HarpyProjectileEntityRenderer;
import dev.xylonity.olympus.client.entity.renderer.PoseidonTridentEntityRenderer;
import dev.xylonity.olympus.client.entity.renderer.SpearOfAresEntityRenderer;
import dev.xylonity.olympus.client.entity.renderer.SummoningSpearsEntityRenderer;
import dev.xylonity.olympus.client.item.model.BracersOfZeusModel;
import dev.xylonity.olympus.client.item.model.HelmetOfHadesModel;
import dev.xylonity.olympus.client.particle.AresSpearHitParticle;
import dev.xylonity.olympus.client.particle.AresSpearTraceParticle;
import dev.xylonity.olympus.client.particle.ArtemisLeafParticle;
import dev.xylonity.olympus.client.particle.ForgingSparkParticle;
import dev.xylonity.olympus.client.particle.HarpyProjectileTrailParticle;
import dev.xylonity.olympus.client.particle.HarpyProjectileTrailParticleGroup;
import dev.xylonity.olympus.client.particle.HarpyMagicParticle;
import dev.xylonity.olympus.client.particle.HarpyFeatherParticle;
import dev.xylonity.olympus.client.particle.LightningBoltParticle;
import dev.xylonity.olympus.client.particle.LightningParticleGroup;
import dev.xylonity.olympus.client.particle.LightningSparksParticle;
import dev.xylonity.olympus.client.particle.LyreNoteParticle;
import dev.xylonity.olympus.client.particle.PoppyGrowthParticle;
import dev.xylonity.olympus.client.particle.SoulSalvationParticle;
import dev.xylonity.olympus.client.particle.SoulTrailParticle;
import dev.xylonity.olympus.client.particle.SoulTrailParticleGroup;
import dev.xylonity.olympus.client.particle.TridentSplashParticle;
import dev.xylonity.olympus.client.particle.TridentUnderwaterSplashParticle;
import dev.xylonity.olympus.client.particle.TridentUnderwaterSplashParticleGroup;
import dev.xylonity.olympus.client.particle.TridentWaterDropParticle;
import dev.xylonity.olympus.client.item.renderer.BracersOfZeusRenderer;
import dev.xylonity.olympus.client.item.renderer.HermesSandalsRenderer;
import dev.xylonity.olympus.client.item.renderer.HelmetOfHadesRenderer;
import dev.xylonity.olympus.client.item.model.HermesSandalsModel;
import dev.xylonity.olympus.client.compat.IrisCompat;
import dev.xylonity.olympus.client.texture.SpearDissolveTextures;
import dev.xylonity.olympus.common.item.HermesSandalsItem;
import dev.xylonity.olympus.network.payload.CameraShakePayload;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.network.payload.LightningBoltPayload;
import dev.xylonity.olympus.network.payload.LyreMusicPayload;
import dev.xylonity.olympus.network.payload.HermesJumpPayload;
import dev.xylonity.olympus.network.payload.SoulSalvationPayload;
import dev.xylonity.olympus.common.entity.projectile.AbsorbedSoulEntity;
import dev.xylonity.olympus.common.entity.projectile.HarpyProjectileEntity;
import dev.xylonity.olympus.client.util.HadesInvisibilityRenderState;
import dev.xylonity.olympus.client.util.CameraShake;
import dev.xylonity.olympus.client.sound.LyreMusicSound;
import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.registry.OlympusMobEffects;
import dev.xylonity.olympus.registry.OlympusParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@EventBusSubscriber(modid = Olympus.MOD_ID, value = Dist.CLIENT)
public final class OlympusClientEvents {

    private static final Identifier LIGHTNING_STUN_POST_EFFECT = Olympus.of("lightning_stun");

    private static int hermesExtraJumps;
    private static boolean hermesJumpKeyDown;

    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        ICurioRenderer.register(OlympusItems.BRACERS_OF_ZEUS.get(), BracersOfZeusRenderer::new);
        ICurioRenderer.register(OlympusItems.HELMET_OF_HADES.get(), HelmetOfHadesRenderer::new);
        ICurioRenderer.register(OlympusItems.HERMES_SANDALS.get(), HermesSandalsRenderer::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OlympusEntities.ABSORBED_SOUL.get(), AbsorbedSoulEntityRenderer::new);
        event.registerEntityRenderer(OlympusEntities.POSEIDON_TRIDENT.get(), PoseidonTridentEntityRenderer::new);
        event.registerEntityRenderer(OlympusEntities.SPEAR_OF_ARES.get(), SpearOfAresEntityRenderer::new);
        event.registerEntityRenderer(OlympusEntities.SUMMONING_SPEARS.get(), SummoningSpearsEntityRenderer::new);
        event.registerEntityRenderer(OlympusEntities.HARPY.get(), HarpyEntityRenderer::new);
        event.registerEntityRenderer(OlympusEntities.ELITE_HARPY.get(), context -> new HarpyEntityRenderer(context, true));
        event.registerEntityRenderer(OlympusEntities.HARPY_PROJECTILE.get(), HarpyProjectileEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AbsorbedSoulModel.LAYER_LOCATION, AbsorbedSoulModel::createBodyLayer);
        event.registerLayerDefinition(BracersOfZeusModel.SLIM_LAYER_LOCATION, BracersOfZeusModel::createSlimBodyLayer);
        event.registerLayerDefinition(BracersOfZeusModel.WIDE_LAYER_LOCATION, BracersOfZeusModel::createWideBodyLayer);
        event.registerLayerDefinition(HelmetOfHadesModel.LAYER_LOCATION, HelmetOfHadesModel::createBodyLayer);
        event.registerLayerDefinition(HermesSandalsModel.LAYER_LOCATION, HermesSandalsModel::createBodyLayer);
        event.registerLayerDefinition(HarpyProjectileModel.LAYER_LOCATION, HarpyProjectileModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderPipelines(final RegisterRenderPipelinesEvent event) {
        event.registerPipeline(OlympusRenderTypes.INVERTED_CUBES_GLOW_PIPELINE);
        event.registerPipeline(OlympusRenderTypes.FIRST_PERSON_INVERTED_CUBES_GLOW_PIPELINE);
        event.registerPipeline(OlympusRenderTypes.UNDERWATER_SPLASH_PIPELINE);
        event.registerPipeline(OlympusRenderTypes.TRANSLUCENT_CURIO_GLINT_PIPELINE);

        IrisCompat.registerRenderPipelines();
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(final AddClientReloadListenersEvent event) {
        event.addListener(Olympus.of("spear_dissolve_textures"), SpearDissolveTextures.INSTANCE);
    }

    @SubscribeEvent
    public static void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Olympus.of("hades_screen_filter"),
            (guiGraphics, ignoredDeltaTracker) -> {
                final Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null || !minecraft.player.hasEffect(OlympusMobEffects.INVISIBILITY_OF_HADES)) {
                    return;
                }

                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Olympus.of("textures/gui/invisibility_of_hades_screen_filter.png"), 0, 0, 0.0F, 0.0F, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 256, 256, 256, 256);
            }

        );

    }

    @SubscribeEvent
    public static void registerParticleGroups(final RegisterParticleGroupsEvent event) {
        event.register(LightningParticleGroup.TYPE, LightningParticleGroup::new);
        event.register(SoulTrailParticleGroup.TYPE, SoulTrailParticleGroup::new);
        event.register(HarpyProjectileTrailParticleGroup.TYPE, HarpyProjectileTrailParticleGroup::new);
        event.register(TridentUnderwaterSplashParticleGroup.TYPE, TridentUnderwaterSplashParticleGroup::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(OlympusParticles.ARES_SPEAR_TRACE.get(), AresSpearTraceParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.ARES_SPEAR_HIT.get(), AresSpearHitParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.LYRE_NOTE.get(), LyreNoteParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.SOUL_SALVATION.get(), SoulSalvationParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.POPPY_GROWTH.get(), PoppyGrowthParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.LIGHTNING_SPARKS.get(), LightningSparksParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.HARPY_MAGIC.get(), HarpyMagicParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.HARPY_FEATHER.get(), HarpyFeatherParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.ELITE_HARPY_FEATHER.get(), HarpyFeatherParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.TRIDENT_SPLASH_OF_WATER.get(), TridentSplashParticle.MainProvider::new);
        event.registerSpriteSet(OlympusParticles.TRIDENT_WATER_DROP.get(), TridentWaterDropParticle.Provider::new);
        event.registerSpriteSet(OlympusParticles.TRIDENT_SMALL_SPLASH_OF_WATER.get(), TridentSplashParticle.SmallProvider::new);
        event.registerSpriteSet(OlympusParticles.FORGING_SPARK.get(), ForgingSparkParticle.Provider::new);
        event.registerSpecial(OlympusParticles.TRIDENT_UNDERWATER_SPLASH.get(), new TridentUnderwaterSplashParticle.Provider());
        event.registerSpriteSet(OlympusParticles.ARTEMIS_ARROW_TRACE.get(), ArtemisLeafParticle.MainProvider::new);
        event.registerSpriteSet(OlympusParticles.ARTEMIS_ARROW_TRACE_SMALL.get(), ArtemisLeafParticle.SmallProvider::new);
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(final RegisterRenderStateModifiersEvent event) {
        // Handles the player invisibility when the hades effect is active
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(final T avatar, final @NonNull AvatarRenderState renderState) {
                renderState.setRenderData(HadesInvisibilityRenderState.ACTIVE, avatar.hasEffect(OlympusMobEffects.INVISIBILITY_OF_HADES));
            }

        });

    }

    @SubscribeEvent
    public static void registerClientPayloadHandlers(final RegisterClientPayloadHandlersEvent event) {
        event.register(CameraShakePayload.TYPE, (payload, _) ->
                CameraShake.trigger(payload.origin(), payload.radius(), payload.strength(), payload.duration())
        );
        event.register(LightningBoltPayload.TYPE, (payload, _) -> {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                minecraft.particleEngine.add(new LightningBoltParticle(minecraft.level, payload.start(), payload.end(), payload.skyStrike()));
            }

        });
        event.register(LyreMusicPayload.TYPE, (payload, _) -> {
            if (payload.playing()) {
                LyreMusicSound.start(payload.musicianId());
            }
            else {
                LyreMusicSound.fadeOut(payload.musicianId());
            }

        });
        event.register(SoulSalvationPayload.TYPE, (payload, _) -> {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }

            final Entity entity = minecraft.level.getEntity(payload.entityId());
            if (entity == null) {
                return;
            }

            final int count = payload.particleCount();
            final Vec3 position = payload.sphericalBurst() ? entity.getBoundingBox().getCenter() : new Vec3(entity.getX(), entity.getY() + 0.2D, entity.getZ());
            final RandomSource random = minecraft.level.getRandom();
            for (int direction = 0; direction < count; direction++) {
                final double x;
                final double y;
                final double z;
                // Particle direction in a spherical-like shape
                if (payload.sphericalBurst()) {
                    final double vertical = random.nextDouble() * 2.0 - 1.0;
                    final double horizontal = Math.sqrt(1.0 - vertical * vertical);
                    final double angle = random.nextDouble() * Math.PI * 2.0;
                    final double speed = 0.08 + random.nextDouble() * 0.04;
                    x = Math.cos(angle) * horizontal * speed;
                    y = vertical * speed;
                    z = Math.sin(angle) * horizontal * speed;
                }
                // Relative random direction
                else {
                    final double angle = Math.PI * 2.0 * direction / count;
                    x = Math.cos(angle) * 0.23225;
                    y = 0.0D;
                    z = Math.sin(angle) * 0.23225;
                }

                minecraft.level.addParticle(OlympusParticles.SOUL_SALVATION.get(), position.x, position.y, position.z, x, y, z);
            }

        });

    }

    @SubscribeEvent
    private static void onHermesClientTick(final ClientTickEvent.Pre event) {
        final Minecraft minecraft = Minecraft.getInstance();
        // Restarts the jump amount counter
        if (minecraft.player == null || minecraft.level == null) {
            hermesExtraJumps = HermesSandalsItem.getExtraJumps();
            hermesJumpKeyDown = false;
            return;
        }

        // Restarts the jump amount counter
        final boolean jumpKeyDown = minecraft.options.keyJump.isDown();
        if (minecraft.player.onGround()) {
            hermesExtraJumps = HermesSandalsItem.getExtraJumps();
        }

        // The player jumps
        if (minecraft.gui.screen() == null && jumpKeyDown && !hermesJumpKeyDown && hermesExtraJumps > 0 && HermesSandalsItem.canExtraJump(minecraft.player)) {
            hermesExtraJumps--;
            minecraft.player.jumpFromGround();
            ClientPacketDistributor.sendToServer(HermesJumpPayload.INSTANCE);
        }

        hermesJumpKeyDown = jumpKeyDown;
    }

    @SubscribeEvent
    private static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbsorbedSoulEntity soul && event.getLevel() instanceof ClientLevel level) {
            Minecraft.getInstance().particleEngine.add(new SoulTrailParticle(level, soul));
        }
        else if (event.getEntity() instanceof HarpyProjectileEntity projectile && event.getLevel() instanceof ClientLevel level) {
            Minecraft.getInstance().particleEngine.add(new HarpyProjectileTrailParticle(level, projectile));
        }

    }

    @SubscribeEvent
    private static void onClientTick(final ClientTickEvent.Post event) {
        CameraShake.tick();

        final Minecraft minecraft = Minecraft.getInstance();
        final boolean stunned = minecraft.getCameraEntity() instanceof LivingEntity livingEntity
                && livingEntity.hasEffect(OlympusMobEffects.LIGHTNING_STUN);
        final Identifier currentEffect = minecraft.gameRenderer.spectatedEntityPostEffect();

        // Handles the screen post shader when the lightning stun effect is active
        if (stunned) {
            if (!LIGHTNING_STUN_POST_EFFECT.equals(currentEffect)) {
                minecraft.gameRenderer.setSpectatedEntityPostEffect(LIGHTNING_STUN_POST_EFFECT);
            }

        }
        else if (LIGHTNING_STUN_POST_EFFECT.equals(currentEffect)) {
            minecraft.gameRenderer.clearSpectatedEntityPostEffect();
        }

    }

    @SubscribeEvent
    private static void onComputeCameraAngles(final ViewportEvent.ComputeCameraAngles event) {
        final Vec3 offsets = CameraShake.getOffsets(event.getPartialTick());
        event.setYaw(event.getYaw() + (float) offsets.x);
        event.setPitch(event.getPitch() + (float) offsets.y);
        event.setRoll(event.getRoll() + (float) offsets.z);
    }

}
