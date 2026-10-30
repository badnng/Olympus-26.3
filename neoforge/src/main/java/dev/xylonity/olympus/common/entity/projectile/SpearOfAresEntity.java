package dev.xylonity.olympus.common.entity.projectile;

import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.registry.OlympusParticles;
import dev.xylonity.olympus.registry.OlympusSounds;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

public final class SpearOfAresEntity extends ThrownTrident {

    // First tick in which the spear collides with an entity or block
    private static final EntityDataAccessor<Long> COLLISION_START_TICK = SynchedEntityData.defineId(SpearOfAresEntity.class, EntityDataSerializers.LONG);

    private static final String TAG_COLLISION_START_TICK = "CollisionStartTick";

    private static final int DISSOLVE_DELAY = 60;
    private static final int DISSOLVE_DURATION = 20;

    // Entities already hit by this spear
    private final IntSet piercedEntityIds = new IntOpenHashSet();
    // Entities currently moving with the spear
    private final IntSet pinnedEntityIds = new IntOpenHashSet();
    // Entities pinned during the current tick
    private final IntSet newlyPinnedEntityIds = new IntOpenHashSet();

    private boolean pinning;
    private boolean hitWallThisTick;
    private boolean nailedSurfaceImpactPending;
    private double pinDistanceTraveled;

    public SpearOfAresEntity(final EntityType<? extends SpearOfAresEntity> entityType, final Level level) {
        super(entityType, level);
        pickup = Pickup.DISALLOWED;
    }

    public SpearOfAresEntity(final Level level, final LivingEntity owner, final ItemStack stack) {
        this(OlympusEntities.SPEAR_OF_ARES.get(), level);
        init(stack);
        setOwner(owner);
        pickup = Pickup.DISALLOWED;
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    public SpearOfAresEntity(final Level level, final double x, final double y, final double z, final ItemStack stack) {
        this(OlympusEntities.SPEAR_OF_ARES.get(), level);
        init(stack);
        setPos(x, y, z);
    }

    private void init(final ItemStack stack) {
        // Keeps a single copy of the thrown item
        final ItemStack projectileStack = stack.copyWithCount(1);
        setPickupItemStack(projectileStack);
        applyComponentsFromItemStack(projectileStack);
        pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(COLLISION_START_TICK, -1L);
    }

    @Override
    public void tick() {
        // Deletes the spear after a short time after colliding with a surface
        if (!level().isClientSide() && getCollisionAge(0) >= DISSOLVE_DELAY + DISSOLVE_DURATION) {
            discard();
            return;
        }

        final Vec3 previousPosition = position();
        final boolean wasPinning = pinning;
        newlyPinnedEntityIds.clear();
        hitWallThisTick = false;

        super.tick();

        if (!isAlive()) {
            return;
        }

        // Particles
        if (!isInGround() && level() instanceof ServerLevel serverLevel) {
            particleTrail(serverLevel, previousPosition);
        }

        // Enabled on entity hit
        if (!pinning) {
            return;
        }

        // Tracks how far pinned entities have traveled
        final Vec3 movement = position().subtract(previousPosition);
        if (wasPinning) {
            pinDistanceTraveled += movement.length();
        }

        // Releases pinned entities after the max length has been reached
        if (!(level() instanceof ServerLevel serverLevel)) {
            if (pinDistanceTraveled >= OlympusConfig.INSTANCE.aresSpearPinnedEntityDistance.get()) {
                releasePinnedEntities();
            }

            return;
        }

        // Moves every pinned entity with the spear
        final boolean pinnedEntityHitWall = movePinnedEntities(serverLevel, movement);
        // Computed if entities hit a wall this tick
        if (hitWallThisTick) {
            applyWallImpactEffects(serverLevel);
            finishPinningAtSurface();
            return;
        }

        // Computed if entities hit a wall this tick too
        if (pinnedEntityHitWall) {
            applyWallImpactEffects(serverLevel);
            nailedSurfaceImpactPending = true;
            releasePinnedEntities();
            return;
        }

        if (pinDistanceTraveled >= OlympusConfig.INSTANCE.aresSpearPinnedEntityDistance.get()) {
            releasePinnedEntities();
        }

    }

    @Override
    protected @NonNull Collection<EntityHitResult> findHitEntities(final @NonNull Vec3 from, final @NonNull Vec3 to) {
        return ProjectileUtil.getManyEntityHitResult(level(), this, from, to, getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0D), this::canHitEntity, false, false);
    }

