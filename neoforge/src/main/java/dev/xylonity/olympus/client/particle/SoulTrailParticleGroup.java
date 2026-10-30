package dev.xylonity.olympus.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.olympus.Olympus;
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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.joml.Matrix4fc;

/// Based off my own ribbon trail particle implementation, although a big chunk of the code has changed to honor the render rework
/// https://github.com/Xylonity/Knight-Lib/blob/1.20.1/common/src/main/java/dev/xylonity/knightlib/client/particle/AbstractRibbonTrailParticle.java
public final class SoulTrailParticleGroup extends ParticleGroup<SoulTrailParticle> {

    public static final ParticleRenderType TYPE = new ParticleRenderType("OLYMPUS_SOUL_TRAILS", "OLYMPUS_SOUL_TRAILS");

    private static final RenderType RENDERTYPE = OlympusRenderTypes.translucentEntityComposite(Olympus.of("textures/particle/soul_trail.png"));

    public SoulTrailParticleGroup(final ParticleEngine engine) {
        super(engine);
    }

    @Override
    public @NonNull ParticleGroupRenderState extractRenderState(final Frustum frustum, final Camera camera, final float partialTickTime) {
        // Freezes visible trails
        final List<SoulTrailParticle.RenderSnapshot> snapshots = particles.stream()
                .filter(particle -> frustum.isVisible(particle.getBoundingBox()))
                .map(particle -> particle.extractSnapshot(camera, partialTickTime))
                .filter(Objects::nonNull)
                .toList();

        return new RenderState(snapshots);
    }

    private static void render(final Matrix4fc pose, final VertexConsumer buffer, final SoulTrailParticle.RenderSnapshot snapshot) {
        final List<Vec3> points = snapshot.points();
        if (points.size() < 2) {
            return;
        }

        final List<CrossSection> sections = buildSections(points, snapshot.width());
        for (int index = 0; index < sections.size() - 1; index++) {
            final CrossSection from = sections.get(index);
            final CrossSection to = sections.get(index + 1);
            // Render for both sides
            addSegment(pose, buffer, from, to, false);
            addSegment(pose, buffer, from, to, true);
        }

    }

    private static List<CrossSection> buildSections(final List<Vec3> points, final float baseWidth) {
        final double totalLength = pathLength(points);
        final List<CrossSection> sections = new ArrayList<>(points.size());
        Vec3 previousRight = null;
        double distance = 0;

        for (int index = 0; index < points.size(); index++) {
            final Vec3 point = points.get(index);
            if (index > 0) {
                distance += points.get(index - 1).distanceTo(point);
            }

            // Using both neighbours on inner points keeps turns smoother than following a single segment
            final Vec3 line;
            if (index == 0) {
                line = points.get(1).subtract(point);
            }
            else if (index == points.size() - 1) {
                line = point.subtract(points.get(index - 1));
            }
            else {
                line = points.get(index + 1).subtract(points.get(index - 1));
            }

            final Vec3 direction = line.normalize();
            Vec3 right = direction.cross(point.scale(-1D));
            if (right.lengthSqr() < 1.0E-8D) {
                right = direction.cross(Math.abs(direction.y) > 0.9 ? Vec3.X_AXIS : Vec3.Y_AXIS);
            }

            right = right.normalize();
            // Keeping the same side along the path so the ribbon doesn't randomly twist
            if (previousRight != null && previousRight.dot(right) < 0D) {
                right = right.scale(-1D);
            }

            previousRight = right;

            final float progress = totalLength < 1.0E-5D ? 0F : (float) (distance / totalLength);
            // The trail width decreases based on the distance
            final float width = baseWidth * (1F - progress * 0.55F);
            sections.add(new CrossSection(point, right, width, progress));
        }

        return sections;
    }

    private static double pathLength(final List<Vec3> points) {
        double length = 0.0D;
        for (int index = 1; index < points.size(); index++) {
            length += points.get(index - 1).distanceTo(points.get(index));
        }

        return length;
    }

    private static void addSegment(final Matrix4fc pose, final VertexConsumer buffer, final CrossSection from, final CrossSection to, final boolean reverse) {
        final Vec3 fromLeft = from.offset(1);
        final Vec3 fromRight = from.offset(-1);
        final Vec3 toLeft = to.offset(1);
        final Vec3 toRight = to.offset(-1);

        if (reverse) {
            addVertex(pose, buffer, fromLeft, from.progress, 0);
            addVertex(pose, buffer, toLeft, to.progress, 0);
            addVertex(pose, buffer, toRight, to.progress, 1);
            addVertex(pose, buffer, fromRight, from.progress, 1);
        }
        else {
            addVertex(pose, buffer, fromLeft, from.progress, 0);
            addVertex(pose, buffer, fromRight, from.progress, 1);
            addVertex(pose, buffer, toRight, to.progress, 1);
            addVertex(pose, buffer, toLeft, to.progress, 0);
        }

    }

    private static void addVertex(final Matrix4fc pose, final VertexConsumer buffer, final Vec3 position, final float u, final float v) {
        buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(0xBD, 0x00, 0x1A, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }

    private record RenderState(
            List<SoulTrailParticle.RenderSnapshot> snapshots
    ) implements ParticleGroupRenderState {

        @Override
        public void submit(final @NonNull SubmitNodeCollector submitNodeCollector, final @NonNull CameraRenderState camera) {
            if (snapshots.isEmpty()) {
                return;
            }

            // Every trail shares a render type, so one geometry submission should be enough for the whole group
            submitNodeCollector.submitCustomGeometry(new PoseStack(), RENDERTYPE,
                    (pose, buffer) -> snapshots.forEach(snapshot -> render(pose.pose(), buffer, snapshot))
            );

        }

    }

    private record CrossSection(
            Vec3 center,
            Vec3 right,
            float halfWidth,
            float progress
    ) {

        private Vec3 offset(final float multiplier) {
            return center.add(right.scale(halfWidth * multiplier));
        }

    }

}
