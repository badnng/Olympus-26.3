package dev.xylonity.olympus.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ArtemisLeafParticle extends FallingLeavesParticle {

    private final float initialSize;
    private final int initialLifetime;
    private final boolean burst;
    private final double windPhase;
    private final float spinAcceleration;
    private float spinSpeed;

    private ArtemisLeafParticle(final ClientLevel level, final double x, final double y, final double z, final double xSpeed, final double ySpeed, final double zSpeed, final SpriteSet sprites, final RandomSource random, final boolean small) {
        super(level, x, y, z, sprites.get(random), 0.08F, 1.35F, true, true, small ? 1.9F : 2.8F, 0.003F);

        initialSize = quadSize * (0.2F + random.nextFloat() * 0.5F);
        initialLifetime = 38 + random.nextInt(45);
        lifetime = initialLifetime;
        burst = xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed > 0.0016D;
        windPhase = random.nextDouble() * Mth.TWO_PI;
        spinSpeed = (random.nextBoolean() ? 1 : -1) * (0.055F + random.nextFloat() * 0.055F);
        spinAcceleration = (random.nextBoolean() ? 1 : -1) * (0.0008F + random.nextFloat() * 0.0012F);
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        setSize(Mth.clamp(initialSize * 0.65F, 0.025F, 0.07F), Mth.clamp(initialSize * 0.65F, 0.025F, 0.07F));

        final float tint = 0.88F + random.nextFloat() * 0.12F;
        setColor(tint * (small ? 0.98F : 0.92F), tint, tint * (small ? 0.72F : 0.82F));
        setParticleSpeed(xSpeed, ySpeed, zSpeed);
    }

    /// Code based off super
    @Override
    public void tick() {
        if (!burst) {
            super.tick();

            // super removes the particle on block hit
            if (removed && lifetime > 0) {
                removed = false;
            }

            return;
        }

        xo = x;
        yo = y;
        zo = z;

        if (lifetime-- <= 0) {
            remove();
            return;
        }

        age++;
        if (onGround) {
            yd = -0.002D;
        }
        else {
            if (age > 5) {
                yd -= 0.0022D;
            }

            if (age > 8) {
                final double windAngle = windPhase + age * 0.22D;
                xd += Math.cos(windAngle) * 0.00045D;
                zd += Math.sin(windAngle) * 0.00045D;
            }

        }

        spinSpeed = Mth.clamp(spinSpeed + spinAcceleration, -0.16F, 0.16F);
        oRoll = roll;
        roll += spinSpeed;

        move(xd, yd, zd);

        if (onGround) {
            xd *= 0.48D;
            zd *= 0.48D;
            spinSpeed *= 0.82F;
        }
        else {
            final double drag = age <= 10 ? 0.8D : 0.96D;
            xd *= drag;
            yd *= 0.98D;
            zd *= drag;
        }

    }

    @Override
    public void move(final double x, final double y, final double z) {
        Vec3 movement = new Vec3(x, y, z);
        if (hasPhysics && movement.lengthSqr() > 0) {
            movement = Entity.collideBoundingBox((Entity) null, movement, getBoundingBox(), level, List.of());
        }

        if (movement.lengthSqr() > 0) {
            setBoundingBox(getBoundingBox().move(movement));
            setLocationFromBoundingbox();
        }

        onGround = y != movement.y && y < 0;
        if (x != movement.x) {
            xd = 0;
        }
        if (y != movement.y) {
            yd = 0;
        }
        if (z != movement.z) {
            zd = 0;
        }

    }

    @Override
    public float getQuadSize(final float partialTick) {
        final float progress = Mth.clamp(1 - (lifetime - partialTick) / initialLifetime, 0, 1);
        float scale = Mth.clamp((progress - 0.68F) / 0.32F, 0, 1);
        scale = scale * scale * (3 - 2 * scale);
        return initialSize * Mth.lerp(scale, 1, 0);
    }

    public static final class MainProvider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public MainProvider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(final SimpleParticleType options, final ClientLevel level, final double x, final double y, final double z, final double xSpeed, final double ySpeed, final double zSpeed, final RandomSource random) {
            return new ArtemisLeafParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, random, false);
        }

    }

    public static final class SmallProvider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public SmallProvider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(final SimpleParticleType options, final ClientLevel level, final double x, final double y, final double z, final double xSpeed, final double ySpeed, final double zSpeed, final RandomSource random) {
            return new ArtemisLeafParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, random, true);
        }

    }

}