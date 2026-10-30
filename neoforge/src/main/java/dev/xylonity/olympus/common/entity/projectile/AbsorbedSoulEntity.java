package dev.xylonity.olympus.common.entity.projectile;

import dev.xylonity.olympus.common.item.PersephoneCupItem;
import dev.xylonity.olympus.network.payload.SoulSalvationPayload;
import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.registry.OlympusItems;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Clone of the experience orb entity logic with tweaked movement
 */
public final class AbsorbedSoulEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(AbsorbedSoulEntity.class, EntityDataSerializers.INT);

    private static final String TAG_TARGET_PLAYER = "TargetPlayer";
    private static final String TAG_AGE = "Age";

    private static final int HOMING_DELAY = 20;
    private static final double RADIUS = 20;

    private @Nullable UUID targetPlayerUuid;
    private int age;

    public AbsorbedSoulEntity(final EntityType<? extends AbsorbedSoulEntity> entityType, final Level level) {
        super(entityType, level);
    }

    public AbsorbedSoulEntity(final Level level, final Vec3 position, final ServerPlayer targetPlayer) {
        this(OlympusEntities.ABSORBED_SOUL.get(), level);
        setPos(position);
        setYRot(random.nextFloat() * 360f);
        setDeltaMovement((random.nextDouble() * 0.2 - 0.1) * 2d, random.nextDouble() * 0.4D, (random.nextDouble() * 0.2 - 0.1) * 2d);
        targetPlayerUuid = targetPlayer.getUUID();
        entityData.set(DATA_TARGET_ID, targetPlayer.getId());
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET_ID, -1);
    }

    @Override
    public void tick() {
        if (firstTick && level().isClientSide()) {
            firstTick = false;
            return;
        }

        super.tick();

        // Despawns the soul
        if (++age >= 6000) {
            discard();
            return;
        }

        // Lerps towards the target player
        final Player targetPlayer = findTargetPlayer();
        Vec3 movement = getDeltaMovement();
        if (targetPlayer != null && age > HOMING_DELAY) {
            movement = moveTowards(targetPlayer, movement);
        }
        else {
            movement = movement.add(0, -getDefaultGravity(), 0);
        }

        setDeltaMovement(movement);
        final double previousVerticalMovement = movement.y;

        move(MoverType.SELF, movement);

        movement = getDeltaMovement().scale(onGround() ? 0.588D : 0.98D);
        if (verticalCollisionBelow && previousVerticalMovement < 0) {
            movement = new Vec3(movement.x, -previousVerticalMovement * 0.4, movement.z);
        }

        setDeltaMovement(movement);
    }

    private @Nullable Player findTargetPlayer() {
        final MinecraftServer server = level().getServer();
        if (!level().isClientSide() && targetPlayerUuid != null && server != null) {
            final PlayerList players = server.getPlayerList();
            final ServerPlayer targetPlayer = players.getPlayer(targetPlayerUuid);
            final int targetId = targetPlayer == null || targetPlayer.level() != level() ? -1 : targetPlayer.getId();
            if (entityData.get(DATA_TARGET_ID) != targetId) {
                entityData.set(DATA_TARGET_ID, targetId);
            }

            return isValidTarget(targetPlayer) ? targetPlayer : null;
        }

        final Entity target = level().getEntity(entityData.get(DATA_TARGET_ID));
        return target instanceof Player player && isValidTarget(player) ? player : null;
    }

    private boolean isValidTarget(final @Nullable Player player) {
        return player != null && !player.isSpectator() && !player.isDeadOrDying() && player.distanceToSqr(this) <= RADIUS * RADIUS;
    }

    private Vec3 moveTowards(final Player targetPlayer, final Vec3 movement) {
        final Vec3 targetPosition = targetPlayer.position().add(0, targetPlayer.getEyeHeight() * 0.5, 0);
        final Vec3 direction = targetPosition.subtract(position().add(0, getBbHeight() * 0.5, 0));
        final double distance = direction.length();
        if (distance < 1.0E-5D) {
            return movement;
        }

        final double proximity = 1d - Math.min(distance / RADIUS, 1d);
        final double targetSpeed = 0.2 + 0.4 * proximity;
        final double steer = 0.08 + proximity * 0.12;
        return movement.lerp(direction.scale(targetSpeed / distance), steer);
    }

    @Override
    public void playerTouch(final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || age <= HOMING_DELAY || player.getId() != entityData.get(DATA_TARGET_ID) || player.takeXpDelay != 0) {
            return;
        }

        // Checks if the player's current curios inventory has a persephone's cup
        final Optional<ItemStack> equippedCup = CuriosApi.getCuriosInventory(serverPlayer)
                .flatMap(handler -> handler.findFirstCurio(OlympusItems.PERSEPHONE_CUP.get())).map(SlotResult::stack);
        if (equippedCup.isEmpty()) {
            return;
        }

        // Adds a charge to the cup
        PersephoneCupItem.addSoulCharge(equippedCup.get());
        // Experience orb pickup sound
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, (random.nextFloat() - random.nextFloat()) * 0.35F + 0.9F);
        // Spawns particles
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new SoulSalvationPayload(serverPlayer.getId(), 3 + random.nextInt(2), true));

        player.takeXpDelay = 2;
        player.take(this, 1);

        discard();
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        if (targetPlayerUuid != null) {
            output.store(TAG_TARGET_PLAYER, UUIDUtil.CODEC, targetPlayerUuid);
        }

        output.putInt(TAG_AGE, age);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        targetPlayerUuid = input.read(TAG_TARGET_PLAYER, UUIDUtil.CODEC).orElse(null);
        age = input.getIntOr(TAG_AGE, 0);
    }

    @Override
    protected @NonNull MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03D;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource damageSource, final float amount) {
        return false;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this);
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.SERVER_AND_CLIENT;
    }

}
