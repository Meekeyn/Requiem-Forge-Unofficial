package ua.meekeyn.requiemunofficial.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;

@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class ZombiePlayerBodyGameTests {
    @GameTest(template = "empty", batch = "zombie_player_body")
    public static void zombieUsesPlayerCameraDimensionsAndSingleMovement(GameTestHelper helper) {
        Fixture f = create(helper);
        helper.assertTrue(PossessionManager.isUsingBeta16PlayerBody(f.player), "Zombie must select the existing Player Body path");
        helper.assertTrue(f.player.getCamera() == f.player, "Zombie must retain the locally predicted player camera");
        helper.assertTrue(!f.player.noPhysics && !f.player.isNoGravity(), "The player must own collisions and gravity");
        helper.assertTrue(f.player.getDimensions(Pose.STANDING).equals(f.host.getDimensions(Pose.STANDING)),
                "Player collision dimensions must come from the host");
        f.player.setYRot(47.0F);
        f.player.setXRot(-18.0F);
        Vec3 before = f.player.position();
        step(f, 1.0F, 0.0F, false);
        helper.assertTrue(f.player.position().distanceToSqr(before) > 0.0D, "Player physics must move the body");
        helper.assertTrue(f.host.distanceToSqr(f.player) < 1.0E-10D, "Server host must mirror the authoritative player result");
        helper.assertTrue(f.host.getYRot() == 47.0F && f.host.getXRot() == -18.0F, "Host must mirror player yaw and pitch");
        Vec3 mirrored = f.host.position();
        f.host.aiStep();
        helper.assertTrue(f.host.position().equals(mirrored), "Host aiStep must not apply velocity a second time");
        close(f);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "zombie_player_body")
    public static void zombieWasdSprintAndStopUsePlayerPhysics(GameTestHelper helper) {
        Fixture f = create(helper);
        for (float[] axes : new float[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            reset(f);
            Vec3 before = f.player.position();
            step(f, axes[0], axes[1], false);
            Vec3 change = f.player.position().subtract(before);
            helper.assertTrue(axes[0] == 0 ? change.x * axes[1] > 0 : change.z * axes[0] > 0,
                    "W/A/S/D must follow player-local axes");
        }
        reset(f);
        step(f, 1, 0, false);
        double walk = f.player.position().distanceTo(f.origin);
        reset(f);
        f.player.setSprinting(true);
        step(f, 1, 0, false);
        helper.assertTrue(f.player.position().distanceTo(f.origin) > walk, "Vanilla sprint must increase Zombie speed");
        double moving = f.player.getDeltaMovement().horizontalDistance();
        f.player.setSprinting(false);
        step(f, 0, 0, false);
        helper.assertTrue(f.player.getDeltaMovement().horizontalDistance() < moving, "Releasing input must apply vanilla friction");
        for (int i = 0; i < 20; i++) step(f, 0, 0, false);
        helper.assertTrue(f.player.getDeltaMovement().horizontalDistance() < 0.003D, "Neutral input must settle without replay");
        close(f);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "zombie_player_body")
    public static void zombiePlayerBodyJumpsLandsAndHitsWall(GameTestHelper helper) {
        Fixture f = create(helper);
        step(f, 0, 0, true);
        helper.assertTrue(f.player.getY() > f.origin.y + 0.2D, "Zombie jump must be integrated by the player");
        for (int i = 0; i < 40; i++) step(f, 0, 0, false);
        helper.assertTrue(f.player.onGround() && Math.abs(f.player.getY() - f.origin.y) < 0.01D,
                "Player gravity must return Zombie to the floor");
        for (int x = 1; x <= 3; x++) for (int y = 2; y <= 5; y++)
            helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        for (int i = 0; i < 20; i++) step(f, 1, 0, false);
        double wall = helper.absolutePos(new BlockPos(2, 2, 3)).getZ();
        helper.assertTrue(f.player.getBoundingBox().maxZ <= wall + 0.001D && f.player.horizontalCollision,
                "Zombie Player Body must collide with a wall using its hitbox");
        helper.assertTrue(f.host.horizontalCollision && f.host.distanceToSqr(f.player) < 1.0E-10D,
                "Host must reflect the same collision result");
        close(f);
        helper.succeed();
    }

    private static Fixture create(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            for (int y = 2; y < 10; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        Mob host = EntityType.ZOMBIE.create(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        host.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(host);
        EntitySoulManager.removeSoul(host);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "zombie-player-body"));
        SoulManager.setState(player, SoulState.VAGRANT);
        helper.assertTrue(PossessionManager.tryPossess(player, host), "Zombie possession must succeed");
        Fixture f = new Fixture(player, host, host.position());
        reset(f);
        return f;
    }

    private static void reset(Fixture f) {
        f.player.absMoveTo(f.origin.x, f.origin.y, f.origin.z, 0, 0);
        f.player.setOnGround(true);
        f.player.setSprinting(false);
        f.player.setDeltaMovement(Vec3.ZERO);
        PossessionManager.syncBeta16HostToPlayer(f.player, f.host);
    }

    private static void step(Fixture f, float forward, float strafe, boolean jump) {
        f.player.zza = forward;
        f.player.xxa = strafe;
        f.player.setJumping(jump);
        f.player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(f.player, f.host);
    }

    private static void close(Fixture f) {
        PossessionManager.release(f.player, f.host);
        f.host.discard();
    }

    private record Fixture(ServerPlayer player, Mob host, Vec3 origin) { }
}
