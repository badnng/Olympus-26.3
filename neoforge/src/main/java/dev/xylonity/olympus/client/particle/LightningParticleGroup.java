package dev.xylonity.olympus.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;

/// Based off my own implementation
/// https://github.com/Xylonity/Hostiles/blob/v1.20.1/common/src/main/java/dev/xylonity/hostiles/client/particle/GroundToTargetLightningParticle.java
public final class LightningParticleGroup extends ParticleGroup<LightningBoltParticle> {

    public static final ParticleRenderType TYPE = new ParticleRenderType("OLYMPUS_LIGHTNING_BOLTS", "OLYMPUS_LIGHTNING_BOLTS");

    public LightningParticleGroup(final ParticleEngine engine) {
        super(engine);
    }

    @Override
    public @NonNull ParticleGroupRenderState extractRenderState(final Frustum frustum, final Camera camera, final float partialTickTime) {
        final List<LightningBoltParticle.RenderSnapshot> snapshots = particles.stream()
                .filter(particle -> frustum.isVisible(particle.getBoundingBox()))
                .map(particle -> particle.extractSnapshot(camera, partialTickTime))
                .filter(Objects::nonNull)
                .toList();

        return new State(snapshots);
    }

    private static void render(final Matrix4fc pose, final VertexConsumer buffer, final LightningBoltParticle.RenderSnapshot snapshot) {
        final List<Vec3> points = snapshot.points();
        if (points.size() < 2) {
            return;
        }

        final List<CrossSection> sections = buildCrossSections(points, snapshot.width());

        // Artificial glow (the first gradient is less noticeable and larger)
        drawGradientGlow(pose, buffer, sections, 6.6F, 0.28F * snapshot.alpha(), 1.0F, 1.0F, 0.02F);
        drawGradientGlow(pose, buffer, sections, 3.2F, 0.56F * snapshot.alpha(), 1.0F, 1.0F, 0.08F);

        // Internal part
        drawCore(pose, buffer, sections, snapshot.alpha(), 1.0F, 1.0F, 0.7F);
    }

    private static List<CrossSection> buildCrossSections(final List<Vec3> points, final float baseWidth) {
        final List<CrossSection> sections = new ArrayList<>(points.size());
        Vec3 previousRight = null;
        for (int index = 0; index < points.size(); index++) {
            final Vec3 point = points.get(index);
            final Vec3 tangent;
            if (index == 0) {
                tangent = points.get(1).subtract(point);
            }
            else if (index == points.size() - 1) {
                tangent = point.subtract(points.get(index - 1));
            }
            else {
                tangent = points.get(index + 1).subtract(points.get(index - 1));
            }

            final Vec3 direction = tangent.normalize();

            // Points are already relative to the camera so this should keep every vertex facing the camera
            Vec3 right = direction.cross(point.scale(-1));
            if (right.lengthSqr() < 1.0E-8D) {
                right = direction.cross(Math.abs(direction.y) > 0.9D ? Vec3.X_AXIS : Vec3.Y_AXIS);
            }

            right = right.normalize();
            if (previousRight != null && previousRight.dot(right) < 0) {
                right = right.scale(-1);
            }

            previousRight = right;

            final float progress = index / (float) (points.size() - 1);
            sections.add(new CrossSection(point, right, baseWidth * (1.0F - progress * 0.32F)));
        }

        return sections;
    }

    private static void drawGradientGlow(final Matrix4fc pose, final VertexConsumer buffer, final List<CrossSection> sections, final float widthMultiplier, final float innerAlpha, final float red, final float green, final float blue) {
        if (innerAlpha <= 0.001F) {
            return;
        }

        for (int index = 0; index < sections.size() - 1; index++) {
            final CrossSection from = sections.get(index);
            final CrossSection to = sections.get(index + 1);
            final Vec3 fromLeft = from.offset(widthMultiplier);
            final Vec3 toLeft = to.offset(widthMultiplier);
            final Vec3 fromRight = from.offset(-widthMultiplier);
            final Vec3 toRight = to.offset(-widthMultiplier);

            // Each half fades from the center line towards a fully transparent outer edge
            addDoubleSidedQuad(
                    pose, buffer,
                    new ColoredVertex(from.center, red, green, blue, innerAlpha),
                    new ColoredVertex(fromLeft, red, green, blue, 0),
                    new ColoredVertex(toLeft, red, green, blue, 0),
                    new ColoredVertex(to.center, red, green, blue, innerAlpha)
            );
            addDoubleSidedQuad(
                    pose, buffer,
                    new ColoredVertex(from.center, red, green, blue, innerAlpha),
                    new ColoredVertex(to.center, red, green, blue, innerAlpha),
                    new ColoredVertex(toRight, red, green, blue, 0),
                    new ColoredVertex(fromRight, red, green, blue, 0)
            );

        }

    }

    private static void drawCore(final Matrix4fc pose, final VertexConsumer buffer, final List<CrossSection> sections, final float alpha, final float red, final float green, final float blue) {
        for (int index = 0; index < sections.size() - 1; index++) {
            final CrossSection from = sections.get(index);
            final CrossSection to = sections.get(index + 1);
            addDoubleSidedQuad(
                    pose, buffer,
                    new ColoredVertex(from.offset(1.0F), red, green, blue, alpha),
                    new ColoredVertex(from.offset(-1.0F), red, green, blue, alpha),
                    new ColoredVertex(to.offset(-1.0F), red, green, blue, alpha),
                    new ColoredVertex(to.offset(1.0F), red, green, blue, alpha)
            );

        }

    }

    private static void addDoubleSidedQuad(final Matrix4fc pose, final VertexConsumer buffer, final ColoredVertex first, final ColoredVertex second, final ColoredVertex third, final ColoredVertex fourth) {
        // Inverse quads so the bolt is visible from both sides
        addTriangle(pose, buffer, first, second, third);
        addTriangle(pose, buffer, first, third, fourth);
        addTriangle(pose, buffer, third, second, first);
        addTriangle(pose, buffer, fourth, third, first);
    }

    private static void addTriangle(final Matrix4fc pose, final VertexConsumer buffer, final ColoredVertex first, final ColoredVertex second, final ColoredVertex third) {
        addVertex(pose, buffer, first);
        addVertex(pose, buffer, second);
        addVertex(pose, buffer, third);
    }

    private static void addVertex(final Matrix4fc pose, final VertexConsumer buffer, final ColoredVertex vertex) {
        addVertex(pose, buffer, vertex.position, vertex.red, vertex.green, vertex.blue, vertex.alpha);
    }

    private static void addVertex(final Matrix4fc pose, final VertexConsumer buffer, final Vec3 position, final float red, final float green, final float blue, final float alpha) {
        buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z).setColor(red, green, blue, alpha);
    }

    private record State(
            List<LightningBoltParticle.RenderSnapshot> snapshots
    ) implements ParticleGroupRenderState {

        @Override
        public void submit(final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
            if (snapshots.isEmpty()) {
                return;
            }

            submitNodeCollector.submitCustomGeometry(new PoseStack(), OlympusRenderTypes.lightningBolt(),
                    (pose, buffer) -> snapshots.forEach(snapshot -> render(pose.pose(), buffer, snapshot))
            );

        }

    }

    private record CrossSection(
            Vec3 center,
            Vec3 right,
            float halfWidth
    ) {

        private Vec3 offset(final float multiplier) {
            return center.add(right.scale(halfWidth * multiplier));
        }

    }

    private record ColoredVertex(
            Vec3 position,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        ;;
    }

}
