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
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.concurrent.TimeUnit;

public class VelocityPlayerDataManager extends PlayerDataManager<Player, PostLoginEvent, DisconnectEvent, PubSubMessageEvent, PlayerChangedServerNetworkEvent, PlayerLeftNetworkEvent, ServerConnectedEvent, PlayerJoinedNetworkEvent> {
    public VelocityPlayerDataManager(RedisBungeePlugin<Player> plugin) {
        super(plugin);
    }

    @Override
    @Subscribe
    public void onPlayerChangedServerNetworkEvent(PlayerChangedServerNetworkEvent event) {
        handleNetworkPlayerServerChange(event);
    }

    @Override
    @Subscribe
    public void onNetworkPlayerQuit(PlayerLeftNetworkEvent event) {
        handleNetworkPlayerQuit(event);
    }

    @Override
    @Subscribe
    public void onNetworkPlayerJoin(PlayerJoinedNetworkEvent event) {
        handleNetworkPlayerJoin(event);
    }

    @Override
    @Subscribe
    public void onPubSubMessageEvent(PubSubMessageEvent event) {
        handlePubSubMessageEvent(event);
    }

    @Override
    @Subscribe
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        final String currentServer = event.getServer().getServerInfo().getName();
        final String oldServer;
        if (event.getPreviousServer().isPresent()) {
            oldServer = event.getPreviousServer().get().getServerInfo().getName();
        } else {
            oldServer = null;
        }
        super.playerChangedServer(event.getPlayer().getUniqueId(), oldServer, currentServer);
    }

    @Subscribe
    public void onLoginEvent(LoginEvent event, Continuation continuation) {
        // check if online
        if (getLastOnline(event.getPlayer().getUniqueId()) == 0) {
            // because something can go wrong and proxy somehow does not update player data correctly on shutdown
            // we have to check proxy if it has the player
            String proxyId = getProxyFor(event.getPlayer().getUniqueId());
            if (proxyId == null || !plugin.proxyDataManager().isPlayerTrulyOnProxy(proxyId, event.getPlayer().getUniqueId())) {
                continuation.resume();
            } else {
                if (plugin.configuration().kickWhenOnline()) {
                    kickPlayer(event.getPlayer().getUniqueId(), plugin.langConfiguration().messages().loggedInFromOtherLocation());
                    // wait 3 seconds before releasing the event
                    plugin.executeAsyncAfter(continuation::resume, TimeUnit.SECONDS, 3);
                } else {
                    event.setResult(ResultedEvent.ComponentResult.denied(plugin.langConfiguration().messages().alreadyLoggedIn()));
                    continuation.resume();
                }
            }
        } else {
            continuation.resume();
        }
    }

    @Override
    @Subscribe
    public void onLoginEvent(PostLoginEvent event) {
        addPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getUsername(), event.getPlayer().getRemoteAddress().getAddress());
    }

    @Override
    @Subscribe
    public void onDisconnectEvent(DisconnectEvent event) {
        if (event.getLoginStatus() == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN || event.getLoginStatus() == DisconnectEvent.LoginStatus.PRE_SERVER_JOIN) {
            removePlayer(event.getPlayer().getUniqueId());
        }
    }
}
