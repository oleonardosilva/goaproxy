package com.openmc.goaproxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.TextComponent;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.HashMap;
import java.util.Optional;

public class GoaCommand {

    private final Goaproxy goaproxy;
    private final ProxyServer server;
    private final ConfigurationNode config;

    public GoaCommand(Goaproxy goaproxy) {
        this.goaproxy = goaproxy;
        this.server = goaproxy.getServer();
        this.config = goaproxy.getConfig();
    }

    public void register() {
        final CommandManager commandManager = server.getCommandManager();
        final CommandMeta commandMeta = commandManager.metaBuilder("goa")
                .plugin(goaproxy)
                .build();

        final LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("goa")
                .requires(source -> {
                    final boolean b = source.hasPermission(GoaPermissions.USE.getPermission());
                    if (!b) {
                        final TextComponent text = Utils.getMessage(config, "error", "permission");
                        source.sendMessage(text);
                    }
                    return b;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            final String input = builder.getRemaining().toLowerCase();
                            server.matchPlayer(input).forEach(player -> builder.suggest(player.getUsername()));
                            return builder.buildFuture();
                        })
                        .executes(this::goa)
                )
                .executes(this::goa)
                .build();

        final BrigadierCommand command = new BrigadierCommand(node);
        commandManager.register(commandMeta, command);
    }

    @SuppressWarnings("SameReturnValue")
    private int goa(CommandContext<CommandSource> context) {
        final CommandSource source = context.getSource();
        final HashMap<String, String> trackedCache = goaproxy.getTrackedCache();
        if (source instanceof Player player) {
            final String follower = player.getUsername().toLowerCase();
            final String targetName = context.getArguments().containsKey("player") ? context.getArgument("player", String.class) : null;

            if (targetName == null) {
                if (trackedCache.containsKey(follower)) {
                    trackedCache.remove(follower);
                    source.sendMessage(Utils.getMessage(config, "success", "disabled"));
                } else {
                    source.sendMessage(Utils.getMessage(config, "error", "not-track"));
                }
                return Command.SINGLE_SUCCESS;
            }

            final Optional<Player> optPlayer = server.getPlayer(targetName);
            if (optPlayer.isEmpty()) {
                source.sendMessage(Utils.getMessage(config, "error", "player-not-found"));
                return Command.SINGLE_SUCCESS;
            }

            optPlayer.get().getCurrentServer().ifPresentOrElse(serverConnection -> {
                trackedCache.put(follower, targetName);
                source.sendMessage(Utils.getMessage(config, "success", "tracked"));

                if (!serverConnection.getServerInfo().getName().equals(player.getCurrentServer().get().getServerInfo().getName())) {
                    player.createConnectionRequest(serverConnection.getServer()).connect().thenAccept(result -> {
                        if (!result.isSuccessful()) {
                            source.sendMessage(Utils.getMessage(config, "error", "unknown"));
                            result.getReasonComponent().ifPresent(source::sendMessage);
                        }

                        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("TeleportTo");
                        out.writeUTF(targetName);

                        if (!goaproxy.sendTeleportMessageToBackend(player, out.toByteArray())) {
                            source.sendMessage(Utils.getMessage(config, "error", "unknown"));
                        }
                    });
                }
            }, () -> source.sendMessage(Utils.getMessage(config, "error", "not-connected")));

        } else {
            source.sendMessage(Utils.getMessage(config, "error", "only-player"));
        }

        return Command.SINGLE_SUCCESS;
    }
}
