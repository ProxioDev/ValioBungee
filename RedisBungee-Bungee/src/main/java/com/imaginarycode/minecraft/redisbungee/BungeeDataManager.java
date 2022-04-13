package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.events.bungee.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.DataManager;
import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeDataManager extends DataManager<ProxiedPlayer, PostLoginEvent, PlayerDisconnectEvent, PubSubMessageEvent> implements Listener {

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
}