    @Override
    protected boolean canHitEntity(final Entity entity) {
        // Prevents hitting the same entity twice
        return !piercedEntityIds.contains(entity.getId()) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(final EntityHitResult hitResult) {
        // Registers the entity as pierced and pinned
        final Entity target = hitResult.getEntity();
        if (!piercedEntityIds.add(target.getId())) {
            return;
        }

        pinnedEntityIds.add(target.getId());
        newlyPinnedEntityIds.add(target.getId());
        startCollisionLifetime();

        if (!pinning) {
            beginPinning();
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Applies trident damage and enchantment effects
        final Entity currentOwner = getOwner();
        final DamageSource damageSource = damageSources().trident(this, currentOwner == null ? this : currentOwner);
        final float configuredDamage = OlympusConfig.INSTANCE.aresSpearProjectileDamage.get().floatValue();
        final float damage = EnchantmentHelper.modifyDamage(serverLevel, getWeaponItem(), target, damageSource, configuredDamage);
        if (target.hurtServer(serverLevel, damageSource, damage)) {
            if (currentOwner instanceof LivingEntity livingOwner) {
                livingOwner.setLastHurtMob(target);
            }

            EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(serverLevel, target, damageSource, getWeaponItem(), _ -> kill(serverLevel));

            if (target instanceof LivingEntity livingTarget) {
                doPostHurtEffects(livingTarget);
            }

        }

    }

    private void beginPinning() {
        pinDistanceTraveled = 0;
        pinning = true;
    }

    @Override
    protected void onHitBlock(final @NonNull BlockHitResult hitResult) {
        // Keeps the impact rotation after the vanilla collision (arrow logic for surface collision)
        final boolean nailedEntities = hasNailedEntitiesAtSurface();
        final float impactYRot = getYRot();
        final float impactXRot = getXRot();

        startCollisionLifetime();
        hitWallThisTick = pinning;

        // super arrow caches its ground hit sound when the projectile itself is constructed before entities are pinned
        setSoundEvent(nailedEntities ? OlympusSounds.ARES_SPEAR_NAILING.get() : OlympusSounds.ARES_SPEAR_SURFACE_HIT.get());

        if (nailedEntities && level() instanceof ServerLevel serverLevel) {
            final Vec3 pos = hitResult.getLocation().add(hitResult.getDirection().getUnitVec3().scale(0.01D));
            serverLevel.sendParticles(OlympusParticles.ARES_SPEAR_HIT.get(), pos.x, pos.y, pos.z, 0, 3.5, 0, 0, 1);
        }

        super.onHitBlock(hitResult);

        setYRot(impactYRot);
        setXRot(impactXRot);
        yRotO = impactYRot;
        xRotO = impactXRot;

        nailedSurfaceImpactPending = false;
    }

    private boolean movePinnedEntities(final ServerLevel level, final Vec3 movement) {
        // Ignores stuck entities
        if (movement.lengthSqr() <= 1.0E-8D) {
            return false;
        }

        // Moves valid targets
        boolean hitWall = false;
        final IntIterator iterator = pinnedEntityIds.iterator();
        // Iterates over each pierced entity
        while (iterator.hasNext()) {
            final int entityId = iterator.nextInt();
            final Entity target = level.getEntity(entityId);
            // Irrelevant entities
            if (target == null || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            // Already pierced entities
            if (newlyPinnedEntityIds.contains(entityId)) {
                continue;
            }

            // Actually moves the current entity, negates its movement and applies basically the same displacement as the spear itself
            final Vec3 previousPosition = target.position();
            target.setDeltaMovement(Vec3.ZERO);
            target.move(MoverType.SELF, movement);
            target.setDeltaMovement(Vec3.ZERO);
            target.syncVelocity = true;

            // Whether if the entity hit a wall
            final Vec3 actualMovement = target.position().subtract(previousPosition);
            final double xDiff = actualMovement.x - movement.x;
            final double zDiff = actualMovement.z - movement.z;
            if (xDiff * xDiff + zDiff * zDiff > 0.0025D) {
                hitWall = true;
            }

        }

        return hitWall;
    }

    private void applyWallImpactEffects(final ServerLevel level) {
        final Entity currentOwner = getOwner();
        final DamageSource damageSource = damageSources().trident(this, currentOwner == null ? this : currentOwner);
        final IntIterator iterator = pinnedEntityIds.iterator();
        while (iterator.hasNext()) {
            final Entity target = level.getEntity(iterator.nextInt());
            if (target instanceof LivingEntity entity && target.isAlive()) {
                // Prevents invulnerability window from taking effect, so the damage on collision is applied properly
                final int previousDamageCooldownTime = entity.damageCooldownTime;
                entity.damageCooldownTime = 0;
                if (!entity.hurtServer(level, damageSource, OlympusConfig.INSTANCE.aresSpearWallImpactDamage.get().floatValue())) {
                    entity.damageCooldownTime = previousDamageCooldownTime;
                }

                final int slownessTicks = OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.aresSpearWallSlownessSeconds.get());
                if (slownessTicks > 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slownessTicks, 2, false, false, false));
                }
            }

        }

    }

