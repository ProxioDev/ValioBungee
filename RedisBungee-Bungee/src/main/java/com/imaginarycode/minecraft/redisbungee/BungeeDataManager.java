package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.AbstractDataManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class BungeeDataManager extends AbstractDataManager<ProxiedPlayer, PostLoginEvent, PlayerDisconnectEvent, PubSubMessageEvent> implements Listener {

    public BungeeDataManager(RedisBungeePlugin<ProxiedPlayer> plugin) {
        super(plugin);
    }

    @Override
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    @Override
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    @Override
    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {
        handlePubSubMessage(event.getChannel(), event.getMessage());
    }

    @Override
    public boolean handleKick(UUID target, String message) {
        // check if the player is online on this proxy
        ProxiedPlayer player = plugin.getPlayer(target);
        if (player == null) return false;
        player.disconnect(TextComponent.fromLegacyText(message));
        return true;
    }
}
