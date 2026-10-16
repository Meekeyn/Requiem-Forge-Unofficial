package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.init.ModParticles;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientVagrantPhaseFx {
    private static Vec3 previousPosition = Vec3.ZERO;
    private static int soundCooldown;
    private static boolean phasing;

    private ClientVagrantPhaseFx() {
    }

    /**
     * Whether the local player is currently passing through a wall (or is inside a phaseable block).
     * Shared by the phase trail particles and the soulless body outline; refreshed once per tick
     * so render-time consumers never have to re-evaluate block collision queries per entity.
     */
    public static boolean isPhasing() {
        return phasing;
    }

    private static boolean isPhasing(Player player) {
        return (VagrantRules.isPhaseCharged(player) && VagrantRules.isPhaseAttempting(player))
                || VagrantRules.isInsidePhaseableBlock(player);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            previousPosition = Vec3.ZERO;
            soundCooldown = 0;
            phasing = false;
            return;
        }

        phasing = isPhasing(player);
        boolean leafOnly = phasing
                && !VagrantRules.isInsideNonLeafPhaseableBlock(player)
                && VagrantRules.isInsideLeaves(player);
        if (!phasing) {
            previousPosition = player.position();
            if (soundCooldown > 0) {
                soundCooldown--;
            }
            return;
        }

        if (!leafOnly) {
            spawnPhaseTrail(level, player);
        }
        if (soundCooldown <= 0) {
            level.playLocalSound(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS,
                    0.55F,
                    0.75F + level.random.nextFloat() * 0.2F,
                    false
            );
            soundCooldown = 14;
        } else {
            soundCooldown--;
        }
        previousPosition = player.position();
    }

    private static void spawnPhaseTrail(ClientLevel level, LocalPlayer player) {
        Vec3 start = previousPosition == Vec3.ZERO ? player.position() : previousPosition;
        Vec3 intendedMovement = player.position().subtract(start);

        // Beta 16 look: ten soul particles spawned at random points strictly inside the
        // player's hitbox, drifting along the intended movement. They never scatter outwards.
        for (int i = 0; i < 10; i++) {
            level.addParticle(
                    ModParticles.GHOST.get(),
                    player.getRandomX(0.5D),
                    player.getRandomY(),
                    player.getRandomZ(0.5D),
                    intendedMovement.x * 0.2D,
                    intendedMovement.y * 0.2D,
                    intendedMovement.z * 0.2D
            );
        }
    }
}
