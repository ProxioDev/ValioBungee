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
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerPing;

import java.util.*;
import java.util.stream.Collectors;

import static com.imaginarycode.minecraft.redisbungee.api.util.serialize.MultiMapSerialization.serializeMultimap;
import static com.imaginarycode.minecraft.redisbungee.api.util.serialize.MultiMapSerialization.serializeMultiset;

public class RedisBungeeListener {

    private final RedisBungeePlugin<Player> plugin;

    public RedisBungeeListener(RedisBungeePlugin<Player> plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.LAST) // some plugins changes it online players so we need to be executed as last
    public void onPing(ProxyPingEvent event) {
        if (plugin.configuration().getExemptAddresses().contains(event.getConnection().getRemoteAddress().getAddress())) {
            return;
        }
        ServerPing.Builder ping = event.getPing().asBuilder();
        ping.onlinePlayers(plugin.proxyDataManager().totalNetworkPlayers());
        event.setPing(ping.build());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection) || !RedisBungeeVelocityPlugin.IDENTIFIERS.contains(event.getIdentifier())) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

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
                        original = plugin.proxyDataManager().networkPlayers();
                    } else {
                        out.writeUTF(type);
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
                        out.writeInt(plugin.proxyDataManager().totalNetworkPlayers());
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
                        case "COUNT" -> includesUsers = false;
                        case "PLAYERS" -> includesUsers = true;
                        default -> {
                            // TODO: Should I raise an error?
                            return;
                        }
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
                    out.writeUTF(plugin.configuration().getProxyId());
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


}
