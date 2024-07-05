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
import com.imaginarycode.minecraft.redisbungee.api.config.HandleMotdOrder;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.*;

import static com.imaginarycode.minecraft.redisbungee.api.util.serialize.MultiMapSerialization.*;

public class RedisBungeeListener implements Listener {

    private final RedisBungeePlugin<ProxiedPlayer> plugin;

    public RedisBungeeListener(RedisBungeePlugin<ProxiedPlayer> plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPingFirst(ProxyPingEvent event) {
        if (plugin.configuration().handleMotdOrder() != HandleMotdOrder.FIRST) {
            return;
        }
        onPing0(event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPingNormal(ProxyPingEvent event) {
        if (plugin.configuration().handleMotdOrder() != HandleMotdOrder.NORMAL) {
            return;
        }
        onPing0(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPingLast(ProxyPingEvent event) {
        if (plugin.configuration().handleMotdOrder() != HandleMotdOrder.LAST) {
            return;
        }
        onPing0(event);
    }

    private void onPing0(ProxyPingEvent event) {
        if (!plugin.configuration().handleMotd()) return;
        if (plugin.configuration().getExemptAddresses().contains(event.getConnection().getAddress().getAddress())) return;
        ServerInfo forced = AbstractReconnectHandler.getForcedHost(event.getConnection());

        if (forced != null && event.getConnection().getListener().isPingPassthrough()) return;
        event.getResponse().getPlayers().setOnline(plugin.proxyDataManager().totalNetworkPlayers());
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if ((event.getTag().equals("legacy:redisbungee") || event.getTag().equals("RedisBungee")) && event.getSender() instanceof Server) {
            final String currentChannel = event.getTag();
            final byte[] data = Arrays.copyOf(event.getData(), event.getData().length);
            plugin.executeAsync(() -> {
                ByteArrayDataInput in = ByteStreams.newDataInput(data);

                String subchannel = in.readUTF();
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                String type;

                switch (subchannel) {
                    case "PlayerList" -> {
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
                        Set<String> players = new HashSet<>();
                        for (UUID uuid : original)
                            players.add(plugin.getUuidTranslator().getNameFromUuid(uuid, false));
                        out.writeUTF(Joiner.on(',').join(players));
                    }
                    case "PlayerCount" -> {
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
                    }
                    case "LastOnline" -> {
                        String user = in.readUTF();
                        out.writeUTF("LastOnline");
                        out.writeUTF(user);
                        out.writeLong(plugin.getAbstractRedisBungeeApi().getLastOnline(Objects.requireNonNull(plugin.getUuidTranslator().getTranslatedUuid(user, true))));
                    }
                    case "ServerPlayers" -> {
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
                    }
                    case "Proxy" -> {
                        out.writeUTF("Proxy");
                        out.writeUTF(plugin.configuration().getProxyId());
                    }
                    case "PlayerProxy" -> {
                        String username = in.readUTF();
                        out.writeUTF("PlayerProxy");
                        out.writeUTF(username);
                        out.writeUTF(plugin.getAbstractRedisBungeeApi().getProxy(Objects.requireNonNull(plugin.getUuidTranslator().getTranslatedUuid(username, true))));
                    }
                    default -> {
                        return;
                    }
                }

                ((Server) event.getSender()).sendData(currentChannel, out.toByteArray());
            });
        }
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY && plugin.configuration().handleReconnectToLastServer()) {
            ProxiedPlayer player = event.getPlayer();
            String lastServer = plugin.playerDataManager().getLastServerFor(event.getPlayer().getUniqueId());
            if (lastServer == null) return;
            player.sendMessage(BungeeComponentSerializer.get().serialize(plugin.langConfiguration().messages().serverConnecting(player.getLocale(), lastServer)));
            ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(lastServer);
            if (serverInfo == null) {
                player.sendMessage(BungeeComponentSerializer.get().serialize(plugin.langConfiguration().messages().serverNotFound(player.getLocale(), lastServer)));
                return;
            }
            event.setTarget(serverInfo);
        }
    }
}
