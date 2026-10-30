package dev.xylonity.olympus;


import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.network.OlympusNetwork;
import dev.xylonity.olympus.registry.OlympusAttachments;
import dev.xylonity.olympus.registry.OlympusBlockEntities;
import dev.xylonity.olympus.registry.OlympusBlocks;
import dev.xylonity.olympus.registry.OlympusCreativeModeTabs;
import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.registry.OlympusMobEffects;
import dev.xylonity.olympus.registry.OlympusParticles;
import dev.xylonity.olympus.registry.OlympusSounds;
import dev.xylonity.olympus.registry.OlympusStructureTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Olympus.MOD_ID)
public class OlympusNeoForge {

    public OlympusNeoForge(final IEventBus eventBus, final ModContainer modContainer) {

        OlympusAttachments.ATTACHMENTS.register(eventBus);
        OlympusEntities.ENTITIES.register(eventBus);
        OlympusBlocks.BLOCKS.register(eventBus);
        OlympusBlockEntities.BLOCK_ENTITIES.register(eventBus);
        OlympusItems.ITEMS.register(eventBus);
        OlympusCreativeModeTabs.CREATIVE_MODE_TABS.register(eventBus);
        OlympusMobEffects.MOB_EFFECTS.register(eventBus);
        OlympusParticles.PARTICLES.register(eventBus);
        OlympusSounds.SOUND_EVENTS.register(eventBus);
        OlympusStructureTypes.STRUCTURE_TYPES.register(eventBus);

        eventBus.addListener(OlympusNetwork::registerPayloads);

        modContainer.registerConfig(ModConfig.Type.SYNCED, OlympusConfig.SPEC, "olympus-server.toml");

        Olympus.init();
    }

}
