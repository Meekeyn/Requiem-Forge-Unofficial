package ua.meekeyn.requiemunofficial.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.List;
import java.util.UUID;

@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class GroundMovementGameTests {
    private GroundMovementGameTests() { }

    @GameTest(template = "ground_movement", batch = "ground_measurement", timeoutTicks = 100)
    public static void groundTrajectoriesMatchNativeMoveControl(GameTestHelper helper) {
        for (int z = 0; z < 256; z++) {
            for (int x = 0; x < 9; x++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        var types = new java.util.ArrayList<EntityType<?>>(List.of(EntityType.SPIDER, EntityType.ENDERMAN,
                EntityType.DONKEY, EntityType.COW, EntityType.CAT, EntityType.PIG, EntityType.SHEEP,
                EntityType.IRON_GOLEM, EntityType.CREEPER, EntityType.ENDERMITE, EntityType.HORSE,
                EntityType.VILLAGER));
        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(
                net.minecraft.resources.ResourceLocation.tryParse("alexsmobs:grizzly_bear")).ifPresent(types::add);
        for (EntityType<?> type : types) {
            for (String scenario : List.of("walk", "sprint", "speed", "slowness", "addition", "large_attribute",
                    "cold_start", "diagonal", "dynamic", "resume")) {
                measure(helper, type, scenario);
            }
        }
        helper.succeed();
    }

    private static void measure(GameTestHelper helper, EntityType<?> type, String scenario) {
        Mob reference = spawn(helper, type, 2);
        Mob host = spawn(helper, type, 6);
        // Random horse attributes must not turn a physics comparison into a genetics comparison.
        host.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(reference.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        for (Mob mob : List.of(reference, host)) {
            if (scenario.equals("speed")) mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            if (scenario.equals("slowness")) mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
            if (scenario.equals("addition")) mob.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
                    new AttributeModifier(UUID.fromString("b087540c-a7fb-4cae-a6ee-e1aa54c488cb"), "test", 0.07, AttributeModifier.Operation.ADDITION));
            if (scenario.equals("large_attribute")) mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(1.2);
        }
        ProbePlayer player = new ProbePlayer(helper);
        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(host);
        helper.assertTrue(PossessionManager.tryPossess(player, host), "Possession: " + type);
        boolean sprint = scenario.equals("sprint");
        reference.setSprinting(sprint);
        player.setSprinting(sprint);
        PossessionManager.syncBeta16HostToPlayer(player, host);
        PossessionManager.syncBeta16HostToPlayer(player, host);
        player.setSpeed((float) player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        player.setOnGround(true);
        player.setDeltaMovement(0, -0.0784, 0);
        if (scenario.equals("cold_start")) {
            reference.setDeltaMovement(Vec3.ZERO);
            player.setDeltaMovement(Vec3.ZERO);
            player.setSpeed(0.1F);
        }
        double maxError = 0;
        double nativeCruise = 0;
        double bodyCruise = 0;
        double nativeStop = 0;
        double bodyStop = 0;
        try {
            for (int tick = 0; tick < 80; tick++) {
                boolean moving = tick < 60 && !(scenario.equals("resume") && tick >= 20 && tick < 30);
                if (scenario.equals("dynamic") && (tick == 20 || tick == 40)) {
                    sprint = tick == 20;
                    player.setSprinting(sprint);
                    reference.setSprinting(sprint);
                    for (Mob mob : List.of(reference, host)) {
                        if (sprint) mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
                        else mob.removeEffect(MobEffects.MOVEMENT_SPEED);
                    }
                    PossessionManager.syncBeta16HostToPlayer(player, host);
                }
                reference.getMoveControl().setWantedPosition(reference.getX(), reference.getY(),
                        reference.getZ() + (moving ? 64 : 0), type == EntityType.VILLAGER
                                ? net.minecraft.world.entity.npc.Villager.SPEED_MODIFIER
                                : type == EntityType.CAT && sprint ? 1.33 : 1.0);
                double beforeReference = reference.getZ();
                double beforePlayer = player.getZ();
                reference.aiStep();
                player.zza = moving ? 1 : 0;
                player.xxa = moving && scenario.equals("diagonal") ? 1 : 0;
                player.setYRot(scenario.equals("diagonal") ? 45 : 0);
                player.aiStep();
                PossessionManager.syncBeta16HostToPlayer(player, host);
                Vec3 hostPosition = host.position();
                host.aiStep();
                helper.assertTrue(host.position().equals(hostPosition), "Proxy moved twice: " + type);
                double nativeStep = reference.getZ() - beforeReference;
                double bodyStep = player.getZ() - beforePlayer;
                maxError = Math.max(maxError, Math.abs(nativeStep - bodyStep));
                RequiemUnofficial.LOGGER.info("GROUND_TICK,{},{},{},{},{}", type, scenario, tick, nativeStep, bodyStep);
                helper.assertTrue(!reference.horizontalCollision && !player.horizontalCollision,
                        "Measurement collided: " + type + " " + scenario + " tick=" + tick);
                helper.assertTrue(Math.abs(player.getX() - helper.absolutePos(new BlockPos(6, 2, 3)).getX() - 0.5) < 1.0E-5,
                        "Body must stay in its lane: " + type + " " + scenario);
                if (tick >= 40 && tick < 60) { nativeCruise += nativeStep; bodyCruise += bodyStep; }
                if (!moving) { nativeStop += nativeStep; bodyStop += bodyStep; }
            }
            RequiemUnofficial.LOGGER.info("GROUND_RESULT,{},{},{},{},{},{},{}", type, scenario,
                    nativeCruise, bodyCruise, nativeStop, bodyStop, maxError);
            helper.assertTrue(maxError <= 2.0E-6 + 2.0E-6 * Math.abs(nativeCruise / 20),
                    "Per-tick trajectory mismatch: " + type + " " + scenario + " max=" + maxError
                            + " cruise=" + nativeCruise + "/" + bodyCruise + " stop=" + nativeStop + "/" + bodyStop);
            helper.assertTrue(player.getDeltaMovement().horizontalDistance() < 0.003,
                    "Body must stop after input release: " + type);
        } finally {
            PossessionManager.release(player, host);
            reference.discard();
            host.discard();
        }
    }

    private static Mob spawn(GameTestHelper helper, EntityType<?> type, int lane) {
        Mob mob = (Mob) type.create(helper.getLevel());
        mob.goalSelector.removeAllGoals(goal -> true);
        mob.targetSelector.removeAllGoals(goal -> true);
        mob.getBrain().removeAllBehaviors();
        BlockPos start = helper.absolutePos(new BlockPos(lane, 2, 3));
        mob.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(mob);
        mob.setOnGround(true);
        mob.setDeltaMovement(0, -0.0784, 0);
        return mob;
    }

    private static final class ProbePlayer extends ServerPlayer {
        ProbePlayer(GameTestHelper helper) {
            super(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "ground-probe"));
        }
        @Override public int awardRecipes(java.util.Collection<net.minecraft.world.item.crafting.Recipe<?>> recipes) { return 0; }
    }
}
