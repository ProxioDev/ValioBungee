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

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.imaginarycode.minecraft.redisbungee.api.AbstractRedisBungeeListener;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.util.player.PlayerUtils;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import redis.clients.jedis.UnifiedJedis;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

import static com.imaginarycode.minecraft.redisbungee.api.util.serialize.Serializations.serializeMultimap;
import static com.imaginarycode.minecraft.redisbungee.api.util.serialize.Serializations.serializeMultiset;

public class RedisBungeeVelocityListener extends AbstractRedisBungeeListener<LoginEvent, PostLoginEvent, DisconnectEvent, ServerConnectedEvent, ProxyPingEvent, PluginMessageEvent, PubSubMessageEvent> {
    // Some messages are using legacy characters
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public RedisBungeeVelocityListener(RedisBungeePlugin<?> plugin, List<InetAddress> exemptAddresses) {
        super(plugin, exemptAddresses);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onLogin(LoginEvent event, Continuation continuation) {
        plugin.executeAsync(new RedisTask<Void>(plugin) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                try {
                    if (!event.getResult().isAllowed()) {
                        return null;
                    }
                    if (api.isPlayerOnline(event.getPlayer().getUniqueId())) {
                        PlayerUtils.setKickedOtherLocation(event.getPlayer().getUniqueId().toString(), unifiedJedis);
                        api.kickPlayer(event.getPlayer().getUniqueId(), plugin.getConfiguration().getMessages().get(RedisBungeeConfiguration.MessageType.LOGGED_IN_OTHER_LOCATION));
                    }
                    return null;
                } finally {
                    continuation.resume();
                }
            }

        });
    }

    @Override
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        plugin.executeAsync(new RedisTask<Void>(plugin) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                plugin.getUuidTranslator().persistInfo(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), unifiedJedis);
                VelocityPlayerUtils.createVelocityPlayer(event.getPlayer(), unifiedJedis, true);
                return null;
            }
        });
    }

    @Override
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        plugin.executeAsync(new RedisTask<Void>(plugin) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                PlayerUtils.cleanUpPlayer(event.getPlayer().getUniqueId().toString(), unifiedJedis, true);
                return null;
            }

        });

    }

    @Override
    @Subscribe
    public void onServerChange(ServerConnectedEvent event) {
        final String currentServer = event.getServer().getServerInfo().getName();
        final String oldServer = event.getPreviousServer().map(serverConnection -> serverConnection.getServerInfo().getName()).orElse(null);
        plugin.executeAsync(new RedisTask<Void>(plugin) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                unifiedJedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "server", currentServer);
                PayloadUtils.playerServerChangePayload(event.getPlayer().getUniqueId(), unifiedJedis, currentServer, oldServer);
                return null;
            }
        });
    }

    @Override
    @Subscribe(order = PostOrder.EARLY)
    public void onPing(ProxyPingEvent event) {
        if (exemptAddresses.contains(event.getConnection().getRemoteAddress().getAddress())) {
            return;
        }
        ServerPing.Builder ping = event.getPing().asBuilder();
        ping.onlinePlayers(plugin.getCount());
        event.setPing(ping.build());
    }

    @Override
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection) || !RedisBungeeVelocityPlugin.IDENTIFIERS.contains(event.getIdentifier())) {
            return;
        }

        event.setResult(ForwardResult.handled());

        plugin.executeAsync(() -> {
            ByteArrayDataInput in = event.dataAsDataStream();

            String subchannel = in.readUTF();
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            String type;

            switch (subchannel) {
                case "PlayerList":
                    out.writeUTF("PlayerList");
                    Set<UUID> original = Collections.emptySet();
                    type = in.readUTF();
                    if (type.equals("ALL")) {
                        out.writeUTF("ALL");
                        original = plugin.getPlayers();
                    } else {
                        try {
                            original = plugin.getAbstractRedisBungeeApi().getPlayersOnServer(type);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    Set<String> players = original.stream()
                            .map(uuid -> plugin.getUuidTranslator().getNameFromUuid(uuid, false))
                            .collect(Collectors.toSet());
                    out.writeUTF(Joiner.on(',').join(players));
                    break;
                case "PlayerCount":
                    out.writeUTF("PlayerCount");
                    type = in.readUTF();
                    if (type.equals("ALL")) {
                        out.writeUTF("ALL");
                        out.writeInt(plugin.getCount());
                    } else {
                        out.writeUTF(type);
                        try {
                            out.writeInt(plugin.getAbstractRedisBungeeApi().getPlayersOnServer(type).size());
                        } catch (IllegalArgumentException e) {
                            out.writeInt(0);
                        }
                    }
                    break;
                case "LastOnline":
                    String user = in.readUTF();
                    out.writeUTF("LastOnline");
                    out.writeUTF(user);
                    out.writeLong(plugin.getAbstractRedisBungeeApi().getLastOnline(Objects.requireNonNull(plugin.getUuidTranslator().getTranslatedUuid(user, true))));
                    break;
                case "ServerPlayers":
                    String type1 = in.readUTF();
                    out.writeUTF("ServerPlayers");
                    Multimap<String, UUID> multimap = plugin.getAbstractRedisBungeeApi().getServerToPlayers();

                    boolean includesUsers;

                    switch (type1) {
                        case "COUNT":
                            includesUsers = false;
                            break;
                        case "PLAYERS":
                            includesUsers = true;
                            break;
                        default:
                            // TODO: Should I raise an error?
                            return;
                    }

                    out.writeUTF(type1);

                    if (includesUsers) {
                        Multimap<String, String> human = HashMultimap.create();
                        for (Map.Entry<String, UUID> entry : multimap.entries()) {
                            human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                        }
                        serializeMultimap(human, true, out);
                    } else {
                        serializeMultiset(multimap.keys(), out);
                    }
                    break;
                case "Proxy":
                    out.writeUTF("Proxy");
                    out.writeUTF(plugin.getConfiguration().getProxyId());
                    break;
                case "PlayerProxy":
                    String username = in.readUTF();
                    out.writeUTF("PlayerProxy");
                    out.writeUTF(username);
                    out.writeUTF(plugin.getAbstractRedisBungeeApi().getProxy(Objects.requireNonNull(plugin.getUuidTranslator().getTranslatedUuid(username, true))));
                    break;
                default:
                    return;
            }

            ((ServerConnection) event.getSource()).sendPluginMessage(event.getIdentifier(), out.toByteArray());
        });

    }


    @Override
    @Subscribe
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + plugin.getAbstractRedisBungeeApi().getProxyId())) {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.logInfo("Invoking command via PubSub: /" + message);
            ((RedisBungeeVelocityPlugin) plugin).getProxy().getCommandManager().executeAsync(RedisBungeeCommandSource.getSingleton(), message);

        }
    }
}
