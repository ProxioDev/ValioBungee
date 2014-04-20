/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.imaginarycode.minecraft.redisbungee.consumerevents.PlayerChangedServerConsumerEvent;
import com.imaginarycode.minecraft.redisbungee.consumerevents.PlayerLoggedInConsumerEvent;
import com.imaginarycode.minecraft.redisbungee.consumerevents.PlayerLoggedOffConsumerEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
public class RedisBungeeListener implements Listener {
    private final RedisBungee plugin;

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (plugin.getPool() != null) {
            Jedis rsc = plugin.getPool().getResource();
            try {
                for (String server : plugin.getServerIds()) {
                    if (rsc.sismember("server:" + server + ":usersOnline", event.getConnection().getName())) {
                        event.setCancelled(true);
                        event.setCancelReason("You are already logged on to this server.");
                        break;
                    }
                }
            } finally {
                plugin.getPool().returnResource(rsc);
            }
        }
    }

    @EventHandler
    public void onPlayerConnect(final PostLoginEvent event) {
        plugin.getConsumer().queue(new PlayerLoggedInConsumerEvent(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerDisconnect(final PlayerDisconnectEvent event) {
        plugin.getConsumer().queue(new PlayerLoggedOffConsumerEvent(event.getPlayer()));
    }

    @EventHandler
    public void onServerChange(final ServerConnectedEvent event) {
        plugin.getConsumer().queue(new PlayerChangedServerConsumerEvent(event.getPlayer(), event.getServer().getInfo()));
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing old = event.getResponse();
        ServerPing reply = new ServerPing();
        if (RedisBungee.getConfiguration().getBoolean("player-list-in-ping", false)) {
            Set<UUID> players = plugin.getPlayers();
            ServerPing.PlayerInfo[] info = new ServerPing.PlayerInfo[players.size()];
            int idx = 0;
            for (UUID player : players) {
                info[idx] = new ServerPing.PlayerInfo(plugin.getUuidTranslator().getNameFromUuid(player), "");
                idx++;
            }
            reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), players.size(), info));
        } else {
            reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), plugin.getCount(), null));
        }
        reply.setDescription(old.getDescription());
        reply.setFavicon(old.getFaviconObject());
        reply.setVersion(old.getVersion());
        event.setResponse(reply);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equals("RedisBungee") && event.getSender() instanceof Server) {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());

            String subchannel = in.readUTF();
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            String type;

            switch (subchannel) {
                case "PlayerList":
                    out.writeUTF("Players");
                    Set<UUID> original = Collections.emptySet();
                    type = in.readUTF();
                    if (type.equals("ALL")) {
                        out.writeUTF("ALL");
                        original = plugin.getPlayers();
                    } else {
                        try {
                            original = plugin.getPlayersOnServer(type);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    Set<String> players = new HashSet<>();
                    for (UUID uuid : original)
                        players.add(plugin.getUuidTranslator().getNameFromUuid(uuid));
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
                            out.writeInt(plugin.getPlayersOnServer(type).size());
                        } catch (IllegalArgumentException e) {
                            out.writeInt(0);
                        }
                    }
                    out.writeInt(plugin.getCurrentCount());
                    break;
                case "LastOnline":
                    String user = in.readUTF();
                    out.writeUTF("LastOnline");
                    out.writeUTF(user);
                    out.writeLong(plugin.getLastOnline(plugin.getUuidTranslator().getTranslatedUuid(user)));
                    break;
                default:
                    break;
            }

            ((Server) event.getSender()).sendData("RedisBungee", out.toByteArray());
        }
    }

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + RedisBungee.getConfiguration().getString("server-id"))) {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.getLogger().info("Invoking command via PubSub: /" + message);
            plugin.getProxy().getPluginManager().dispatchCommand(RedisBungeeCommandSender.instance, message);
        }
    }
}
