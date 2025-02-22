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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class BungeePlayerDataManager extends PlayerDataManager<ProxiedPlayer> implements Listener {

    private final RedisBungee bPlugin;
    public BungeePlayerDataManager(RedisBungee plugin) {
        super(plugin);
        bPlugin = plugin;
    }

    @EventHandler
    public void onPlayerChangedServerNetworkEvent(PlayerChangedServerNetworkEvent event) {
        super.handleNetworkPlayerServerChange(event);
    }

    @EventHandler
    public void onNetworkPlayerQuit(PlayerLeftNetworkEvent event) {
        super.handleNetworkPlayerQuit(event);
    }

    @EventHandler
    public void onNetworkPlayerJoin(PlayerJoinedNetworkEvent event) {
        super.handleNetworkPlayerJoin(event);
    }

    @EventHandler
    public void onPubSubMessageEvent(PubSubMessageEvent event) {
        super.handlePubSubMessageEvent(event);
    }

    @EventHandler
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        final String currentServer = event.getServer().getInfo().getName();
        final String oldServer = event.getPlayer().getServer() == null ? null : event.getPlayer().getServer().getInfo().getName();
        super.playerChangedServer(event.getPlayer().getUniqueId(), oldServer, currentServer);
    }

    private final BungeeComponentSerializer BUNGEE_COMPONENT_SERIALIZER = BungeeComponentSerializer.get();

    private final static MiniMessage MINI_MESSAGE_SERIALIZER = MiniMessage.miniMessage();
    @Override
    public boolean handleSerializedKick(UUID uuid, String serializedMiniMessage) {
        ProxiedPlayer player = plugin.getPlayer(uuid);
        if (player == null) return false;
        // decode the adventure component
        if (serializedMiniMessage == null) {
            // kick the player too even if the message is invalid
            player.disconnect(BUNGEE_COMPONENT_SERIALIZER.serialize(Component.empty()));
            plugin.logWarn("unable to decode serialized adventure component because its empty or null");
        }  else {
            Component message = MINI_MESSAGE_SERIALIZER.deserialize(serializedMiniMessage);
            player.disconnect(BUNGEE_COMPONENT_SERIALIZER.serialize(message));
        }
        return true;
    }

    public void kickPlayer(UUID player, Component message) {
        serializedPlayerKick(player, MINI_MESSAGE_SERIALIZER.serialize(message));
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
                    kickPlayer(event.getConnection().getUniqueId(), bPlugin.langConfiguration().messages().loggedInFromOtherLocation());
                    // wait 3 seconds before releasing the event
                    plugin.executeAsyncAfter(() -> event.completeIntent((Plugin) plugin), TimeUnit.SECONDS, 3);
                } else {
                    event.setCancelled(true);
                    event.setCancelReason(BungeeComponentSerializer.get().serialize(bPlugin.langConfiguration().messages().alreadyLoggedIn()));
                    event.completeIntent((Plugin) plugin);
                }
            }
        } else {
            event.completeIntent((Plugin) plugin);
        }

    }


    @EventHandler
    public void onLoginEvent(PostLoginEvent event) {
        super.addPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName(), event.getPlayer().getAddress().getAddress());
    }


    @EventHandler
    public void onDisconnectEvent(PlayerDisconnectEvent event) {
        super.removePlayer(event.getPlayer().getUniqueId());
    }


}
