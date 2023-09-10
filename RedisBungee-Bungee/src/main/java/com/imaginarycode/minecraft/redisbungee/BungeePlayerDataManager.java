/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.api.PlayerDataManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class BungeePlayerDataManager extends PlayerDataManager<ProxiedPlayer, PostLoginEvent, PlayerDisconnectEvent, PubSubMessageEvent, PlayerChangedServerNetworkEvent, PlayerLeftNetworkEvent, ServerConnectedEvent> implements Listener {

    private final BungeeComponentSerializer BUNGEECORD_SERIALIZER = BungeeComponentSerializer.get();

    public BungeePlayerDataManager(RedisBungeePlugin<ProxiedPlayer> plugin) {
        super(plugin);
    }

    @Override
    @EventHandler
    public void onPlayerChangedServerNetworkEvent(PlayerChangedServerNetworkEvent event) {
        super.handleNetworkPlayerServerChange(event);
    }

    @Override
    @EventHandler
    public void onNetworkPlayerQuit(PlayerLeftNetworkEvent event) {
        super.handleNetworkPlayerQuit(event);
    }

    @Override
    @EventHandler
    public void onPubSubMessageEvent(PubSubMessageEvent event) {
        super.handlePubSubMessageEvent(event);
    }

    @Override
    @EventHandler
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        final String currentServer = event.getServer().getInfo().getName();
        final String oldServer = event.getPlayer().getServer() == null ? null : event.getPlayer().getServer().getInfo().getName();
        super.playerChangedServer(event.getPlayer().getUniqueId(), oldServer, currentServer);
    }

    @EventHandler
    public void onLoginEvent(LoginEvent event) {
        event.registerIntent((Plugin) plugin);
        // check if online
        if (getLastOnline(event.getConnection().getUniqueId()) == 0) {
            if (plugin.configuration().kickWhenOnline()) {
                kickPlayer(event.getConnection().getUniqueId(), Component.empty());
                // wait 3 seconds before releasing the event
                plugin.executeAsyncAfter(() -> event.completeIntent((Plugin) plugin), TimeUnit.SECONDS, 3);
            } else {
                event.setCancelled(true);
                event.setCancelReason(BUNGEECORD_SERIALIZER.serialize(Component.empty()));
                event.completeIntent((Plugin) plugin);
            }
        } else {
            event.completeIntent((Plugin) plugin);
        }

    }

    @Override
    @EventHandler
    public void onLoginEvent(PostLoginEvent event) {
        super.addPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getAddress().getAddress());
    }

    @Override
    @EventHandler
    public void onDisconnectEvent(PlayerDisconnectEvent event) {
        super.removePlayer(event.getPlayer().getUniqueId());
    }


}
