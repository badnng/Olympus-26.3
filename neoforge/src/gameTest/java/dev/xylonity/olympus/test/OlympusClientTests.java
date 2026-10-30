package dev.xylonity.olympus.test;

import com.google.gson.GsonBuilder;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.common.entity.HarpyEntity;
import dev.xylonity.olympus.network.payload.*;
import dev.xylonity.olympus.registry.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.sdl.SDLVideo;
import top.theillusivec4.curios.api.CuriosApi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Runs inside a real hidden SDL client, with no desktop input or computer-use tools. */
@EventBusSubscriber(modid = OlympusGameTests.MOD_ID, value = Dist.CLIENT)
public final class OlympusClientTests {
    private static final Identifier STUN = Olympus.of("lightning_stun");
    private static final List<String> CHECKS = new ArrayList<>();
    private static int ticks;
    private static int worldTicks;
    private static int frames;
    private static int itemIndex;
    private static boolean finished;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (Boolean.getBoolean("olympus.clientTest") && Minecraft.getInstance().level != null) frames++;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("olympus.clientTest") || finished) return;
        Minecraft client = Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        SDLVideo.SDL_HideWindow(client.getWindow().handle());
        if (++ticks > 6000) fail("Client did not finish within 6000 ticks");
        if (ticks % 200 == 0 && client.level == null) {
            Olympus.LOGGER.info("Client regression loading: screen={}", client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName());
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        worldTicks++;
        if (worldTicks == 10) {
            check(client.getShaderManager().isPostEffectValid(STUN, LevelTargetBundle.MAIN_TARGETS), "Lightning post-effect definition loaded");
            check(client.getShaderManager().getPostChain(STUN, LevelTargetBundle.MAIN_TARGETS) != null, "Lightning post-effect compiled");
            for (var entry : OlympusItems.ITEMS.getEntries()) {
                for (var context : List.of(ItemDisplayContext.GUI, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
                    var state = new ItemStackRenderState();
                    client.getItemModelResolver().updateForLiving(state, new ItemStack(entry.get()), context, client.player);
                    check(!state.isEmpty(), "Item model " + entry.getId() + " / " + context);
                }
            }
            client.getSingleplayerServer().execute(() -> setupScene(client));
        }
        if (worldTicks > 30 && worldTicks <= 30 + 33 * 8 && (worldTicks - 31) % 8 == 0) {
            var entries = new ArrayList<>(OlympusItems.ITEMS.getEntries());
            var item = entries.get(itemIndex++).get();
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            });
        }
        if (worldTicks == 310) {
            check(frames > 40, "World rendered frames");
            check(itemIndex == 33, "Cycled all 33 held item models");
            check(CuriosApi.getCuriosInventory(client.player).orElseThrow().isEquipped(OlympusItems.HELMET_OF_HADES.get()), "Curios equipment synchronized");
            client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());
                player.addEffect(new MobEffectInstance(OlympusMobEffects.INVISIBILITY_OF_HADES, 80));
                player.addEffect(new MobEffectInstance(OlympusMobEffects.LIGHTNING_STUN, 80));
                PacketDistributor.sendToPlayer(player, new CameraShakePayload(player.position(), 36, 1, 35));
                PacketDistributor.sendToPlayer(player, new LightningBoltPayload(player.position().add(0, 10, 0), player.position().add(0, 0, 3), true));
                PacketDistributor.sendToPlayer(player, new SoulSalvationPayload(player.getId(), 16, true));
                PacketDistributor.sendToPlayer(player, new LyreMusicPayload(player.getId(), true));
            });
        }
        if (worldTicks == 335) {
            check(client.player.hasEffect(OlympusMobEffects.INVISIBILITY_OF_HADES), "Hades effect synchronized and rendered");
            check(client.gameRenderer.getAppliedPostEffects().contains(STUN), "Lightning stun post-effect applied while rendering");
            ClientPacketDistributor.sendToServer(HermesJumpPayload.INSTANCE);
        }
        if (worldTicks == 400) {
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());
                PacketDistributor.sendToPlayer(player, new LyreMusicPayload(player.getId(), false));
            });
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (worldTicks == 440) {
            check(!client.gameRenderer.getAppliedPostEffects().contains(STUN), "Lightning post-effect removed after expiration");
            CHECKS.add("Entity renderers, custom particle groups, Curios glow/glint, and all five payload handlers exercised");
            writeReport(true, "");
            finished = true;
            client.stop();
        }
    }

    private static void setupScene(Minecraft client) {
        var server = client.getSingleplayerServer();
        var player = server.getPlayerList().getPlayer(client.player.getUUID());
        var level = player.level();
        player.setGameMode(GameType.CREATIVE);
        BlockPos origin = player.blockPosition();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }
        player.setYRot(0);
        player.setXRot(0);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        for (var slot : List.of("head", "bracelet", "feet")) {
            var item = switch (slot) {
                case "head" -> OlympusItems.HELMET_OF_HADES.get();
                case "bracelet" -> OlympusItems.BRACERS_OF_ZEUS.get();
                default -> OlympusItems.HERMES_SANDALS.get();
            };
            var stack = new ItemStack(item);
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            inventory.setEquippedCurio(slot, 0, stack);
        }
        int index = 0;
        for (var entry : OlympusEntities.ENTITIES.getEntries()) {
            var entity = entry.get().create(level, EntitySpawnReason.COMMAND);
            entity.setPos(player.position().add((index++ % 4 - 1.5) * 2, 1, 5 + index / 4));
            entity.setNoGravity(true);
            if (entity instanceof HarpyEntity harpy) harpy.setNoAi(true);
            level.addFreshEntity(entity);
        }
        for (var entry : OlympusParticles.PARTICLES.getEntries()) {
            level.sendParticles((ParticleOptions) entry.get(), player.getX(), player.getY() + 1, player.getZ() + 3, 3, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) fail(message);
        CHECKS.add(message);
    }

    private static void fail(String message) {
        writeReport(false, message);
        finished = true;
        throw new IllegalStateException("Olympus client regression failed: " + message);
    }

    private static void writeReport(boolean passed, String failure) {
        try {
            var report = new LinkedHashMap<String, Object>();
            report.put("passed", passed);
            report.put("minecraft", "26.3");
            report.put("neoforge", System.getProperty("olympus.testNeoForgeVersion", "26.3.0.92"));
            report.put("renderedFrames", frames);
            report.put("worldTicks", worldTicks);
            report.put("checks", CHECKS);
            report.put("failure", failure);
            Path path = Path.of(System.getProperty("olympus.clientTestReport"));
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            Olympus.LOGGER.info("Olympus client regression passed={}, frames={}, checks={}", passed, frames, CHECKS.size());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not write client regression report", exception);
        }
    }
}
