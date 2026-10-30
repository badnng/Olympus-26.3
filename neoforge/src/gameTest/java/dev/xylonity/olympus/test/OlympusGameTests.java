package dev.xylonity.olympus.test;

import com.mojang.authlib.GameProfile;
import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.common.block.AirCloudBlock;
import dev.xylonity.olympus.common.block.ClimbingRoseBlock;
import dev.xylonity.olympus.common.block.LockedChestBlock;
import dev.xylonity.olympus.common.block.ParthenonSpawnerBlock;
import dev.xylonity.olympus.common.block.PentelicMarbleColumnBlock;
import dev.xylonity.olympus.common.block.PoppyOfDemeterBlock;
import dev.xylonity.olympus.common.block.entity.ParthenonSpawnerBlockEntity;
import dev.xylonity.olympus.common.block.entity.PoppyOfDemeterBlockEntity;
import dev.xylonity.olympus.common.effect.InvisibilityOfHadesEffect;
import dev.xylonity.olympus.common.entity.HarpyEntity;
import dev.xylonity.olympus.common.entity.ai.harpy.internal.*;
import dev.xylonity.olympus.common.entity.projectile.AbsorbedSoulEntity;
import dev.xylonity.olympus.common.entity.projectile.HarpyProjectileEntity;
import dev.xylonity.olympus.common.entity.projectile.PoseidonTridentEntity;
import dev.xylonity.olympus.common.entity.projectile.SpearOfAresEntity;
import dev.xylonity.olympus.common.entity.projectile.SummoningSpearsEntity;
import dev.xylonity.olympus.common.item.*;
import dev.xylonity.olympus.common.util.ArtemisArrow;
import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Regression expectations taken from Xylonity's original Olympus 26.1.2 implementation. */
@Mod(OlympusGameTests.MOD_ID)
@EventBusSubscriber(modid = OlympusGameTests.MOD_ID)
public final class OlympusGameTests {
    public static final String MOD_ID = "olympus_tests";
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MOD_ID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        add("registered_content", OlympusGameTests::registeredContent);
        add("harpy_attributes", h -> harpyAttributes(h, false));
        add("elite_harpy_attributes", h -> harpyAttributes(h, true));
        add("harpy_state_and_fall_immunity", OlympusGameTests::harpyState);
        add("curios_slot_rules", OlympusGameTests::slotRules);
        add("cup_charge_clamp_and_model", OlympusGameTests::cupCharges);
        add("cup_charge_saturation", OlympusGameTests::cupSaturation);
        add("cup_requires_full_charge", OlympusGameTests::cupRequiresFullCharge);
        add("cup_death_protection", OlympusGameTests::cupProtection);
        add("helmet_death_protection_and_cooldown", OlympusGameTests::helmetProtection);
        add("helmet_priority_over_cup", OlympusGameTests::helmetPriority);
        add("hades_prevents_targeting", OlympusGameTests::hadesTargeting);
        add("hades_attack_shortens_duration", OlympusGameTests::hadesDuration);
        add("stun_blocks_horizontal_travel", OlympusGameTests::stunTravel);
        add("hermes_three_jumps_and_recharge", OlympusGameTests::hermesJumps);
        add("hermes_rejects_ground_and_flying", OlympusGameTests::hermesInvalid);
        add("hephaestus_repairs_most_damaged_tool", OlympusGameTests::repairTool);
        add("hephaestus_kill_reduces_cooldown", OlympusGameTests::repairCooldown);
        add("lyre_breeds_adults_only", OlympusGameTests::lyreBreeding);
        add("zeus_damage_stun_chain_and_cooldown", OlympusGameTests::zeusLightning);
        add("ares_charge_and_ability_cooldown", OlympusGameTests::aresCharge);
        add("ares_landing_summons_spears", OlympusGameTests::aresLanding);
        add("ares_uncharged_landing_rejected", OlympusGameTests::aresUncharged);
        add("summoned_spears_damage_once", OlympusGameTests::summonedDamage);
        add("poppy_accelerates_crops", OlympusGameTests::poppyGrowth);
        add("climbing_rose_bonemeal", OlympusGameTests::roseGrowth);
        add("air_cloud_collision_and_fall", OlympusGameTests::cloudCollision);
        add("chest_requires_key_and_preserves_facing", OlympusGameTests::unlockChest);
        add("parthenon_two_waves_and_rewards", OlympusGameTests::spawnerWaves);
        add("artemis_arrow_attachment", OlympusGameTests::artemisAttachment);
        add("projectiles_spawn_and_tick", OlympusGameTests::projectileLifecycle);
        add("weapon_attributes_and_attack_animation", OlympusGameTests::weaponAttributes);
        add("worldgen_and_loot_resources", OlympusGameTests::dataResources);
        add("elite_harpy_projectile_bypasses_damage_cooldown", h -> harpyProjectileDamage(h, true));
        add("normal_harpy_projectile_respects_damage_cooldown", h -> harpyProjectileDamage(h, false));
        add("trident_direct_and_splash_damage", OlympusGameTests::tridentDamage);
        add("harpy_ai_single_projectile", h -> harpyShooting(h, false));
        add("elite_harpy_ai_triple_projectile", h -> harpyShooting(h, true));
        add("harpy_ai_melee_damage", h -> harpyMelee(h, false));
        add("elite_harpy_ai_melee_damage", h -> harpyMelee(h, true));
        add("harpy_ai_dash_phases", OlympusGameTests::harpyDash);
        add("harpy_ai_flight_and_retreat", OlympusGameTests::harpyFlight);
        add("harpy_ai_dodge_guards", OlympusGameTests::harpyDodgeGuards);
        add("cup_charge_persists_through_item_codec", OlympusGameTests::cupPersistence);
        add("cup_soul_spawns_once_and_collects", OlympusGameTests::soulCollection);
        add("artemis_bow_speed_and_arrow_marker", OlympusGameTests::artemisShot);
        add("artemis_arrow_heals_owned_pet", OlympusGameTests::artemisHealing);
        add("ares_throw_preserves_item_and_sets_cooldown", h -> weaponThrow(h, true));
        add("poseidon_throw_consumes_held_item", h -> weaponThrow(h, false));
        add("all_network_payload_codecs", OlympusGameTests::networkCodecs);
        add("parthenon_template_contains_gameplay_blocks", OlympusGameTests::structureContents);
    }

    public OlympusGameTests(IEventBus bus) {
        FUNCTIONS.register(bus);
        bus.addListener(OlympusGameTests::registerTests);
    }

    @SubscribeEvent
    public static void prepareDisposableWorld(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (event.getServer() instanceof GameTestServer && event.getServer().getWorldData() instanceof net.minecraft.world.level.storage.PrimaryLevelData data) {
            // The client opens a copy of this generated fixture, never a user's existing world.
            data.withConfirmedWarning(true);
        }
    }

    private static void add(String name, Consumer<GameTestHelper> test) {
        TESTS.put(name, test);
        FUNCTIONS.register(name, () -> test);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("normal"), new TestEnvironmentDefinition.SetDifficulty(Difficulty.NORMAL));
        TESTS.forEach((name, test) -> event.registerTest(id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(name)), new TestData<>(environment, id("platform"), 400, 0, true))));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(Component.literal(message));
        }
    }

    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        check(helper, Math.abs(actual - expected) < 0.0001D, message + ": expected " + expected + ", got " + actual);
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "OlympusTest"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPermanentlyInvulnerable(false);
        player.getAbilities().invulnerable = false;
        player.setPos(helper.absoluteVec(new Vec3(8, 2, 8)));
        helper.getLevel().addNewPlayer(player);
        player.connection.markClientLoaded();
        player.setInvulnerableTime(0);
        CuriosApi.getCuriosInventory(player).orElseThrow().reset();
        return player;
    }

    private static ItemStack equip(GameTestHelper helper, ServerPlayer player, String slot, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        var handler = CuriosApi.getCuriosInventory(player).orElseThrow();
        check(helper, handler.getStacksHandler(slot).isPresent(), "Missing Curios slot " + slot);
        handler.setEquippedCurio(slot, 0, stack);
        check(helper, handler.isEquipped(item), "Curios did not equip " + item);
        return stack;
    }

    private static Mob target(GameTestHelper helper, double x, double z) {
        Mob mob = EntityTypes.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        mob.setPos(helper.absoluteVec(new Vec3(x, 2, z)));
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.setHealth(100);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static void registeredContent(GameTestHelper h) {
        near(h, OlympusItems.ITEMS.getEntries().size(), 33, "Item count");
        near(h, OlympusEntities.ENTITIES.getEntries().size(), 7, "Entity count");
        OlympusItems.ITEMS.getEntries().forEach(entry -> check(h, !entry.get().getDefaultInstance().isEmpty(), "Empty item " + entry.getId()));
        OlympusBlocks.BLOCKS.getEntries().forEach(entry -> check(h, entry.get().defaultBlockState().getBlock() == entry.get(), "Block state " + entry.getId()));
        h.succeed();
    }

    private static void harpyAttributes(GameTestHelper h, boolean elite) {
        HarpyEntity harpy = (elite ? OlympusEntities.ELITE_HARPY : OlympusEntities.HARPY).get().create(h.getLevel(), EntitySpawnReason.COMMAND);
        check(h, harpy.isElite() == elite, "Elite flag");
        near(h, harpy.getMaxHealth(), elite ? 70 : 35, "Harpy health");
        near(h, harpy.getAttributeValue(Attributes.ATTACK_DAMAGE), elite ? 8 : 5, "Harpy damage");
        near(h, harpy.getAttributeValue(Attributes.ARMOR), elite ? 8 : 6, "Harpy armor");
        check(h, harpy.isNoGravity(), "Harpy must fly");
        h.succeed();
    }

    private static void harpyState(GameTestHelper h) {
        HarpyEntity harpy = h.spawn(OlympusEntities.HARPY.get(), new BlockPos(8, 3, 8));
        for (int state = 0; state <= 6; state++) {
            harpy.setAttackState(state);
            near(h, harpy.getAttackState(), state, "Harpy attack state");
        }
        check(h, !harpy.causeFallDamage(100, 1, h.getLevel().damageSources().fall()), "Harpy fall damage");
        harpy.setAttackState(HarpyEntity.STATE_IDLE);
        harpy.delaySpecialAttacks();
        check(h, !harpy.canStartSpecialAttack(), "Harpy special attack delay");
        h.succeed();
    }

    private static void slotRules(GameTestHelper h) {
        var player = player(h);
        Map<String, net.minecraft.world.item.Item> items = Map.of("head", OlympusItems.HELMET_OF_HADES.get(), "feet", OlympusItems.HERMES_SANDALS.get(), "bracelet", OlympusItems.BRACERS_OF_ZEUS.get(), "belt", OlympusItems.INSTRUMENTS_OF_HEPHAESTUS.get(), "jewelry", OlympusItems.PERSEPHONE_CUP.get());
        items.forEach((slot, item) -> {
            var curio = (ICurioItem) item;
            var context = new SlotContext(slot, player, 0, false, true);
            check(h, curio.canEquip(context, new ItemStack(item)), "Valid slot rejected: " + slot);
            check(h, curio.canEquipFromUse(context, new ItemStack(item)), "Use equip rejected: " + slot);
            check(h, !curio.canEquip(new SlotContext("invalid", player, 0, false, true), new ItemStack(item)), "Invalid slot accepted");
            equip(h, player, slot, item);
        });
        h.succeed();
    }

    private static void cupCharges(GameTestHelper h) {
        ItemStack cup = new ItemStack(OlympusItems.PERSEPHONE_CUP.get());
        PersephoneCupItem.setSoulCharges(cup, 999);
        near(h, PersephoneCupItem.getSoulCharges(cup), 40, "Cup upper clamp");
        near(h, cup.get(DataComponents.CUSTOM_MODEL_DATA).floats().getFirst(), 40, "Cup fill model");
        PersephoneCupItem.setSoulCharges(cup, -1);
        near(h, PersephoneCupItem.getSoulCharges(cup), 0, "Cup lower clamp");
        check(h, !cup.has(DataComponents.CUSTOM_MODEL_DATA), "Empty cup model data");
        h.succeed();
    }

    private static void cupSaturation(GameTestHelper h) {
        ItemStack cup = new ItemStack(OlympusItems.PERSEPHONE_CUP.get());
        for (int i = 0; i < 50; i++) PersephoneCupItem.addSoulCharge(cup);
        near(h, PersephoneCupItem.getSoulCharges(cup), 40, "Soul saturation");
        h.succeed();
    }

    private static void cupRequiresFullCharge(GameTestHelper h) {
        var player = player(h);
        ItemStack cup = equip(h, player, "jewelry", OlympusItems.PERSEPHONE_CUP.get());
        PersephoneCupItem.setSoulCharges(cup, 39);
        check(h, !PersephoneCupItem.tryActivateAbility(player), "Partially filled cup prevented death");
        near(h, PersephoneCupItem.getSoulCharges(cup), 39, "Failed ability consumed souls");
        h.succeed();
    }

    private static void cupProtection(GameTestHelper h) {
        var player = player(h);
        ItemStack cup = equip(h, player, "jewelry", OlympusItems.PERSEPHONE_CUP.get());
        PersephoneCupItem.setSoulCharges(cup, 40);
        player.setHealth(1);
        player.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 100);
        check(h, player.isAlive(), "Cup did not intercept lethal damage");
        near(h, player.getHealth(), 6, "Cup restored health");
        near(h, PersephoneCupItem.getSoulCharges(cup), 20, "Cup soul cost");
        check(h, player.hasEffect(MobEffects.REGENERATION), "Cup regeneration");
        h.succeed();
    }

    private static void helmetProtection(GameTestHelper h) {
        var player = player(h);
        ItemStack helmet = equip(h, player, "head", OlympusItems.HELMET_OF_HADES.get());
        player.setHealth(1);
        player.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 100);
        near(h, player.getHealth(), 10, "Helmet restored health");
        check(h, player.hasEffect(OlympusMobEffects.INVISIBILITY_OF_HADES), "Helmet invisibility");
        check(h, player.getCooldowns().isOnCooldown(helmet), "Helmet cooldown");
        check(h, !HelmetOfHadesItem.tryActivateAbility(player), "Helmet bypassed cooldown");
        h.succeed();
    }

    private static void helmetPriority(GameTestHelper h) {
        var player = player(h);
        equip(h, player, "head", OlympusItems.HELMET_OF_HADES.get());
        ItemStack cup = equip(h, player, "jewelry", OlympusItems.PERSEPHONE_CUP.get());
        PersephoneCupItem.setSoulCharges(cup, 40);
        player.setHealth(1);
        player.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 100);
        near(h, PersephoneCupItem.getSoulCharges(cup), 40, "Helmet must take priority over cup");
        near(h, player.getHealth(), 10, "Helmet priority healing");
        h.succeed();
    }

    private static void hadesTargeting(GameTestHelper h) {
        var player = player(h);
        player.addEffect(new MobEffectInstance(OlympusMobEffects.INVISIBILITY_OF_HADES, 600));
        Mob mob = target(h, 6, 8);
        mob.setTarget(player);
        check(h, mob.getTarget() == null, "Hidden player targeted");
        check(h, !player.canBeSeenAsEnemy(), "Hades mixin did not hide player");
        h.succeed();
    }

    private static void hadesDuration(GameTestHelper h) {
        var player = player(h);
        player.addEffect(new MobEffectInstance(OlympusMobEffects.INVISIBILITY_OF_HADES, 600));
        InvisibilityOfHadesEffect.shortenAfterAttack(player);
        near(h, player.getEffect(OlympusMobEffects.INVISIBILITY_OF_HADES).getDuration(), 300, "Attack halves invisibility");
        h.succeed();
    }

    private static void stunTravel(GameTestHelper h) {
        Mob mob = target(h, 8, 8);
        mob.addEffect(new MobEffectInstance(OlympusMobEffects.LIGHTNING_STUN, 100));
        mob.setDeltaMovement(0.5, 0, 0.5);
        Vec3 before = mob.position();
        mob.travel(new Vec3(1, 0, 1));
        near(h, mob.getX(), before.x, "Stun X movement");
        near(h, mob.getZ(), before.z, "Stun Z movement");
        near(h, mob.getAttributeValue(Attributes.ATTACK_DAMAGE), 0, "Stun damage suppression");
        h.succeed();
    }

    private static void hermesJumps(GameTestHelper h) {
        var player = player(h);
        equip(h, player, "feet", OlympusItems.HERMES_SANDALS.get());
        player.setOnGround(false);
        for (int i = 0; i < 3; i++) {
            player.setDeltaMovement(Vec3.ZERO);
            HermesSandalsItem.tryActiveAbility(player);
            check(h, player.getDeltaMovement().y > 0, "Extra jump " + i);
        }
        player.setDeltaMovement(Vec3.ZERO);
        HermesSandalsItem.tryActiveAbility(player);
        near(h, player.getDeltaMovement().y, 0, "Fourth jump accepted");
        player.setOnGround(true);
        HermesSandalsItem.rechargeExtraJumps(player);
        player.setOnGround(false);
        HermesSandalsItem.tryActiveAbility(player);
        check(h, player.getDeltaMovement().y > 0, "Jump recharge");
        h.succeed();
    }

    private static void hermesInvalid(GameTestHelper h) {
        check(h, !HermesSandalsItem.canExtraJump(player(h)), "Jump without sandals");
        var player = player(h);
        equip(h, player, "feet", OlympusItems.HERMES_SANDALS.get());
        player.setOnGround(true);
        check(h, !HermesSandalsItem.canExtraJump(player), "Ground jump");
        player.setOnGround(false);
        player.getAbilities().flying = true;
        check(h, !HermesSandalsItem.canExtraJump(player), "Creative flight jump");
        h.succeed();
    }

    private static void repairTool(GameTestHelper h) {
        var player = player(h);
        ItemStack instruments = equip(h, player, "belt", OlympusItems.INSTRUMENTS_OF_HEPHAESTUS.get());
        ItemStack first = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack second = new ItemStack(Items.IRON_PICKAXE);
        first.setDamageValue(100);
        second.setDamageValue(50);
        player.getInventory().setItem(0, first);
        player.getInventory().setItem(1, second);
        ((InstrumentsOfHephaestusItem) instruments.getItem()).curioTick(new SlotContext("belt", player, 0, false, true), instruments);
        near(h, first.getDamageValue(), 70, "Repair amount");
        near(h, second.getDamageValue(), 50, "Only most damaged tool repaired");
        check(h, player.getCooldowns().isOnCooldown(instruments), "Repair cooldown");
        h.succeed();
    }

    private static void repairCooldown(GameTestHelper h) {
        var player = player(h);
        ItemStack instruments = equip(h, player, "belt", OlympusItems.INSTRUMENTS_OF_HEPHAESTUS.get());
        long end = h.getLevel().getGameTime() + 1000;
        CustomData.update(DataComponents.CUSTOM_DATA, instruments, tag -> tag.putLong("olympus_hephaestus_cooldown_end", end));
        InstrumentsOfHephaestusItem.reduceCooldownOnKill(player);
        near(h, instruments.get(DataComponents.CUSTOM_DATA).copyTag().getLongOr("olympus_hephaestus_cooldown_end", 0), end - 100, "Kill reduces repair cooldown by five seconds");
        h.succeed();
    }

    private static void lyreBreeding(GameTestHelper h) {
        var player = player(h);
        var adult = h.spawn(EntityTypes.COW, new BlockPos(9, 2, 8));
        var baby = h.spawn(EntityTypes.COW, new BlockPos(10, 2, 8));
        baby.setBaby(true);
        ItemStack lyre = new ItemStack(OlympusItems.APHRODITE_LYRE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, lyre);
        lyre.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        lyre.getItem().onUseTick(h.getLevel(), player, lyre, AphroditeLyreItem.DURATION_TICKS);
        check(h, adult.isInLove(), "Adult did not enter love mode");
        check(h, !baby.isInLove(), "Baby entered love mode");
        lyre.getItem().releaseUsing(lyre, h.getLevel(), player, 100);
        check(h, player.getCooldowns().isOnCooldown(lyre), "Lyre cooldown");
        h.succeed();
    }

    private static void zeusLightning(GameTestHelper h) {
        var player = player(h);
        ItemStack bracers = equip(h, player, "bracelet", OlympusItems.BRACERS_OF_ZEUS.get());
        Mob first = target(h, 7, 8);
        Mob second = target(h, 9, 8);
        BracersOfZeusItem.tryActivateAbility(first, player, h.getLevel().damageSources().playerAttack(player), 1);
        near(h, first.getHealth(), 92, "Lightning damage");
        near(h, second.getHealth(), 96, "Chain lightning damage");
        check(h, first.hasEffect(OlympusMobEffects.LIGHTNING_STUN), "Lightning stun");
        check(h, player.getCooldowns().isOnCooldown(bracers), "Lightning cooldown");
        BracersOfZeusItem.tryActivateAbility(first, player, h.getLevel().damageSources().playerAttack(player), 1);
        near(h, first.getHealth(), 92, "Cooldown allowed another strike");
        h.succeed();
    }

    private static void aresCharge(GameTestHelper h) {
        var player = player(h);
        ItemStack spear = new ItemStack(OlympusItems.SPEAR_OF_ARES.get());
        check(h, !SpearOfAresItem.isSpecialAbilityCharged(spear), "New spear charged");
        SpearOfAresItem.setSpecialAbilityCharged(spear, player, true);
        check(h, SpearOfAresItem.isSpecialAbilityCharged(spear), "Spear did not charge");
        SpearOfAresItem.startSpecialAbilityCooldown(spear, h.getLevel());
        check(h, !SpearOfAresItem.isSpecialAbilityReady(spear, h.getLevel()), "Spear ability cooldown");
        h.succeed();
    }

    private static void aresLanding(GameTestHelper h) {
        var player = player(h);
        ItemStack spear = new ItemStack(OlympusItems.SPEAR_OF_ARES.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, spear);
        SpearOfAresItem.setSpecialAbilityCharged(spear, player, true);
        player.setShiftKeyDown(true);
        check(h, SpearOfAresItem.handleSpecialLanding(player, 4), "Charged landing rejected");
        check(h, !SpearOfAresItem.isSpecialAbilityCharged(spear), "Landing did not consume charge");
        check(h, !h.getLevel().getEntitiesOfClass(SummoningSpearsEntity.class, player.getBoundingBox().inflate(2)).isEmpty(), "Landing did not summon spears");
        h.succeed();
    }

    private static void aresUncharged(GameTestHelper h) {
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OlympusItems.SPEAR_OF_ARES.get()));
        player.setShiftKeyDown(true);
        check(h, !SpearOfAresItem.handleSpecialLanding(player, 4), "Uncharged ability accepted");
        h.succeed();
    }

    private static void summonedDamage(GameTestHelper h) {
        var player = player(h);
        Mob mob = target(h, 9, 8);
        h.getLevel().addFreshEntity(new SummoningSpearsEntity(h.getLevel(), player));
        h.runAfterDelay(8, () -> {
            near(h, mob.getHealth(), 85, "Summoned spear damage");
            h.runAfterDelay(25, () -> {
                near(h, mob.getHealth(), 85, "Summoned spears damaged twice");
                h.succeed();
            });
        });
    }

    private static void poppyGrowth(GameTestHelper h) {
        BlockPos flower = new BlockPos(8, 2, 8);
        BlockPos crop = new BlockPos(9, 2, 8);
        h.setBlock(flower.below(), Blocks.DIRT);
        h.setBlock(crop.below(), Blocks.FARMLAND);
        h.setBlock(flower, OlympusBlocks.POPPY_OF_DEMETER.get());
        h.setBlock(crop, Blocks.WHEAT);
        h.runAfterDelay(55, () -> {
            check(h, h.getBlockState(crop).getValue(CropBlock.AGE) > 0, "Poppy did not grow wheat");
            h.succeed();
        });
    }

    private static void roseGrowth(GameTestHelper h) {
        BlockPos pos = new BlockPos(8, 2, 8);
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos.below().east(), Blocks.STONE);
        h.setBlock(pos.below().west(), Blocks.STONE);
        BlockState state = OlympusBlocks.CLIMBING_ROSE.get().defaultBlockState().setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.DOWN), true);
        h.setBlock(pos, state);
        var rose = (ClimbingRoseBlock) state.getBlock();
        check(h, rose.isValidBonemealTarget(h.getLevel(), h.absolutePos(pos), state, BonemealSource.INTERACTION), "Rose not growable");
        rose.performBonemeal(h.getLevel(), h.getLevel().getRandom(), h.absolutePos(pos), state, BonemealSource.INTERACTION);
        check(h, h.getBlockState(pos.east()).is(rose) || h.getBlockState(pos.west()).is(rose), "Rose did not spread");
        h.succeed();
    }

    private static void cloudCollision(GameTestHelper h) {
        var player = player(h);
        BlockPos pos = h.absolutePos(new BlockPos(8, 2, 8));
        BlockState state = OlympusBlocks.AIR_CLOUD_BLOCK.get().defaultBlockState();
        player.fallDistance = 0;
        check(h, state.getCollisionShape(h.getLevel(), pos, CollisionContext.of(player)).isEmpty(), "Cloud blocks normal movement");
        player.fallDistance = 4;
        near(h, state.getCollisionShape(h.getLevel(), pos, CollisionContext.of(player)).max(Direction.Axis.Y), 0.9, "Cloud landing height");
        ((AirCloudBlock) state.getBlock()).fallOn(h.getLevel(), state, pos, player, 4);
        near(h, player.fallDistance, 0, "Cloud fall reset");
        h.succeed();
    }

    private static void unlockChest(GameTestHelper h) {
        var player = player(h);
        BlockPos pos = new BlockPos(8, 2, 8);
        h.setBlock(pos, OlympusBlocks.LOCKED_CHEST.get().defaultBlockState().setValue(LockedChestBlock.FACING, Direction.EAST));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        h.useBlock(pos, player);
        check(h, h.getBlockState(pos).is(OlympusBlocks.LOCKED_CHEST), "Wrong item unlocked chest");
        ItemStack key = new ItemStack(OlympusItems.PARTHENON_KEY.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        h.useBlock(pos, player);
        check(h, h.getBlockState(pos).is(Blocks.CHEST), "Key did not unlock chest");
        check(h, h.getBlockState(pos).getValue(ChestBlock.FACING) == Direction.EAST, "Chest facing changed");
        near(h, key.getCount(), 1, "Chest key consumption");
        h.succeed();
    }

    private static void spawnerWaves(GameTestHelper h) {
        var player = player(h);
        BlockPos pos = new BlockPos(8, 1, 8);
        h.setBlock(pos, OlympusBlocks.PARTHENON_SPAWNER.get());
        var spawner = h.getBlockEntity(pos, ParthenonSpawnerBlockEntity.class);
        ParthenonSpawnerBlockEntity.serverTick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos), spawner);
        var bounds = new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(10);
        var first = h.getLevel().getEntitiesOfClass(HarpyEntity.class, bounds);
        near(h, first.size(), 2, "First wave count");
        check(h, first.stream().noneMatch(HarpyEntity::isElite), "First wave elite");
        first.forEach(Entity::discard);
        ParthenonSpawnerBlockEntity.serverTick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos), spawner);
        h.runAfterDelay(42, () -> {
            var second = h.getLevel().getEntitiesOfClass(HarpyEntity.class, bounds);
            near(h, second.size(), 3, "Second wave count");
            near(h, second.stream().filter(HarpyEntity::isElite).count(), 1, "Second wave elite count");
            second.forEach(Entity::discard);
            h.runAfterDelay(25, () -> {
                check(h, !h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds, item -> item.getItem().is(OlympusItems.PARTHENON_KEY)).isEmpty(), "Spawner did not reward key");
                h.succeed();
            });
        });
    }

    private static void artemisAttachment(GameTestHelper h) {
        var arrow = h.spawn(EntityTypes.ARROW, new BlockPos(8, 3, 8));
        check(h, arrow instanceof ArtemisArrow, "Arrow mixin missing");
        ArtemisArrow artemis = (ArtemisArrow) arrow;
        artemis.olympus$setArtemisArrow(true);
        check(h, artemis.olympus$isArtemisArrow(), "Arrow attachment not stored");
        artemis.olympus$setArtemisArrow(false);
        check(h, !artemis.olympus$isArtemisArrow(), "Arrow attachment not removed");
        h.succeed();
    }

    private static void projectileLifecycle(GameTestHelper h) {
        OlympusEntities.ENTITIES.getEntries().forEach(entry -> {
            if (entry.get() != OlympusEntities.HARPY.get() && entry.get() != OlympusEntities.ELITE_HARPY.get()) {
                Entity entity = entry.get().create(h.getLevel(), EntitySpawnReason.COMMAND);
                check(h, entity != null, "Entity factory " + entry.getId());
                if (entity instanceof AbsorbedSoulEntity) {
                    check(h, entity.getMoveSimulationType() == net.minecraft.world.entity.MoveSimulationType.SERVER_AND_CLIENT, "Soul movement must run on both sides like vanilla XP orbs");
                }
                entity.setPos(h.absoluteVec(new Vec3(8, 4, 8)));
                h.getLevel().addFreshEntity(entity);
            }
        });
        h.runAfterDelay(10, h::succeed);
    }

    private static void weaponAttributes(GameTestHelper h) {
        var player = player(h);
        ItemStack trident = new ItemStack(OlympusItems.POSEIDON_TRIDENT.get());
        ItemStack spear = new ItemStack(OlympusItems.SPEAR_OF_ARES.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, spear);
        check(h, spear.get(DataComponents.ATTACK_ANIMATION).type() == net.minecraft.world.item.SwingAnimationType.STAB, "Ares stab animation");
        near(h, spear.get(DataComponents.ATTACK_ANIMATION).duration(), 10, "Ares stab duration");
        near(h, trident.getMaxDamage(), 2000, "Trident durability");
        near(h, spear.getMaxDamage(), 2000, "Spear durability");
        check(h, spear.has(DataComponents.PIERCING_WEAPON), "Ares piercing weapon");
        h.succeed();
    }

    private static void dataResources(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        check(h, access.lookupOrThrow(Registries.STRUCTURE).getValue(Olympus.of("celestial_parthenon")) != null, "Parthenon structure missing");
        check(h, access.lookupOrThrow(Registries.TEMPLATE_POOL).getValue(Olympus.of("celestial_parthenon")) != null, "Parthenon pool missing");
        check(h, access.lookupOrThrow(Registries.FEATURE).getValue(Olympus.of("pentelic_marble")) != null, "Marble feature missing");
        check(h, h.getLevel().getStructureTemplateManager().get(Olympus.of("celestial_parthenon")).isPresent(), "Parthenon template missing");
        check(h, h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Olympus.of("chests/parthenon_chest"))) != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "Parthenon loot missing");
        h.succeed();
    }

    private static void hitEntity(Entity projectile, Entity target) {
        try {
            Class<?> type = projectile.getClass();
            java.lang.reflect.Method method = null;
            while (type != null && method == null) {
                try {
                    method = type.getDeclaredMethod("onHitEntity", net.minecraft.world.phys.EntityHitResult.class);
                } catch (NoSuchMethodException ignored) {
                    type = type.getSuperclass();
                }
            }
            if (method == null) throw new NoSuchMethodException("onHitEntity");
            method.setAccessible(true);
            method.invoke(projectile, new net.minecraft.world.phys.EntityHitResult(target));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not exercise projectile impact", exception);
        }
    }

    private static void harpyProjectileDamage(GameTestHelper h, boolean elite) {
        HarpyEntity owner = h.spawn((elite ? OlympusEntities.ELITE_HARPY : OlympusEntities.HARPY).get(), new BlockPos(8, 3, 3));
        owner.setNoAi(true);
        Mob target = target(h, 8, 8);
        target.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 4);
        check(h, target.damageCooldownTime > 10, "Initial damage did not start cooldown");
        var projectile = new HarpyProjectileEntity(h.getLevel(), owner, target);
        hitEntity(projectile, target);
        near(h, target.getHealth(), elite ? 92 : 96, "Harpy projectile damage during cooldown");
        check(h, projectile.isRemoved(), "Harpy projectile did not disappear on impact");
        h.succeed();
    }

    private static void tridentDamage(GameTestHelper h) {
        var player = player(h);
        Mob direct = target(h, 7, 8);
        Mob splash = target(h, 9, 8);
        Mob distant = target(h, 14, 8);
        var trident = new PoseidonTridentEntity(h.getLevel(), player, new ItemStack(OlympusItems.POSEIDON_TRIDENT.get()));
        trident.setPos(direct.position());
        hitEntity(trident, direct);
        near(h, direct.getHealth(), 90, "Trident direct damage");
        near(h, splash.getHealth(), 90, "Trident splash damage");
        near(h, distant.getHealth(), 100, "Trident splash radius");
        near(h, player.getHealth(), 20, "Trident splash hurt owner");
        h.succeed();
    }

    private static HarpyEntity combatHarpy(GameTestHelper h, boolean elite, Mob target) {
        var harpy = h.spawn((elite ? OlympusEntities.ELITE_HARPY : OlympusEntities.HARPY).get(), new BlockPos(8, 2, 3));
        harpy.setNoAi(true);
        harpy.setTarget(target);
        return harpy;
    }

    private static void harpyShooting(GameTestHelper h, boolean elite) {
        Mob target = target(h, 8, 11);
        var harpy = combatHarpy(h, elite, target);
        var goal = new HarpyProjectileGoal(harpy, 30, 17, 240);
        check(h, goal.canUse(), "Harpy ranged attack cannot start");
        goal.start();
        near(h, harpy.getAttackState(), HarpyEntity.STATE_SHOT, "Ranged animation state");
        for (int i = 0; i < 16; i++) goal.tick();
        var bounds = new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(8, 3, 8))).inflate(12);
        near(h, h.getLevel().getEntitiesOfClass(HarpyProjectileEntity.class, bounds).size(), 0, "Projectile released too early");
        goal.tick();
        near(h, h.getLevel().getEntitiesOfClass(HarpyProjectileEntity.class, bounds).size(), elite ? 3 : 1, "Projectile volley size");
        goal.stop();
        near(h, harpy.getAttackState(), HarpyEntity.STATE_IDLE, "Ranged attack state reset");
        check(h, !harpy.canStartSpecialAttack(), "Post-shot special attack delay");
        h.succeed();
    }

    private static void harpyMelee(GameTestHelper h, boolean elite) {
        Mob target = target(h, 8, 4);
        var harpy = combatHarpy(h, elite, target);
        var goal = new HarpyMeleeGoal(harpy, 20, 20);
        check(h, goal.canUse(), "Harpy melee cannot start");
        goal.start();
        for (int i = 0; i < 20; i++) goal.tick();
        near(h, target.getHealth(), elite ? 92 : 95, "Melee damage must apply exactly once");
        check(h, !goal.canContinueToUse(), "Melee animation exceeded 20 ticks");
        goal.stop();
        check(h, !goal.canUse(), "Melee cooldown absent");
        h.succeed();
    }

    private static void harpyDash(GameTestHelper h) {
        var harpy = combatHarpy(h, false, target(h, 8, 11));
        var goal = new HarpyDashGoal(harpy, 19, 17);
        check(h, goal.canUse(), "Dash cannot start");
        goal.start();
        near(h, harpy.getAttackState(), HarpyEntity.STATE_DASH_PREPARING, "Dash preparation state");
        for (int i = 0; i < 25; i++) goal.tick();
        check(h, harpy.getAttackState() == HarpyEntity.STATE_DASHING || harpy.getAttackState() == HarpyEntity.STATE_DASH_ENDING, "Dash did not leave preparation");
        goal.stop();
        near(h, harpy.getDeltaMovement().length(), 0, "Dash stop movement");
        check(h, !harpy.noPhysics, "Dash stop restored collision");
        h.succeed();
    }

    private static void harpyFlight(GameTestHelper h) {
        var harpy = h.spawn(OlympusEntities.HARPY.get(), new BlockPos(8, 5, 8));
        harpy.setNoAi(true);
        var flight = new HarpyFlightGoal(harpy);
        check(h, flight.canUse(), "Idle harpy cannot fly");
        flight.start();
        for (int i = 0; i < 30; i++) flight.tick();
        harpy.setTarget(target(h, 8, 11));
        check(h, !flight.canContinueToUse(), "Idle flight ignored combat target");
        flight.stop();
        harpy.setAttackState(HarpyEntity.STATE_IDLE);
        var retreat = new HarpyRetreatGoal(harpy);
        check(h, retreat.canUse(), "Combat retreat cannot start");
        retreat.start();
        for (int i = 0; i < 20; i++) retreat.tick();
        retreat.stop();
        check(h, Double.isFinite(harpy.getDeltaMovement().length()), "Retreat produced invalid movement");
        h.succeed();
    }

    private static void harpyDodgeGuards(GameTestHelper h) {
        var harpy = h.spawn(OlympusEntities.HARPY.get(), new BlockPos(8, 4, 8));
        harpy.setNoAi(true);
        var goal = new HarpyProjectileDodgeGoal(harpy);
        check(h, !goal.canUse(), "Dodge started without a projectile");
        harpy.setAttackState(HarpyEntity.STATE_DASHING);
        check(h, !goal.canUse(), "Dodge interrupted dash");
        h.succeed();
    }

    private static void cupPersistence(GameTestHelper h) {
        var cup = new ItemStack(OlympusItems.PERSEPHONE_CUP.get());
        PersephoneCupItem.setSoulCharges(cup, 27);
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, h.getLevel().registryAccess());
        var saved = ItemStack.CODEC.encodeStart(ops, cup).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        near(h, PersephoneCupItem.getSoulCharges(restored), 27, "Saved cup charges");
        check(h, ItemStack.isSameItemSameComponents(cup, restored), "Cup components did not survive serialization");
        h.succeed();
    }

    private static void soulCollection(GameTestHelper h) {
        var player = player(h);
        h.getLevel().getServer().getPlayerList().getPlayersByUUID().put(player.getUUID(), player);
        var cup = equip(h, player, "jewelry", OlympusItems.PERSEPHONE_CUP.get());
        var mob = target(h, 8, 9);
        PersephoneCupItem.spawnSoulOnKill(mob, player);
        PersephoneCupItem.spawnSoulOnKill(mob, player);
        var souls = h.getLevel().getEntitiesOfClass(AbsorbedSoulEntity.class, mob.getBoundingBox().inflate(3));
        near(h, souls.size(), 1, "A kill must spawn exactly one soul");
        var soul = souls.getFirst();
        soul.playerTouch(player);
        near(h, PersephoneCupItem.getSoulCharges(cup), 0, "Soul collected before 20 tick delay");
        h.runAfterDelay(23, () -> {
            try {
                soul.playerTouch(player);
                near(h, PersephoneCupItem.getSoulCharges(cup), 1, "Soul collection did not charge cup");
                check(h, soul.isRemoved(), "Collected soul did not despawn");
                h.succeed();
            } finally {
                h.getLevel().getServer().getPlayerList().getPlayersByUUID().remove(player.getUUID());
            }
        });
    }

    private static void artemisShot(GameTestHelper h) {
        var player = player(h);
        var bow = new ItemStack(OlympusItems.BOW_OF_ARTEMIS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));
        int duration = bow.getItem().getUseDuration(bow, player);
        bow.getItem().releaseUsing(bow, h.getLevel(), player, duration - 20);
        var arrows = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class, player.getBoundingBox().inflate(4));
        near(h, arrows.size(), 1, "Bow projectile count");
        var arrow = arrows.getFirst();
        check(h, ((ArtemisArrow) arrow).olympus$isArtemisArrow(), "Bow did not mark Artemis arrow");
        check(h, Math.abs(arrow.getDeltaMovement().length() - 3.6) < 0.15, "Artemis projectile speed including vanilla random spread");
        h.succeed();
    }

    private static void artemisHealing(GameTestHelper h) {
        var player = player(h);
        var wolf = h.spawn(EntityTypes.WOLF, new BlockPos(8, 2, 9));
        wolf.tame(player);
        wolf.setHealth(10);
        var arrow = h.spawn(EntityTypes.ARROW, new BlockPos(8, 3, 8));
        arrow.setOwner(player);
        arrow.setCritArrow(false);
        arrow.setBaseDamage(2);
        arrow.setDeltaMovement(2, 0, 0);
        ((ArtemisArrow) arrow).olympus$setArtemisArrow(true);
        hitEntity(arrow, wolf);
        near(h, wolf.getHealth(), 12, "Artemis arrow must heal owned pet by half damage");
        check(h, arrow.isRemoved(), "Healing arrow did not disappear");
        h.succeed();
    }

    private static void weaponThrow(GameTestHelper h, boolean ares) {
        var player = player(h);
        var stack = new ItemStack(ares ? OlympusItems.SPEAR_OF_ARES.get() : OlympusItems.POSEIDON_TRIDENT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        int duration = stack.getItem().getUseDuration(stack, player);
        check(h, !stack.getItem().releaseUsing(stack, h.getLevel(), player, duration - 1), "Uncharged throw accepted");
        check(h, stack.getItem().releaseUsing(stack, h.getLevel(), player, duration - 20), "Charged throw rejected");
        var bounds = player.getBoundingBox().inflate(4);
        if (ares) {
            near(h, h.getLevel().getEntitiesOfClass(SpearOfAresEntity.class, bounds).size(), 1, "Ares projectile spawn");
            near(h, stack.getCount(), 1, "Ares throw consumed held spear");
            near(h, stack.getDamageValue(), 1, "Ares throw durability cost");
            check(h, player.getCooldowns().isOnCooldown(stack), "Ares throw cooldown");
        } else {
            near(h, h.getLevel().getEntitiesOfClass(PoseidonTridentEntity.class, bounds).size(), 1, "Trident projectile spawn");
            check(h, stack.isEmpty(), "Thrown trident was duplicated in hand");
        }
        h.succeed();
    }

    private static <T> void roundTrip(GameTestHelper h, net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, T> codec, T value) {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            codec.encode(buffer, value);
            check(h, value.equals(codec.decode(buffer)), "Payload codec round trip: " + value.getClass().getSimpleName());
            check(h, !buffer.isReadable(), "Payload codec left unread bytes");
        } finally {
            buffer.release();
        }
    }

    private static void networkCodecs(GameTestHelper h) {
        roundTrip(h, dev.xylonity.olympus.network.payload.CameraShakePayload.STREAM_CODEC, new dev.xylonity.olympus.network.payload.CameraShakePayload(new Vec3(1.5, -2, 3.25), 36, 1.5F, 35));
        roundTrip(h, dev.xylonity.olympus.network.payload.LightningBoltPayload.STREAM_CODEC, new dev.xylonity.olympus.network.payload.LightningBoltPayload(Vec3.ZERO, new Vec3(2, 10, -3), true));
        roundTrip(h, dev.xylonity.olympus.network.payload.SoulSalvationPayload.STREAM_CODEC, new dev.xylonity.olympus.network.payload.SoulSalvationPayload(17, 16, true));
        roundTrip(h, dev.xylonity.olympus.network.payload.LyreMusicPayload.STREAM_CODEC, new dev.xylonity.olympus.network.payload.LyreMusicPayload(17, true));
        roundTrip(h, dev.xylonity.olympus.network.payload.HermesJumpPayload.STREAM_CODEC, dev.xylonity.olympus.network.payload.HermesJumpPayload.INSTANCE);
        h.succeed();
    }

    private static void structureContents(GameTestHelper h) {
        var template = h.getLevel().getStructureTemplateManager().get(Olympus.of("celestial_parthenon")).orElseThrow();
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        check(h, !template.filterBlocks(BlockPos.ZERO, settings, OlympusBlocks.PARTHENON_SPAWNER.get()).isEmpty(), "Parthenon template lost its spawner during data fixing");
        check(h, !template.filterBlocks(BlockPos.ZERO, settings, OlympusBlocks.LOCKED_CHEST.get()).isEmpty(), "Parthenon template lost locked chests during data fixing");
        h.succeed();
    }
}
