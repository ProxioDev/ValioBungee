package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.DataManager;
import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;


public class VelocityDataManager extends DataManager<Player, PostLoginEvent, DisconnectEvent, PubSubMessageEvent> {

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
}
