package dev.xylonity.olympus.registry;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.mixins.RenderTypeAccessor;
import java.util.function.Function;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class OlympusRenderTypes {

    public static final RenderPipeline INVERTED_CUBES_GLOW_PIPELINE = createInvertedCubeGlowPipeline("inverted_cube_glow");
    public static final RenderPipeline FIRST_PERSON_INVERTED_CUBES_GLOW_PIPELINE = createInvertedCubeGlowPipeline("first_person_inverted_cube_glow");

    /// Based off my own inverted cubes render type (if the code is not visible then the mod is not public yet)
    /// https://github.com/Xylonity/Arcane/blob/v1.20.1/common/src/main/java/dev/xylonity/arcane/registry/ArcaneRenderTypes.java#16
    private static RenderPipeline createInvertedCubeGlowPipeline(final String name) {
        return RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET)
            .withLocation(Olympus.of("pipeline/" + name))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withCull(true)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build();
    }

    /// Rendertype applied to a simple spherical geom, mostly created to make it actually visible underwater
    public static final RenderPipeline UNDERWATER_SPLASH_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Olympus.of("pipeline/underwater_splash"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    public static final RenderPipeline TRANSLUCENT_CURIO_GLINT_PIPELINE = RenderPipelines.ENTITY_TRANSLUCENT.toBuilder()
            .withLocation(Olympus.of("pipeline/translucent_curio_glint"))
            .withSnippet(RenderPipelines.GLINT_SNIPPET)
            .build();

    private static final Function<Identifier, RenderType> TRANSLUCENT_CURIO_GLINT = Util.memoize(texture ->
            RenderTypeAccessor.olympus$create("olympus_translucent_curio_glint",
                    RenderSetup.builder(TRANSLUCENT_CURIO_GLINT_PIPELINE)
                            .withTexture("Sampler0", texture)
                            .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
                            .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                            .useLightmap()
                            .useOverlay()
                            .sortOnUpload()
                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup()));

    private static final RenderType UNDERWATER_SPLASH = RenderTypeAccessor.olympus$create(
            "olympus_underwater_splash",
            RenderSetup.builder(UNDERWATER_SPLASH_PIPELINE)
                    .sortOnUpload()
                    .createRenderSetup()
    );

    private static final Function<Identifier, RenderType> INVERTED_CUBES_GLOW = Util.memoize(texture ->
            RenderTypeAccessor.olympus$create(
                    "olympus_inverted_cube_glow",
                    RenderSetup.builder(INVERTED_CUBES_GLOW_PIPELINE)
                            .withTexture("Sampler0", texture)
                            .sortOnUpload()
                            .createRenderSetup()
            )

    );

    private static final Function<Identifier, RenderType> FIRST_PERSON_INVERTED_CUBES_GLOW = Util.memoize(texture ->
            RenderTypeAccessor.olympus$create(
                    "olympus_first_person_inverted_cube_glow",
                    RenderSetup.builder(FIRST_PERSON_INVERTED_CUBES_GLOW_PIPELINE)
                            .withTexture("Sampler0", texture)
                            .sortOnUpload()
                            .createRenderSetup()
            )

    );

    public static RenderType invertedCubesGlow(final Identifier texture) {
        return INVERTED_CUBES_GLOW.apply(texture);
    }

    public static RenderType firstPersonInvertedCubesGlow(final Identifier texture) {
        return FIRST_PERSON_INVERTED_CUBES_GLOW.apply(texture);
    }

    public static RenderType lightningBolt() {
        return RenderTypes.dragonRays();
    }

    public static RenderType translucentEntityComposite(final Identifier texture) {
        return RenderTypes.entityTranslucentCull(texture);
    }

    public static RenderType underwaterSplash() {
        return UNDERWATER_SPLASH;
    }

    public static RenderType translucentCurio(final Identifier texture, final boolean foil) {
        return foil ? TRANSLUCENT_CURIO_GLINT.apply(texture) : RenderTypes.entityTranslucent(texture);
    }

}
