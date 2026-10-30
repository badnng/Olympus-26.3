package dev.xylonity.olympus.common.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import dev.xylonity.olympus.client.item.renderer.SpearOfAresItemRenderer;
import dev.xylonity.olympus.common.entity.projectile.SpearOfAresEntity;
import dev.xylonity.olympus.common.util.OlympusTooltip;
import dev.xylonity.olympus.common.entity.projectile.SummoningSpearsEntity;
import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.network.payload.CameraShakePayload;
import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.registry.OlympusParticles;
import dev.xylonity.olympus.registry.OlympusSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class SpearOfAresItem extends TridentItem implements GeoItem {

    private static final String TAG_SPECIAL_ABILITY_CHARGED = "olympus_special_ability_charged";
    private static final String TAG_SPECIAL_ABILITY_COOLDOWN_END = "olympus_special_ability_cooldown_end";
    private static final String TAG_PLAYER_SPECIAL_FALL = "olympus_ares_special_fall";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpearOfAresItem(final Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    public static boolean isSpecialAbilityCharged(final ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(TAG_SPECIAL_ABILITY_CHARGED, false);
    }

    public static void setSpecialAbilityCharged(final ItemStack stack, final Player player, final boolean charged) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (charged) {
                if (!isSpecialAbilityCharged(stack)) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.4f, 1f);
                }

                tag.putBoolean(TAG_SPECIAL_ABILITY_CHARGED, true);
            }
            else {
                tag.remove(TAG_SPECIAL_ABILITY_CHARGED);
            }

        });

        if (charged) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(1.0F), List.of(), List.of(), List.of()));
        }
        else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }

    }

    public static boolean isSpecialAbilityReady(final ItemStack stack, final Level level) {
        final long cooldownEnd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(TAG_SPECIAL_ABILITY_COOLDOWN_END, 0L);
        return level.getGameTime() >= cooldownEnd;
    }

    public static void startSpecialAbilityCooldown(final ItemStack stack, final Level level) {
        final int cooldownTicks = OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.aresSpearAbilityCooldownSeconds.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag ->
                tag.putLong(TAG_SPECIAL_ABILITY_COOLDOWN_END, level.getGameTime() + cooldownTicks)
        );

    }

    public static void chargeSpecialAbilityForKill(final ServerPlayer player, final DamageSource damageSource) {
        // On entity kill, checks if the spear was the reason
        final ItemStack spear = findSpearUsedForKill(player, damageSource);
        if (!spear.isEmpty()) {
            setSpecialAbilityCharged(spear, player, true);
        }

    }

    public static void updateSpecialFall(final ServerPlayer player) {
        // If the player is executing the special ability
        if (isActiveFallActive(player)) {
            // On ground hit
            if (player.onGround()) {
                finishActiveFallAbility(player);
                return;
            }

            // On ground hit (if it's water or if the player starts flying), the ability is canceled
            if (player.isInWater() || player.isFallFlying() || player.getAbilities().flying) {
                player.getPersistentData().remove(TAG_PLAYER_SPECIAL_FALL);
                return;
            }

            // Extra speed for the player
            acceleratePlayer(player);

            return;
        }

        // Checks if the player should activate the active ability
        if (player.onGround() || !tryActiveAbility(player, player.fallDistance)) {
            return;
        }

        acceleratePlayer(player);
    }

    public static boolean handleSpecialLanding(final ServerPlayer player, final double fallDistance) {
        if (!isActiveFallActive(player) && !tryActiveAbility(player, fallDistance)) {
            return false;
        }

        return finishActiveFallAbility(player);
    }

    /// Applies the special effect of the helmet of spear (summoning spears come from the ground on ground hit on certain conditions)
    private static boolean tryActiveAbility(ServerPlayer player, double fallDistance) {
        if (fallDistance < OlympusConfig.INSTANCE.aresSpearAbilityMinimumFallDistance.get() || !player.isShiftKeyDown() || player.isInWater() || player.isFallFlying() || player.getAbilities().flying) {
            return false;
        }

        final ItemStack spear = findUsableHeldSpear(player);
        if (spear.isEmpty()) {
            return false;
        }

        // Starts the special ability
        setSpecialAbilityCharged(spear, player, false);
        // The active ability has a different cooldown
        startSpecialAbilityCooldown(spear, player.level());
        player.getPersistentData().putBoolean(TAG_PLAYER_SPECIAL_FALL, true);

        return true;
    }

    private static boolean finishActiveFallAbility(final ServerPlayer player) {
        if (!isActiveFallActive(player) || player.isInWater() || player.isFallFlying() || player.getAbilities().flying) {
            return false;
        }

        player.getPersistentData().remove(TAG_PLAYER_SPECIAL_FALL);
        player.resetFallDistance();

        final ServerLevel level = player.level();
        final Vec3 pos = player.position();
        level.playSound(null, pos.x, pos.y, pos.z, OlympusSounds.ARES_SPEAR_LANDING.get(), SoundSource.PLAYERS, 1, 1);
        level.addFreshEntity(new SummoningSpearsEntity(level, player));
        spawnLandingParticles(level, pos);

        // Camera shake
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, 6, new CameraShakePayload(pos, 6, 1.1f, 30));

        return true;
    }

    private static void spawnLandingParticles(final ServerLevel level, final Vec3 center) {
        final RandomSource random = level.getRandom();
        final double radius = 3;
        final int particles = 6;
        final BlockPos pos = findParticleGround(level, center.x, center.y, center.z, radius);
        if (pos == null) {
            return;
        }

        for (int cluster = 0; cluster < 20; cluster++) {
            final double angle = random.nextDouble() * Math.PI * 2;
            final double distance = Math.sqrt(random.nextDouble()) * radius;
            final double x = center.x + Math.cos(angle) * distance;
            final double z = center.z + Math.sin(angle) * distance;
            final BlockPos particleGround = findParticleGround(level, x, center.y, z, radius);
            final BlockPos sanitizedPos = particleGround != null ? particleGround : pos;
            final double particleX = particleGround != null ? x : center.x;
            final double particleZ = particleGround != null ? z : center.z;

            final BlockState state = level.getBlockState(sanitizedPos);
            final double y = sanitizedPos.getY() + state.getCollisionShape(level, sanitizedPos).max(Direction.Axis.Y) + 0.05;
            level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, state, sanitizedPos), particleX, y, particleZ, particles, 0.12, 0.04, 0.12, 0.16);
        }

    }

    private static @Nullable BlockPos findParticleGround(final ServerLevel level, final double x, final double y, final double z, final double radius) {
        final int blockX = (int) Math.floor(x);
        final int blockZ = (int) Math.floor(z);

        // Searches a few blocks down so particles follow nearby slopes instead of floating
        for (int blockY = (int) Math.floor(y); blockY >= Math.floor(y - radius); blockY--) {
            final BlockPos blockPos = new BlockPos(blockX, blockY, blockZ);
            if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
                return blockPos;
            }

        }

        return null;
    }

    private static boolean isActiveFallActive(final ServerPlayer player) {
        // Checked on player tick, whether it is executing the spear special ability or not
        return player.getPersistentData().getBooleanOr(TAG_PLAYER_SPECIAL_FALL, false);
    }

    private static void acceleratePlayer(final ServerPlayer player) {
        // Moves the player downwards
        final Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.min(movement.y, -2), movement.z);
        player.syncVelocity = true;

        final double angle = Math.PI * 2 * (player.tickCount % 10) / 10;
        final double x = player.getX() + Math.cos(angle);
        final double z = player.getZ() + Math.sin(angle);
        player.level().sendParticles(OlympusParticles.ARES_SPEAR_TRACE.get(), x, player.getY(), z, 1, 0, 0, 0, 0);
        player.level().sendParticles(OlympusParticles.ARES_SPEAR_TRACE.get(), x, player.getY(), z, 1, 0.1, 0.1, 0.1, 0);
    }

    private static ItemStack findSpearUsedForKill(final ServerPlayer player, final DamageSource damageSource) {
        // If the damage is caused by the thrown entity
        if (damageSource.getDirectEntity() instanceof SpearOfAresEntity thrownSpear) {
            final ItemStack projectileStack = thrownSpear.getWeaponItem();
            // If the spear is in the hand
            for (final InteractionHand hand : InteractionHand.values()) {
                final ItemStack stack = player.getItemInHand(hand);
                if (stack.is(OlympusItems.SPEAR_OF_ARES.get()) && ItemStack.isSameItemSameComponents(stack, projectileStack)) {
                    return stack;
                }

            }

            // If the player changed to another slot before the entity died
            final int slotIdx = player.getInventory().findSlotMatchingItem(projectileStack);
            if (slotIdx >= 0) {
                final ItemStack stack = player.getInventory().getItem(slotIdx);
                if (stack.is(OlympusItems.SPEAR_OF_ARES.get())) {
                    return stack;
                }

            }

            return ItemStack.EMPTY;
        }

        // If it's a direct hit (killed with the spear item itself)
        final ItemStack mainHandStack = player.getMainHandItem();
        return damageSource.getDirectEntity() == player && mainHandStack.is(OlympusItems.SPEAR_OF_ARES.get()) ? mainHandStack : ItemStack.EMPTY;
    }

    private static ItemStack findUsableHeldSpear(final ServerPlayer player) {
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (stack.is(OlympusItems.SPEAR_OF_ARES.get()) && isSpecialAbilityCharged(stack) && isSpecialAbilityReady(stack, player.level())) {
                return stack;
            }

        }

        return ItemStack.EMPTY;
    }

    /// Same code over again {@link PoseidonTridentItem}
    @Override
    public boolean releaseUsing(final @NonNull ItemStack stack, final @NonNull Level level, final @NonNull LivingEntity user, final int remainingUseDuration) {
        if (!(user instanceof Player player)) {
            return false;
        }

        final int useTicks = getUseDuration(stack, user) - remainingUseDuration;
        if (useTicks < THROW_THRESHOLD_TIME || stack.nextDamageWillBreak() || player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        // Reduces the item durability and doesn't delete the stack
        stack.hurtWithoutBreaking(1, player);
        final ItemStack aresSpear = stack.copyWithCount(1);
        final SpearOfAresEntity spear = Projectile.spawnProjectileFromRotation(
                SpearOfAresEntity::new, serverLevel, aresSpear, player, 0.0F, PROJECTILE_SHOOT_POWER, 1.0F
        );
        spear.pickup = AbstractArrow.Pickup.DISALLOWED;

        final int cooldownTicks = OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.aresSpearThrowCooldownSeconds.get());
        if (cooldownTicks > 0) {
            player.getCooldowns().addCooldown(stack, cooldownTicks);
        }

        level.playSound(null, spear, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return true;
    }

    @Override
    public @NonNull InteractionResult use(final @NonNull Level level, final @NonNull Player player, final @NonNull InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (stack.nextDamageWillBreak() || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        player.startUsingItem(hand);

        return InteractionResult.CONSUME;
    }

    @Override
    public @NonNull Projectile asProjectile(final @NonNull Level level, final Position position, final ItemStack stack, final @NonNull Direction direction) {
        final SpearOfAresEntity spear = new SpearOfAresEntity(level, position.x(), position.y(), position.z(), stack.copyWithCount(1));
        spear.pickup = AbstractArrow.Pickup.DISALLOWED;
        return spear;
    }

    @Override
    public boolean supportsEnchantment(final ItemStack stack, final Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.LOYALTY) && !enchantment.is(Enchantments.RIPTIDE) && super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        OlympusTooltip.append(tooltip, "spear_of_ares", 0xE06C6C,
                OlympusTooltip.ability(1,
                        OlympusTooltip.property("fall_distance", OlympusTooltip.number(OlympusConfig.INSTANCE.aresSpearAbilityMinimumFallDistance.get())),
                        OlympusTooltip.property("cooldown", OlympusTooltip.seconds(OlympusConfig.INSTANCE.aresSpearAbilityCooldownSeconds.get()))
                ),
                OlympusTooltip.ability(2,
                        OlympusTooltip.property("cooldown", OlympusTooltip.seconds(OlympusConfig.INSTANCE.aresSpearThrowCooldownSeconds.get()))
                ));

    }

    @Override
    public void createGeoRenderer(final Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SpearOfAresItemRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new SpearOfAresItemRenderer();
                }

                return renderer;
            }

        });

    }

    @Override
    public void registerControllers(final AnimatableManager.@NonNull ControllerRegistrar controllers) {
        ;;
    }

    @Override
    public @NonNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
