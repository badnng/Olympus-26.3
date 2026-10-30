package dev.xylonity.olympus.client.compat;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.RenderPipelines;

public final class IrisCompat {

    private static boolean pipelinesRegistered;

    public static void registerRenderPipelines() {
        if (pipelinesRegistered) {
            return;
        }

        // Basically translates my custom rendertypes to the instant vanilla equivalents when iris is present
        try {
            final Class<?> pipelinesClass = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines");
            final Method copyPipeline = pipelinesClass.getMethod("copyPipeline", RenderPipeline.class, RenderPipeline.class);
            copyPipeline.invoke(null, RenderPipelines.EYES, OlympusRenderTypes.INVERTED_CUBES_GLOW_PIPELINE);
            copyPipeline.invoke(null, RenderPipelines.ENTITY_CUTOUT, OlympusRenderTypes.FIRST_PERSON_INVERTED_CUBES_GLOW_PIPELINE);
            copyPipeline.invoke(null, RenderPipelines.LIGHTNING, OlympusRenderTypes.UNDERWATER_SPLASH_PIPELINE);
            pipelinesRegistered = true;
        }
        catch (final ClassNotFoundException ignored) {
            ;;
        }
        catch (final ReflectiveOperationException | LinkageError exception) {
            Olympus.LOGGER.warn("Could not register the Olympus render pipelines with Iris", exception);
        }

    }

}
