package dev.xylonity.olympus.common.entity.projectile;

import dev.xylonity.olympus.common.entity.HarpyEntity;
import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.registry.OlympusParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public final class HarpyProjectileEntity extends Projectile {

    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(HarpyProjectileEntity.class, EntityDataSerializers.INT);

    public static final double SPEED = 0.65D;

    public HarpyProjectileEntity(final EntityType<? extends HarpyProjectileEntity> entityType, final Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public HarpyProjectileEntity(final ServerLevel level, final LivingEntity owner, final LivingEntity target) {
        this(OlympusEntities.HARPY_PROJECTILE.get(), level);
        setOwner(owner);
        entityData.set(TARGET_ID, target.getId());
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {

        updateMovement();

        final Vec3 movement = getDeltaMovement();
        final HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        final boolean impacted = hitResult.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hitResult);

        setPos(impacted ? hitResult.getLocation() : position().add(movement));
        updateRotation();

        super.tick();

        if (impacted && isAlive()) {
            hitTargetOrDeflectSelf(hitResult);
        }

        if (!level().isClientSide() && tickCount >= 200) {
            discard();
        }

    }

    private void updateMovement() {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        final Vec3 direction = target.getBoundingBox().getCenter().subtract(position()).normalize();
        Vec3 currentDirection = getDeltaMovement().normalize();
        if (currentDirection.lengthSqr() < 1.0E-6D) {
            currentDirection = direction;
        }

        final Vec3 normalizedDirection = currentDirection.scale(1.0D - 0.16D).add(direction.scale(0.16D)).normalize();
        setDeltaMovement(normalizedDirection.scale(SPEED));
    }

    private @Nullable LivingEntity getTarget() {
        final Entity target = level().getEntity(entityData.get(TARGET_ID));
        return target instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canHitEntity(final Entity entity) {
        final boolean friendlyHarpy = isHarpy(entity) && isHarpy(getOwner());
        return entity instanceof LivingEntity && !friendlyHarpy && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(final EntityHitResult hitResult) {
        if (level() instanceof ServerLevel serverLevel) {
            playImpactEffects(serverLevel, hitResult.getLocation());
            final Entity owner = getOwner();
            final LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
            final Entity hitEntity = hitResult.getEntity();

            // Damage computation per harpy projectile ignores invulnerability ticks
            final boolean harpy = isHarpy(hitEntity) && isHarpy(owner);
            if (!harpy && hitEntity instanceof LivingEntity target) {
                final DamageSource damageSource = damageSources().mobProjectile(this, livingOwner);
                final boolean ignoresInvulnerabilityTicks = livingOwner instanceof HarpyEntity ownerHarpy && ownerHarpy.isElite();
                final int previousDamageCooldownTime = target.damageCooldownTime;
                if (ignoresInvulnerabilityTicks) {
                    target.damageCooldownTime = 0;
                }

                if (target.hurtServer(serverLevel, damageSource, 4.0F)) {
                    if (livingOwner != null) {
                        livingOwner.setLastHurtMob(target);
                    }

                }
                else if (ignoresInvulnerabilityTicks) {
                    target.damageCooldownTime = previousDamageCooldownTime;
                }

            }

            discard();
        }

    }

    private static boolean isHarpy(final @Nullable Entity entity) {
        return entity instanceof HarpyEntity;
    }

    @Override
    protected void onHitBlock(final BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (level() instanceof ServerLevel serverLevel) {
            playImpactEffects(serverLevel, hitResult.getLocation());
            discard();
        }

    }

    @Override
    protected void onDeflection(final boolean byAttack) {
        super.onDeflection(byAttack);
        if (byAttack) {
            entityData.set(TARGET_ID, -1);
            final Vec3 movement = getDeltaMovement();
            if (movement.lengthSqr() > 1.0E-6D) {
                setDeltaMovement(movement.normalize().scale(SPEED));
            }

        }

    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    private static void playImpactEffects(final ServerLevel level, final Vec3 position) {
        level.sendParticles(OlympusParticles.HARPY_MAGIC.get(), position.x, position.y, position.z, 10, 0.08D, 0.08D, 0.08D, 0.16D);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.8F, 1.5F + level.getRandom().nextFloat() * 0.2F);
    }

}
