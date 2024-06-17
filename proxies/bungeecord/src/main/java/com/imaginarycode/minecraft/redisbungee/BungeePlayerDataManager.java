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
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;


public class BungeePlayerDataManager extends PlayerDataManager<ProxiedPlayer, PostLoginEvent, PlayerDisconnectEvent, PubSubMessageEvent, PlayerChangedServerNetworkEvent, PlayerLeftNetworkEvent, ServerConnectedEvent, PlayerJoinedNetworkEvent> implements Listener {

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
    public void onNetworkPlayerJoin(PlayerJoinedNetworkEvent event) {
        super.handleNetworkPlayerJoin(event);
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
            // because something can go wrong and proxy somehow does not update player data correctly on shutdown
            // we have to check proxy if it has the player
            String proxyId = getProxyFor(event.getConnection().getUniqueId());
            if (proxyId == null || !plugin.proxyDataManager().isPlayerTrulyOnProxy(proxyId, event.getConnection().getUniqueId())) {
                event.completeIntent((Plugin) plugin);
            } else {
                if (plugin.configuration().kickWhenOnline()) {
                    kickPlayer(event.getConnection().getUniqueId(), plugin.langConfiguration().messages().loggedInFromOtherLocation());
                    // wait 3 seconds before releasing the event
                    plugin.executeAsyncAfter(() -> event.completeIntent((Plugin) plugin), TimeUnit.SECONDS, 3);
                } else {
                    event.setCancelled(true);
                    event.setCancelReason(BungeeComponentSerializer.get().serialize(plugin.langConfiguration().messages().alreadyLoggedIn()));
                    event.completeIntent((Plugin) plugin);
                }
            }
        } else {
            event.completeIntent((Plugin) plugin);
        }

    }

    @Override
    @EventHandler
    public void onLoginEvent(PostLoginEvent event) {
        super.addPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName(), event.getPlayer().getAddress().getAddress());
    }

    @Override
    @EventHandler
    public void onDisconnectEvent(PlayerDisconnectEvent event) {
        super.removePlayer(event.getPlayer().getUniqueId());
    }


}
