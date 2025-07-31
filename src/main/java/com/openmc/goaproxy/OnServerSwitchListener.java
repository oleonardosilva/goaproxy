package com.openmc.goaproxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.HashMap;

public class OnServerSwitchListener {

    private final Goaproxy goaproxy;

    public OnServerSwitchListener(Goaproxy goaproxy) {
        this.goaproxy = goaproxy;
    }

    public void register() {
        goaproxy.getServer().getEventManager().register(goaproxy, this);
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        final ConfigurationNode config = goaproxy.getConfig();
        final String switchedPlayer = event.getPlayer().getUsername();
        final HashMap<String, String> trackedCache = goaproxy.getTrackedCache();

        trackedCache.forEach((s, s2) -> {
            if (s2.equals(switchedPlayer)) {
                goaproxy.getServer().getPlayer(s).ifPresentOrElse(player -> player
                        .createConnectionRequest(event.getPlayer().getCurrentServer().get().getServer())
                        .connect()
                        .thenAccept(result -> {
                                    if (!result.isSuccessful()) {
                                        player.sendMessage(Utils.getMessage(config, "error", "unknown"));
                                        result.getReasonComponent().ifPresent(player::sendMessage);
                                    }

                                    final ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                    out.writeUTF("TeleportTo");
                                    out.writeUTF(switchedPlayer);

                                    if (!goaproxy.sendTeleportMessageToBackend(player, out.toByteArray())) {
                                        player.sendMessage(Utils.getMessage(config, "error", "unknown"));
                                    } else player.sendMessage(Utils.getMessage(config, "success", "server-change"));
                                }
                        ), () -> trackedCache.remove(s));
            }
        });
    }

}
