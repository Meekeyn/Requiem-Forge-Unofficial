package ua.meekeyn.requiemunofficial.common.command;

import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.soul.VagrantEvents;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class RequiemCommands {
    private static final DynamicCommandExceptionType NOT_LIVING =
            new DynamicCommandExceptionType(entity -> Component.translatable(
                    "requiemunofficial:commands.soul.set.fail.not_living",
                    entity
            ));
    private static final DynamicCommandExceptionType PERMANENTLY_SOULLESS =
            new DynamicCommandExceptionType(entity -> Component.translatable(
                    "requiemunofficial:commands.soul.set.fail.permanently_soulless",
                    entity
            ));
    private static final DynamicCommandExceptionType NOT_MOB =
            new DynamicCommandExceptionType(entity -> Component.translatable(
                    "requiemunofficial:commands.possession.start.fail.not_mob",
                    entity
            ));
    private static final DynamicCommandExceptionType NOT_INCORPOREAL =
            new DynamicCommandExceptionType(player -> Component.translatable(
                    "requiemunofficial:commands.possession.start.fail.not_incorporeal",
                    player
            ));
    private static final DynamicCommandExceptionType POSSESSION_FAILED =
            new DynamicCommandExceptionType(entity -> Component.translatable(
                    "requiemunofficial:commands.possession.start.fail",
                    entity
            ));
    private static final DynamicCommandExceptionType VAGRANT_MORTAL =
            new DynamicCommandExceptionType(player -> Component.translatable(
                    "requiemunofficial:commands.ethereal.set.fail.mortal",
                    player
            ));
    private static final DynamicCommandExceptionType VAGRANT_FAILED =
            new DynamicCommandExceptionType(player -> Component.translatable(
                    "requiemunofficial:commands.ethereal.set.fail",
                    player
            ));
    private static final DynamicCommandExceptionType SHELL_SPLIT_MORTAL =
            new DynamicCommandExceptionType(player -> Component.translatable(
                    "requiemunofficial:commands.shell.split.fail.mortal",
                    player
            ));
    private static final DynamicCommandExceptionType SHELL_SPLIT_VAGRANT =
            new DynamicCommandExceptionType(player -> Component.translatable(
                    "requiemunofficial:commands.shell.split.fail.vagrant",
                    player
            ));
    private static final DynamicCommandExceptionType NOT_SHELL =
            new DynamicCommandExceptionType(entity -> Component.translatable(
                    "requiemunofficial:commands.shell.fail.not_shell",
                    entity
            ));
    private static final DynamicCommandExceptionType SHELL_MERGE_FAILED =
            new DynamicCommandExceptionType(player -> Component.translatable(
                    "requiemunofficial:commands.shell.merge.fail",
                    player
            ));
    private static final SimpleCommandExceptionType TOO_MANY_PROFILES =
            new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));
    private static final DynamicCommandExceptionType REMNANT_TYPE_NOT_FOUND =
            new DynamicCommandExceptionType(type -> Component.translatable(
                    "requiemunofficial:remnant_type.not_found",
                    type
            ));

    private RequiemCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        // Як RequiemCommand.checkPermissions у beta.16: рівень 2 або будь-який вузол requiem.command*.
        event.getDispatcher().register(Commands.literal("requiem")
                .requires(ModPermissions::checkRoot)
                .then(vagrantSubcommand())
                .then(possessionSubcommand())
                .then(remnantSubcommand())
                .then(shellSubcommand())
                .then(soulSubcommand()));
        // Port-only розширення тримаємо в окремому корені, щоб /requiem лишався ідентичним beta.16.
        event.getDispatcher().register(Commands.literal("requiemunofficial")
                .requires(source -> source.hasPermission(2))
                .then(mortalSubcommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> mortalSubcommand() {
        return Commands.literal("mortal")
                .executes(context -> setMortal(
                        context.getSource(),
                        Set.of(context.getSource().getPlayerOrException())
                ))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes(context -> setMortal(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "target")
                        )));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> soulSubcommand() {
        return Commands.literal("soul")
                .requires(ModPermissions.require(ModPermissions.SOUL_QUERY)
                        .or(ModPermissions.require(ModPermissions.SOUL_SET)))
                .then(Commands.literal("query")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .requires(ModPermissions.require(ModPermissions.SOUL_QUERY))
                                .executes(context -> querySoul(
                                        context.getSource(),
                                        EntityArgument.getEntity(context, "target")
                                ))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.entities())
                                .requires(ModPermissions.require(ModPermissions.SOUL_REMOVE))
                                .executes(context -> setSoul(
                                        context.getSource(),
                                        EntityArgument.getEntities(context, "target"),
                                        false
                                ))))
                .then(Commands.literal("restore")
                        .then(Commands.argument("target", EntityArgument.entities())
                                .requires(ModPermissions.require(ModPermissions.SOUL_RESTORE))
                                .executes(context -> setSoul(
                                        context.getSource(),
                                        EntityArgument.getEntities(context, "target"),
                                        true
                                ))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> remnantSubcommand() {
        return Commands.literal("remnant")
                .requires(ModPermissions.require(ModPermissions.REMNANT_QUERY_SELF)
                        .or(ModPermissions.require(ModPermissions.REMNANT_SET_SELF)))
                .then(Commands.literal("query")
                        .requires(ModPermissions.require(ModPermissions.REMNANT_QUERY_SELF))
                        .executes(context -> queryRemnant(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(ModPermissions.require(ModPermissions.REMNANT_QUERY))
                                .executes(context -> queryRemnant(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .requires(ModPermissions.require(ModPermissions.REMNANT_SET_SELF))
                        .then(Commands.argument("remnant_type", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        List.of("true", "false", "requiem:mortal", "requiem:remnant"),
                                        builder
                                ))
                                .executes(context -> setRemnant(
                                        context.getSource(),
                                        Set.of(context.getSource().getPlayerOrException()),
                                        parseRemnantState(context, "remnant_type")
                                ))
                                .then(Commands.argument("target", EntityArgument.players())
                                        .requires(ModPermissions.require(ModPermissions.REMNANT_SET))
                                        .executes(context -> setRemnant(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "target"),
                                                parseRemnantState(context, "remnant_type")
                                        )))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> vagrantSubcommand() {
        return Commands.literal("vagrant")
                .requires(ModPermissions.require(ModPermissions.VAGRANT_QUERY_SELF)
                        .or(ModPermissions.require(ModPermissions.VAGRANT_SET_SELF)))
                .then(Commands.literal("query")
                        .requires(ModPermissions.require(ModPermissions.VAGRANT_QUERY_SELF))
                        .executes(context -> queryVagrant(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(ModPermissions.require(ModPermissions.VAGRANT_QUERY))
                                .executes(context -> queryVagrant(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .requires(ModPermissions.require(ModPermissions.VAGRANT_SET_SELF))
                        .then(Commands.argument("ethereal", BoolArgumentType.bool())
                                .executes(context -> setVagrant(
                                        context.getSource(),
                                        Set.of(context.getSource().getPlayerOrException()),
                                        BoolArgumentType.getBool(context, "ethereal")
                                ))
                                .then(Commands.argument("target", EntityArgument.players())
                                        .requires(ModPermissions.require(ModPermissions.VAGRANT_SET))
                                        .executes(context -> setVagrant(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "target"),
                                                BoolArgumentType.getBool(context, "ethereal")
                                        )))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> possessionSubcommand() {
        return Commands.literal("possession")
                .requires(ModPermissions.require(ModPermissions.POSSESSION_START_SELF)
                        .or(ModPermissions.require(ModPermissions.POSSESSION_STOP_SELF)))
                .then(Commands.literal("stop")
                        .requires(ModPermissions.require(ModPermissions.POSSESSION_STOP_SELF))
                        .executes(context -> stopPossession(
                                context.getSource(),
                                Set.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("target", EntityArgument.players())
                                .requires(ModPermissions.require(ModPermissions.POSSESSION_STOP))
                                .executes(context -> stopPossession(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "target")
                                ))))
                .then(Commands.literal("start")
                        .requires(ModPermissions.require(ModPermissions.POSSESSION_START_SELF))
                        .then(Commands.argument("possessed", EntityArgument.entity())
                                .executes(context -> startPossession(
                                        context.getSource(),
                                        EntityArgument.getEntity(context, "possessed"),
                                        context.getSource().getPlayerOrException()
                                ))
                                .then(Commands.argument("possessor", EntityArgument.player())
                                        .requires(ModPermissions.require(ModPermissions.POSSESSION_START))
                                        .executes(context -> startPossession(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "possessed"),
                                                EntityArgument.getPlayer(context, "possessor")
                                        )))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> shellSubcommand() {
        // В оригіналі shell не має власних вузлів (лише корінь); тут лишаємо рівень 2 — ризик дюпу.
        return Commands.literal("shell")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .executes(context -> runOne(
                                context.getSource().getPlayerOrException(),
                                player -> createIdentityShell(context.getSource(), player)
                        ))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> runOne(
                                        EntityArgument.getPlayer(context, "player"),
                                        player -> createIdentityShell(context.getSource(), player)
                                ))))
                .then(Commands.literal("split")
                        .executes(context -> runOne(
                                context.getSource().getPlayerOrException(),
                                RequiemCommands::splitShell
                        ))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> runMany(
                                        EntityArgument.getPlayers(context, "players"),
                                        RequiemCommands::splitShell
                                ))))
                .then(Commands.literal("merge")
                        .then(Commands.argument("shell", EntityArgument.entity())
                                .executes(context -> runOne(
                                        context.getSource().getPlayerOrException(),
                                        player -> mergeShell(player, EntityArgument.getEntity(context, "shell"))
                                ))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> runOne(
                                                EntityArgument.getPlayer(context, "player"),
                                                player -> mergeShell(player, EntityArgument.getEntity(context, "shell"))
                                        )))))
                .then(Commands.literal("identity")
                        .then(Commands.literal("set")
                                .then(Commands.argument("shells", EntityArgument.entities())
                                        .then(Commands.argument("profile", GameProfileArgument.gameProfile())
                                                .executes(context -> setShellIdentity(context))))));
    }

    private static int querySoul(CommandSourceStack source, Entity target) {
        boolean hasSoul = target instanceof LivingEntity living && EntitySoulManager.hasSoul(living);
        source.sendSuccess(() -> Component.translatable(
                "requiemunofficial:commands.soul.query." + (hasSoul ? "has_soul" : "no_soul"),
                target.getDisplayName()
        ), true);
        return hasSoul ? 1 : 0;
    }

    private static int setSoul(CommandSourceStack source,
                               Collection<? extends Entity> targets,
                               boolean restore) throws CommandSyntaxException {
        int count = 0;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity living)) {
                throw NOT_LIVING.create(target.getDisplayName());
            }
            if (isPermanentlySoulless(living)) {
                throw PERMANENTLY_SOULLESS.create(target.getDisplayName());
            }
            boolean hasSoul = EntitySoulManager.hasSoul(living);
            if (hasSoul != restore) {
                if (restore) {
                    EntitySoulManager.restoreSoul(living);
                    source.sendSuccess(() -> Component.translatable(
                            "requiemunofficial:commands.soul.restore.success",
                            target.getDisplayName()
                    ), true);
                } else {
                    EntitySoulManager.removeSoul(living);
                    source.sendSuccess(() -> Component.translatable(
                            "requiemunofficial:commands.soul.remove.success",
                            target.getDisplayName()
                    ), true);
                }
                count++;
            }
        }
        return count;
    }

    private static int queryRemnant(CommandSourceStack source, ServerPlayer player) {
        SoulState state = SoulManager.getState(player);
        boolean remnant = state == SoulState.REMNANT || state == SoulState.VAGRANT;
        Component remnantName = remnantName(remnant ? SoulState.REMNANT : SoulState.MORTAL);
        source.sendSuccess(() -> queryComponent(source, player, remnantName), true);
        return remnant ? 1 : 0;
    }

    private static int setRemnant(CommandSourceStack source,
                                  Collection<ServerPlayer> players,
                                  SoulState type) {
        int count = 0;
        for (ServerPlayer player : players) {
            SoulState current = SoulManager.getState(player);
            boolean currentRemnant = current == SoulState.REMNANT || current == SoulState.VAGRANT;
            boolean targetRemnant = type == SoulState.REMNANT;
            if (currentRemnant == targetRemnant) {
                continue;
            }
            if (targetRemnant) {
                SoulManager.setState(player, SoulState.REMNANT);
                player.setInvisible(false);
            } else {
                makeMortal(player);
            }
            sendSetRemnantFeedback(source, player, type);
            count++;
        }
        return count;
    }

    private static int queryVagrant(CommandSourceStack source, ServerPlayer player) {
        // Як isVagrant() у beta.16: true і під час одержимості.
        boolean vagrant = SoulManager.getState(player) == SoulState.VAGRANT;
        Component name = Component.translatable("requiemunofficial:" + (vagrant ? "ethereal" : "not_ethereal"));
        source.sendSuccess(() -> queryComponent(source, player, name), true);
        return vagrant ? 1 : 0;
    }

    private static int setVagrant(CommandSourceStack source,
                                  Collection<ServerPlayer> players,
                                  boolean vagrant) throws CommandSyntaxException {
        int count = 0;
        for (ServerPlayer player : players) {
            boolean current = SoulManager.getState(player) == SoulState.VAGRANT;
            if (current == vagrant) {
                continue;
            }
            if (vagrant) {
                if (SoulManager.getState(player) != SoulState.REMNANT) {
                    throw VAGRANT_MORTAL.create(player.getDisplayName());
                }
                if (!SoulLifecycleManager.splitPlayer(player)) {
                    throw VAGRANT_FAILED.create(player.getDisplayName());
                }
            } else {
                // Спершу перевіряємо оболонку, щоб не випустити гравця з носія даремно; далі відпускаємо носія.
                PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
                if (shell == null) {
                    throw VAGRANT_FAILED.create(player.getDisplayName());
                }
                LivingEntity host = PossessionManager.findPossessedHost(player);
                if (host != null) {
                    PossessionManager.release(player, host);
                }
                if (!SoulLifecycleManager.tryReclaimShell(player, shell)) {
                    throw VAGRANT_FAILED.create(player.getDisplayName());
                }
            }
            sendSetVagrantFeedback(source, player, vagrant);
            count++;
        }
        return count;
    }

    private static int setMortal(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (SoulManager.getState(player) == SoulState.MORTAL
                    && SoulManager.getPossessedEntityId(player) == null
                    && SoulLifecycleManager.findShell(player) == null) {
                continue;
            }
            makeMortal(player);
            sendSetRemnantFeedback(source, player, SoulState.MORTAL);
            count++;
        }
        return count;
    }

    private static int startPossession(CommandSourceStack source,
                                       Entity possessed,
                                       ServerPlayer player) throws CommandSyntaxException {
        if (!(possessed instanceof Mob mob)) {
            throw NOT_MOB.create(possessed.getDisplayName());
        }
        if (SoulManager.getState(player) != SoulState.VAGRANT
                || SoulManager.getPossessedEntityId(player) != null) {
            throw NOT_INCORPOREAL.create(player.getDisplayName());
        }
        if (!PossessionManager.tryPossess(player, mob)) {
            throw POSSESSION_FAILED.create(possessed.getDisplayName());
        }
        source.sendSuccess(() -> source.getEntity() == player
                ? Component.translatable("requiemunofficial:commands.possession.start.success.self",
                possessed.getDisplayName())
                : Component.translatable("requiemunofficial:commands.possession.start.success.other",
                player.getDisplayName(), possessed.getDisplayName()), true);
        return 1;
    }

    private static int stopPossession(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            LivingEntity host = PossessionManager.findPossessedHost(player);
            if (host != null) {
                Component hostName = host.getDisplayName();
                PossessionManager.release(player, host);
                source.sendSuccess(() -> source.getEntity() == player
                        ? Component.translatable("requiemunofficial:commands.possession.stop.success.self", hostName)
                        : Component.translatable("requiemunofficial:commands.possession.stop.success.other",
                        player.getDisplayName(), hostName), true);
                count++;
            }
        }
        return count;
    }

    private static void createIdentityShell(CommandSourceStack source, ServerPlayer player) {
        PlayerShellEntity shell = new PlayerShellEntity(ModEntities.PLAYER_SHELL.get(), player.serverLevel());
        shell.moveTo(source.getPosition().x, source.getPosition().y, source.getPosition().z,
                player.getYRot(), player.getXRot());
        // Оригінал: PlayerSplitter.createShell(player) копіює дані гравця разом з інвентарем,
        // а сам гравець нічого не втрачає (без finishBodySplit і без прив'язки оболонки).
        shell.captureBody(player);
        player.serverLevel().addFreshEntity(shell);
    }

    private static void splitShell(ServerPlayer player) throws CommandSyntaxException {
        SoulState state = SoulManager.getState(player);
        if (state == SoulState.MORTAL) {
            throw SHELL_SPLIT_MORTAL.create(player.getDisplayName());
        }
        if (state == SoulState.VAGRANT) {
            throw SHELL_SPLIT_VAGRANT.create(player.getDisplayName());
        }
        if (!SoulLifecycleManager.splitPlayer(player)) {
            throw SHELL_SPLIT_MORTAL.create(player.getDisplayName());
        }
    }

    private static void mergeShell(ServerPlayer player, Entity entity) throws CommandSyntaxException {
        if (!(entity instanceof PlayerShellEntity shell)) {
            throw NOT_SHELL.create(entity.getDisplayName());
        }
        // Оригінал: RemnantComponent.get(player).merge(shell) — результат ігнорується, без повідомлення.
        SoulLifecycleManager.mergeShellForCommand(player, shell);
    }

    private static int setShellIdentity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "profile");
        if (profiles.size() > 1) {
            throw TOO_MANY_PROFILES.create();
        }
        GameProfile profile = profiles.iterator().next();
        UUID id = profile.getId();
        if (id == null) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        }
        return runMany(EntityArgument.getEntities(context, "shells"), shell -> {
            if (!(shell instanceof PlayerShellEntity playerShell)) {
                throw NOT_SHELL.create(shell.getDisplayName());
            }
            playerShell.setDisplayProfile(profile);
        });
    }

    private static boolean isPermanentlySoulless(LivingEntity entity) {
        return entity instanceof PlayerShellEntity || entity.getType().is(ModTags.SOULLESS);
    }

    private static void makeMortal(ServerPlayer player) {
        LivingEntity host = PossessionManager.findPossessedHost(player);
        if (host != null) {
            PossessionManager.release(player, host);
        } else {
            SoulManager.clearPossession(player);
        }
        SoulLifecycleManager.removeShell(player);
        player.stopUsingItem();
        SoulManager.clearResurrectionPoint(player);
        SoulManager.setState(player, SoulState.MORTAL);
        player.setInvisible(false);
        VagrantEvents.refreshAfterSoulStateChange(player);
        SoulManager.sync(player);
    }

    private static void sendSetRemnantFeedback(CommandSourceStack source, ServerPlayer player, SoulState type) {
        Component name = remnantName(type);
        if (source.getEntity() == player) {
            source.sendSuccess(() -> Component.translatable(
                    "requiemunofficial:commands.remnant.set.success.self",
                    name
            ), true);
            return;
        }
        if (source.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            player.sendSystemMessage(Component.translatable(
                    "requiemunofficial:commands.remnant.set.target",
                    name
            ));
        }
        source.sendSuccess(() -> Component.translatable(
                "requiemunofficial:commands.remnant.set.success.other",
                player.getDisplayName(),
                name
        ), true);
    }

    private static void sendSetVagrantFeedback(CommandSourceStack source, ServerPlayer player, boolean vagrant) {
        Component name = Component.translatable("requiemunofficial:" + (vagrant ? "ethereal" : "not_ethereal"));
        if (source.getEntity() == player) {
            source.sendSuccess(() -> Component.translatable(
                    "requiemunofficial:commands.ethereal.set.success.self",
                    name
            ), true);
            return;
        }
        if (source.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            player.sendSystemMessage(Component.translatable(
                    "requiemunofficial:commands.ethereal.set.target",
                    name
            ));
        }
        source.sendSuccess(() -> Component.translatable(
                "requiemunofficial:commands.ethereal.set.success.other",
                player.getDisplayName(),
                name
        ), true);
    }

    private static Component queryComponent(CommandSourceStack source, ServerPlayer player, Component stateName) {
        return source.getEntity() == player
                ? Component.translatable("requiemunofficial:commands.query.success.self", stateName)
                : Component.translatable("requiemunofficial:commands.query.success.other",
                stateName, player.getDisplayName());
    }

    private static Component remnantName(SoulState state) {
        // Ключ як у SimpleRemnantType beta.16: remnant_type.<id з '.' замість ':'>.name
        return Component.translatable(state == SoulState.REMNANT
                ? "requiemunofficial:remnant_type.requiem.remnant.name"
                : "requiemunofficial:remnant_type.requiem.mortal.name");
    }

    private static SoulState parseRemnantState(CommandContext<CommandSourceStack> context,
                                               String name) throws CommandSyntaxException {
        // Аналог RemnantArgumentType: true/false, id без простору імен трактуємо як requiem:<id>.
        ResourceLocation id = ResourceLocationArgument.getId(context, name);
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("minecraft".equals(namespace)) {
            if ("true".equals(path)) {
                return SoulState.REMNANT;
            }
            if ("false".equals(path)) {
                return SoulState.MORTAL;
            }
            namespace = "requiem";
        }
        if ("requiem".equals(namespace) || RequiemUnofficial.MODID.equals(namespace)) {
            if ("remnant".equals(path)) {
                return SoulState.REMNANT;
            }
            if ("mortal".equals(path)) {
                return SoulState.MORTAL;
            }
        }
        throw REMNANT_TYPE_NOT_FOUND.create("minecraft".equals(id.getNamespace())
                ? ResourceLocation.fromNamespaceAndPath("requiem", path).toString()
                : id.toString());
    }

    private static <T> int runOne(T element, ThrowingConsumer<T> action) throws CommandSyntaxException {
        action.accept(element);
        return 1;
    }

    private static <T> int runMany(Collection<T> collection,
                                   ThrowingConsumer<T> action) throws CommandSyntaxException {
        int count = 0;
        for (T element : collection) {
            action.accept(element);
            count++;
        }
        return count;
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws CommandSyntaxException;
    }
}
