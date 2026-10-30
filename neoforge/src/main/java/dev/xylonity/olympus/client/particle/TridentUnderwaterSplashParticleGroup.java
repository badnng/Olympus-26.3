package dev.xylonity.olympus.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.olympus.registry.OlympusRenderTypes;
import java.util.List;
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

public final class TridentUnderwaterSplashParticleGroup extends ParticleGroup<TridentUnderwaterSplashParticle> {

    public static final ParticleRenderType TYPE = new ParticleRenderType("OLYMPUS_TRIDENT_UNDERWATER_SPLASHES", "OLYMPUS_TRIDENT_UNDERWATER_SPLASHES");

    private static final int LATITUDE_SEGMENTS = 12;
    private static final int LONGITUDE_SEGMENTS = 24;

    public TridentUnderwaterSplashParticleGroup(final ParticleEngine engine) {
        super(engine);
    }

    @Override
    public @NonNull ParticleGroupRenderState extractRenderState(final Frustum frustum, final Camera camera, final float partialTickTime) {
        final List<TridentUnderwaterSplashParticle.RenderSnapshot> snapshots = particles.stream()
                .filter(particle -> frustum.isVisible(particle.getBoundingBox()))
                .map(particle -> particle.extractSnapshot(camera, partialTickTime))
                .toList();
        return new RenderState(snapshots);
    }

    private static void render(final Matrix4fc pose, final VertexConsumer buffer, final TridentUnderwaterSplashParticle.RenderSnapshot snapshot) {
        if (snapshot.alpha() <= 0.001F) {
            return;
        }

        // Sphere split into quads based on a segment amount (distributed along latitude and longitude)
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            final double latitudeFrom = -Math.PI * 0.5D + Math.PI * latitude / LATITUDE_SEGMENTS;
            final double latitudeTo = -Math.PI * 0.5D + Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                final double longitudeFrom = Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS;
                final double longitudeTo = Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS;

                addVertex(pose, buffer, spherePoint(snapshot, latitudeFrom, longitudeFrom), snapshot);
                addVertex(pose, buffer, spherePoint(snapshot, latitudeTo, longitudeFrom), snapshot);
                addVertex(pose, buffer, spherePoint(snapshot, latitudeTo, longitudeTo), snapshot);
                addVertex(pose, buffer, spherePoint(snapshot, latitudeFrom, longitudeTo), snapshot);
            }

        }

    }

    private static Vec3 spherePoint(final TridentUnderwaterSplashParticle.RenderSnapshot snapshot, final double latitude, final double longitude) {
        final double wave = 1d + Math.sin(longitude * 3d + latitude * 5d - snapshot.progress() * Math.PI * 4d) * 0.012;
        final double radius = snapshot.radius() * wave;
        final double horizontalRadius = Math.cos(latitude) * radius;
        return snapshot.center().add(Math.cos(longitude) * horizontalRadius, Math.sin(latitude) * radius, Math.sin(longitude) * horizontalRadius);
    }

    private static void addVertex(final Matrix4fc pose, final VertexConsumer buffer, final Vec3 position, final TridentUnderwaterSplashParticle.RenderSnapshot snapshot) {
        buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z).setColor(snapshot.red(), snapshot.green(), snapshot.blue(), snapshot.alpha());
    }

    private record RenderState(
            List<TridentUnderwaterSplashParticle.RenderSnapshot> snapshots
    ) implements ParticleGroupRenderState {

        @Override
        public void submit(final @NonNull SubmitNodeCollector submitNodeCollector, final @NonNull CameraRenderState camera) {
            if (snapshots.isEmpty()) {
                return;
            }

            submitNodeCollector.submitCustomGeometry(new PoseStack(), OlympusRenderTypes.underwaterSplash(), (pose, buffer) -> snapshots.forEach(snapshot -> render(pose.pose(), buffer, snapshot)));
        }

    }

}