    private void releasePinnedEntities() {
        pinning = false;
        pinnedEntityIds.clear();
        newlyPinnedEntityIds.clear();
    }

    private void finishPinningAtSurface() {
        pinning = false;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        pinnedEntityIds.clear();
        newlyPinnedEntityIds.clear();
    }

    private boolean hasNailedEntitiesAtSurface() {
        return (pinnedEntityIds != null && !pinnedEntityIds.isEmpty()) || nailedSurfaceImpactPending;
    }

    private void startCollisionLifetime() {
        // Starts the dissolve timer only once on the server
        if (!level().isClientSide() && entityData.get(COLLISION_START_TICK) < 0L) {
            entityData.set(COLLISION_START_TICK, level().getGameTime());
        }

    }

    private float getCollisionAge(final float partialTick) {
        final long collisionStartTick = entityData.get(COLLISION_START_TICK);
        return collisionStartTick < 0L ? -1 : level().getGameTime() - collisionStartTick + partialTick;
    }

    public float getDissolveVisibility(final float partialTick) {
        // Converts collision age into a visibility value
        final float dissolveAge = getCollisionAge(partialTick) - DISSOLVE_DELAY;
        if (dissolveAge <= 0.0F) {
            return 1;
        }

        return Math.clamp(1f - dissolveAge / DISSOLVE_DURATION, 0, 1);
    }

    // Just some trail particles sampled to the whole movement trail of the spear so the spear itself remains visible
    private void particleTrail(final ServerLevel level, final Vec3 previousPosition) {
        final Vec3 movement = position().subtract(previousPosition);
        if (movement.lengthSqr() <= 1.0E-8D) {
            return;
        }

        final int samples = Math.clamp((int) Math.ceil(movement.length() * 2), 1, 5);
        final Vec3 speed = movement.normalize().scale(-0.03);
        for (int sample = 0; sample < samples; sample++) {
            final Vec3 pos = previousPosition.lerp(position(), (sample + 0.5) / samples);
            level.sendParticles(OlympusParticles.ARES_SPEAR_TRACE.get(), pos.x, pos.y, pos.z, 0, speed.x, speed.y, speed.z, 1);
        }

    }

    @Override
    protected boolean tryPickup(final @NonNull Player player) {
        return false;
    }

    @Override
    protected @NonNull ItemStack getDefaultPickupItem() {
        return new ItemStack(OlympusItems.SPEAR_OF_ARES.get());
    }

    @Override
    public void playSound(final SoundEvent sound, final float volume, final float pitch) {
        super.playSound(sound, sound == OlympusSounds.ARES_SPEAR_NAILING.get() ? 2 : volume, pitch);
    }

    @Override
    protected @NonNull SoundEvent getDefaultHitGroundSoundEvent() {
        return hasNailedEntitiesAtSurface() ? OlympusSounds.ARES_SPEAR_NAILING.get() : OlympusSounds.ARES_SPEAR_SURFACE_HIT.get();
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(COLLISION_START_TICK, input.getLongOr(TAG_COLLISION_START_TICK, -1L));
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        final long collisionStartTick = entityData.get(COLLISION_START_TICK);
        if (collisionStartTick >= 0) {
            output.putLong(TAG_COLLISION_START_TICK, collisionStartTick);
        }

    }

}
