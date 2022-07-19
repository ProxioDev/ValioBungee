package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.AbstractDataManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;


public class VelocityDataManager extends AbstractDataManager<Player, PostLoginEvent, DisconnectEvent, PubSubMessageEvent> {

    public VelocityDataManager(RedisBungeePlugin<Player> plugin) {
        super(plugin);
    }

    @Override
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    @Override
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    @Override
    @Subscribe
    public void onPubSubMessage(PubSubMessageEvent event) {
        handlePubSubMessage(event.getChannel(), event.getMessage());
    }

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    @Override
    public boolean handleKick(UUID target, String message) {
        Player player = plugin.getPlayer(target);
        if (player == null) {
            return false;
        }
        player.disconnect(serializer.deserialize(message));
        return true;
    }
}
