package com.openmc.goaproxy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;

public class Utils {

    public static TextComponent getMessage(ConfigurationNode node, Object... path) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(node.node(path).getString("§cFind Message Error"));
    }

    public static TextComponent getMessageList(ConfigurationNode node, Object... path) {
        final TextComponent.Builder text = Component.text();
        try {
            final List<String> list = node.node(path).getList(String.class);
            if (list != null) {
                text.content(String.join("", list).replace("&", "§"));
            }
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        return text.build();
    }

}
