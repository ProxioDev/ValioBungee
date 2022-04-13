package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.events.bungee.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.DataManager;
import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;

public class BungeeDataManager extends DataManager<ProxiedPlayer, PostLoginEvent, PlayerDisconnectEvent, PubSubMessageEvent> {

    public BungeeDataManager(RedisBungeePlugin<ProxiedPlayer> plugin) {
        super(plugin);
    }

    @Override
    public void onPostLogin(PostLoginEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPubSubMessage(PubSubMessageEvent event) {
        handlePubSubMessage(event.getChannel(), event.getMessage());
    }
}
