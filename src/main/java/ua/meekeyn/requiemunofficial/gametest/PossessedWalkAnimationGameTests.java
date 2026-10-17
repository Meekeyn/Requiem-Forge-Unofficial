package ua.meekeyn.requiemunofficial.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.mixin.WalkAnimationStateAccessor;

import java.util.UUID;

/**
 * Скелет і зомбі у власному aiStep не рухаються (travel перенаправлено), тому їхній
 * walkAnimation не бачить переміщення тіла. Тіло-господар мусить отримати ту саму ходу,
 * що й фізичний гравець. Ці тести фіксують: після кожного тіка стан ходи хоста збігається
 * зі станом ходи гравця, і ноги рухаються (speed > 0), а не застигають.
 */
@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class PossessedWalkAnimationGameTests {
    private PossessedWalkAnimationGameTests() { }

    @GameTest(template = "ground_movement", batch = "possessed_walk_animation", timeoutTicks = 120)
    public static void possessedSkeletonWalkAnimationMirrorsPlayerBody(GameTestHelper helper) {
        Fixture f = create(helper, EntityType.SKELETON, "walk-anim-skeleton");
        for (int i = 0; i < 20; i++) {
            step(f, 1.0F, 0.0F, false, false);
            assertMirrors(helper, f, "Skeleton");
        }
        helper.assertTrue(f.host.walkAnimation.speed() > 0.05F,
                "Possessed Skeleton legs must swing while the body walks instead of staying frozen");
        close(f);
        helper.succeed();
    }

    @GameTest(template = "ground_movement", batch = "possessed_walk_animation", timeoutTicks = 120)
    public static void possessedZombieWalkAnimationMirrorsPlayerBody(GameTestHelper helper) {
        Fixture f = create(helper, EntityType.ZOMBIE, "walk-anim-zombie");
        for (int i = 0; i < 20; i++) {
            step(f, 1.0F, 0.0F, false, false);
            assertMirrors(helper, f, "Zombie");
        }
        helper.assertTrue(f.host.walkAnimation.speed() > 0.05F,
                "Possessed Zombie legs must swing while the body walks instead of staying frozen");
        close(f);
        helper.succeed();
    }

    @GameTest(template = "ground_movement", batch = "possessed_walk_animation", timeoutTicks = 120)
    public static void possessedSprintWalkAnimationMatchesPlayerBody(GameTestHelper helper) {
        Fixture f = create(helper, EntityType.SKELETON, "walk-anim-sprint");
        for (int i = 0; i < 20; i++) {
            step(f, 1.0F, 0.0F, false, true);
            assertMirrors(helper, f, "Sprinting Skeleton");
        }
        helper.assertTrue(f.host.isSprinting(), "Possessed Skeleton must carry the sprint flag");
        helper.assertTrue(f.host.walkAnimation.speed() > 0.05F,
                "Possessed sprinting Skeleton legs must swing");
        close(f);
        helper.succeed();
    }

    @GameTest(template = "ground_movement", batch = "possessed_walk_animation", timeoutTicks = 120)
    public static void possessedWalkAnimationStopsAndDecaysWithTheBody(GameTestHelper helper) {
        Fixture f = create(helper, EntityType.ZOMBIE, "walk-anim-stop");
        for (int i = 0; i < 20; i++) {
            step(f, 1.0F, 0.0F, false, false);
        }
        helper.assertTrue(f.host.walkAnimation.speed() > 0.05F, "Zombie legs must animate before the stop");
        for (int i = 0; i < 30; i++) {
            step(f, 0.0F, 0.0F, false, false);
            assertMirrors(helper, f, "Stopping Zombie");
        }
        helper.assertTrue(f.host.walkAnimation.speed() < 0.02F,
                "Releasing input must let the possessed Zombie walk animation settle");
        close(f);
        helper.succeed();
    }

    @GameTest(template = "ground_movement", batch = "possessed_walk_animation", timeoutTicks = 160)
    public static void possessedWalkAnimationFollowsJumpAndLanding(GameTestHelper helper) {
        Fixture f = create(helper, EntityType.SKELETON, "walk-anim-jump");
        for (int i = 0; i < 5; i++) {
            step(f, 1.0F, 0.0F, false, false);
        }
        step(f, 1.0F, 0.0F, true, false);
        helper.assertTrue(f.player.getY() > f.origin.y + 0.2D, "Skeleton Player Body must leave the ground");
        for (int i = 0; i < 40; i++) {
            step(f, 1.0F, 0.0F, false, false);
            assertMirrors(helper, f, "Jumping Skeleton");
        }
        helper.assertTrue(f.player.onGround() && Math.abs(f.player.getY() - f.origin.y) < 0.01D,
                "Skeleton Player Body must return to the floor");
        assertMirrors(helper, f, "Landed Skeleton");
        close(f);
        helper.succeed();
    }

    private static Fixture create(GameTestHelper helper, EntityType<? extends Mob> type, String name) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 40; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        Mob host = type.create(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 3));
        host.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(host);
        EntitySoulManager.removeSoul(host);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name));
        player.moveTo(host.getX(), host.getY(), host.getZ(), 0, 0);
        player.setOnGround(true);
        player.setDeltaMovement(Vec3.ZERO);
        SoulManager.setState(player, SoulState.VAGRANT);
        helper.assertTrue(PossessionManager.tryPossess(player, host), "Possession must succeed for " + host.getType());
        Fixture f = new Fixture(player, host, host.position());
        PossessionManager.syncBeta16HostToPlayer(player, host);
        return f;
    }

    private static void step(Fixture f, float forward, float strafe, boolean jump, boolean sprint) {
        // ServerLevel.tickNonPassenger перед кожним тіком скидає "старі" координати (xo/yo/zo).
        // Без цього calculateEntityAnimation бачить накопичене переміщення за всі тіки й хода
        // не затухає, коли гравець зупиняється. На сервері тіло тікається першим, потім гравець,
        // і лише наприкінці хода копіюється з гравця на хоста.
        f.host.setOldPosAndRot();
        f.player.setOldPosAndRot();
        f.host.aiStep();
        f.player.zza = forward;
        f.player.xxa = strafe;
        f.player.setJumping(jump);
        f.player.setSprinting(sprint);
        f.player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(f.player, f.host);
    }

    private static void assertMirrors(GameTestHelper helper, Fixture f, String label) {
        WalkAnimationStateAccessor host = (WalkAnimationStateAccessor) f.host.walkAnimation;
        WalkAnimationStateAccessor player = (WalkAnimationStateAccessor) f.player.walkAnimation;
        helper.assertTrue(Math.abs(host.requiemunofficial$getSpeedOld() - player.requiemunofficial$getSpeedOld()) < 1.0E-6F,
                label + " walk speedOld must mirror the physical player");
        helper.assertTrue(Math.abs(host.requiemunofficial$getWalkSpeed() - player.requiemunofficial$getWalkSpeed()) < 1.0E-6F,
                label + " walk speed must mirror the physical player");
        helper.assertTrue(Math.abs(host.requiemunofficial$getWalkPosition() - player.requiemunofficial$getWalkPosition()) < 1.0E-6F,
                label + " walk position must mirror the physical player");
    }

    private static void close(Fixture f) {
        PossessionManager.release(f.player, f.host);
        f.host.discard();
    }

    private record Fixture(ServerPlayer player, Mob host, Vec3 origin) { }
}
