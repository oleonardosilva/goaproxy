package com.openmc.goaproxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

@Plugin(id = "goaproxy",
        authors = "oleonardosilva",
        url = "https://github.com/oleonardosilva/goaproxy",
        description = "Follow the player",
        name = "GoAProxy",
        version = BuildConstants.VERSION)
public class Goaproxy {

    public final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("goa:follow");
    private final Logger logger;
    private final ProxyServer server;
    private final PluginContainer container;
    private final Path dataDirectory;
    private final HashMap<String, String> trackedCache = new HashMap<>();
    private ConfigurationNode config;


    @Inject
    public Goaproxy(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory, PluginContainer container) {
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;
        this.container = container;

        server.getChannelRegistrar().register(CHANNEL);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.config = loadConfig("config.yml");

        new GoaCommand(this).register();
        new OnServerSwitchListener(this).register();
        new OnPluginMessageListener(this).register();

        logger.info("Plugin has been initialized successfully!");
        logger.info("Please, check if the goaproxy-spigot are in all servers!");
    }

    @Subscribe(async = false)
    public void onPlayerDisconnect(DisconnectEvent event) {
        final String username = event.getPlayer().getUsername();
        trackedCache.remove(username);
        for (String s : trackedCache.keySet()) {
            if (trackedCache.get(s).equalsIgnoreCase(username)) {
                trackedCache.remove(s);
                getServer().getPlayer(s).ifPresent(player -> player.sendMessage(Utils.getMessage(config, "error", "player-left")));
            }
        }
    }

    public boolean sendTeleportMessageToBackend(Player player, byte[] data) {
        final Optional<ServerConnection> connection = player.getCurrentServer();
        return connection.map(serverConnection -> serverConnection.sendPluginMessage(CHANNEL, data)).orElse(false);
    }

    private ConfigurationNode loadConfig(String... subpaths) {
        final Path pathConfig = dataDirectory.resolve(Paths.get("", subpaths));
        final String resourcePath = "/" + String.join("/", subpaths);

        if (!Files.exists(pathConfig)) {
            try {
                Files.createDirectories(pathConfig.getParent());
                logger.info("Creating config file {} at {} ", resourcePath, pathConfig.toAbsolutePath());
                Files.copy(
                        Objects.requireNonNull(getClass().getResourceAsStream(resourcePath),
                                "Resource not found: " + resourcePath),
                        pathConfig
                );
            } catch (IOException e) {
                logger.error("Failed to create config file: {}", e.getMessage(), e);
                System.exit(1);
            }
        }

        try {
            final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(pathConfig)
                    .build();

            final CommentedConfigurationNode node = loader.load();
            logger.info("Loaded config file: {}", pathConfig.getFileName());
            return node;
        } catch (IOException e) {
            logger.error("Failed to load YAML config: {}", e.getMessage(), e);
            System.exit(1);
            return null;
        }
    }

    public ConfigurationNode getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public ProxyServer getServer() {
        return server;
    }

    public PluginContainer getContainer() {
        return container;
    }

    public HashMap<String, String> getTrackedCache() {
        return trackedCache;
    }
}
