package ua.meekeyn.requiemunofficial.common.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.exceptions.UnregisteredPermissionException;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Вузли прав команд /requiem, 1:1 з beta.16 RequiemCommand.permission(name):
 * повна назва "requiem.command" + name (без крапки після "command"), типовий рівень — 2.
 */
@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class ModPermissions {
    // Простір імен як в оригіналі, щоб вузли в LuckPerms тощо збігалися з beta.16.
    private static final String NAMESPACE = "requiem";
    private static final int DEFAULT_LEVEL = 2;
    private static final List<PermissionNode<Boolean>> NODES = new ArrayList<>();

    public static final PermissionNode<Boolean> VAGRANT_QUERY_SELF = node("vagrant.query.self");
    public static final PermissionNode<Boolean> VAGRANT_QUERY = node("vagrant.query");
    public static final PermissionNode<Boolean> VAGRANT_SET_SELF = node("vagrant.set.self");
    public static final PermissionNode<Boolean> VAGRANT_SET = node("vagrant.set");
    public static final PermissionNode<Boolean> POSSESSION_START_SELF = node("possession.start.self");
    public static final PermissionNode<Boolean> POSSESSION_START = node("possession.start");
    public static final PermissionNode<Boolean> POSSESSION_STOP_SELF = node("possession.stop.self");
    public static final PermissionNode<Boolean> POSSESSION_STOP = node("possession.stop");
    public static final PermissionNode<Boolean> REMNANT_QUERY_SELF = node("remnant.query.self");
    public static final PermissionNode<Boolean> REMNANT_QUERY = node("remnant.query");
    public static final PermissionNode<Boolean> REMNANT_SET_SELF = node("remnant.set.self");
    public static final PermissionNode<Boolean> REMNANT_SET = node("remnant.set");
    public static final PermissionNode<Boolean> SOUL_QUERY = node("soul.query");
    public static final PermissionNode<Boolean> SOUL_SET = node("soul.set");
    public static final PermissionNode<Boolean> SOUL_REMOVE = node("soul.remove");
    public static final PermissionNode<Boolean> SOUL_RESTORE = node("soul.restore");

    private ModPermissions() {
    }

    private static PermissionNode<Boolean> node(String name) {
        // PermissionNode(modID, nodeName) склеює "modID.nodeName" -> "requiem.command<name>".
        PermissionNode<Boolean> node = new PermissionNode<>(
                NAMESPACE,
                "command" + name,
                PermissionTypes.BOOLEAN,
                (player, uuid, context) -> player != null && player.hasPermissions(DEFAULT_LEVEL)
        );
        NODES.add(node);
        return node;
    }

    public static List<PermissionNode<Boolean>> nodes() {
        return Collections.unmodifiableList(NODES);
    }

    @SubscribeEvent
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(new ArrayList<>(NODES));
    }

    /** Аналог Permissions.require(perm, 2) з fabric-permissions-api. */
    public static Predicate<CommandSourceStack> require(PermissionNode<Boolean> node) {
        return source -> check(source, node);
    }

    public static boolean check(CommandSourceStack source, PermissionNode<Boolean> node) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return source.hasPermission(DEFAULT_LEVEL);
        }
        // Функції / командні блоки через execute as мають власний рівень прав, вищий за рівень гравця:
        // як і в оригіналі (fallback на рівень джерела), такий контекст пропускаємо.
        if (source.hasPermission(DEFAULT_LEVEL) && !player.hasPermissions(DEFAULT_LEVEL)) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(PermissionAPI.getPermission(player, node));
        } catch (UnregisteredPermissionException e) {
            // Вузли ще не зібрані (не мало б трапитися) — поводимося як без провайдера прав.
            return source.hasPermission(DEFAULT_LEVEL);
        }
    }

    /** Корінь /requiem: рівень 2 АБО будь-який вузол requiem.command* (як checkPermissions в оригіналі). */
    public static boolean checkRoot(CommandSourceStack source) {
        if (source.hasPermission(DEFAULT_LEVEL)) {
            return true;
        }
        for (PermissionNode<Boolean> node : NODES) {
            if (check(source, node)) {
                return true;
            }
        }
        return false;
    }
}
