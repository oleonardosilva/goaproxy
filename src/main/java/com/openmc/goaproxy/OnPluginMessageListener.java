package com.openmc.goaproxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class OnPluginMessageListener {

    private final Goaproxy goaproxy;

    public OnPluginMessageListener(Goaproxy goaproxy) {
        this.goaproxy = goaproxy;
    }

    public void register() {
        goaproxy.getServer().getEventManager().register(goaproxy, this);
    }

    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        if (!goaproxy.CHANNEL.equals(event.getIdentifier())) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            final String subchannel = in.readUTF();

            if (subchannel.equals("WorldChange")) {
                final String switchedPlayer = in.readUTF();

                goaproxy.getTrackedCache().forEach((s, s2) -> {
                    if (s2.equals(switchedPlayer)) {
                        goaproxy.getServer().getPlayer(s).ifPresentOrElse(player -> {

                                    final ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                    out.writeUTF("TeleportTo");
                                    out.writeUTF(switchedPlayer);

                                    if (!goaproxy.sendTeleportMessageToBackend(player, out.toByteArray())) {
                                        player.sendMessage(Utils.getMessage(goaproxy.getConfig(), "error", "unknown"));
                                    } else
                                        player.sendMessage(Utils.getMessage(goaproxy.getConfig(), "success", "server-change"));
                                }
                                , () -> goaproxy.getTrackedCache().remove(s));
                    }
                });
            }

        } catch (IOException e) {
            goaproxy.getLogger().error(e.getMessage(), e);
        }
    }


}
