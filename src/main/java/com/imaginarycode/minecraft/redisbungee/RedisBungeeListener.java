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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;

import java.util.*;

@AllArgsConstructor
public class RedisBungeeListener implements Listener {
    private final RedisBungee plugin;

    @EventHandler
    public void onPlayerConnect(final PostLoginEvent event) {
        Jedis rsc = plugin.getPool().getResource();
        try {
            for (String server : plugin.getServerIds()) {
                if (rsc.sismember("proxy:" + server + ":usersOnline", event.getPlayer().getUniqueId().toString())) {
                    event.getPlayer().disconnect(new ComponentBuilder("You are already logged on to this server.").color(
                            ChatColor.RED).create());
                    return;
                }
            }
        } finally {
            plugin.getPool().returnResource(rsc);
        }
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
        reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), plugin.getCount(), old.getPlayers().getSample()));
        reply.setDescription(old.getDescription());
        reply.setFavicon(old.getFaviconObject());
        reply.setVersion(old.getVersion());
        event.setResponse(reply);
    }

    @EventHandler
    public void onPluginMessage(final PluginMessageEvent event) {
        if (event.getTag().equals("RedisBungee") && event.getSender() instanceof Server) {
            final byte[] data = Arrays.copyOf(event.getData(), event.getData().length);
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    ByteArrayDataInput in = ByteStreams.newDataInput(data);

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
                                    original = plugin.getPlayersOnServer(type);
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                            Set<String> players = new HashSet<>();
                            for (UUID uuid : original)
                                players.add(plugin.getUuidTranslator().getNameFromUuid(uuid, false));
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
                            break;
                        case "LastOnline":
                            String user = in.readUTF();
                            out.writeUTF("LastOnline");
                            out.writeUTF(user);
                            out.writeLong(RedisBungee.getApi().getLastOnline(plugin.getUuidTranslator().getTranslatedUuid(user, true)));
                            break;
                        default:
                            break;
                    }

                    ((Server) event.getSender()).sendData("RedisBungee", out.toByteArray());
                }
            });
        }
    }

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + RedisBungee.getApi().getServerId())) {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.getLogger().info("Invoking command via PubSub: /" + message);
            plugin.getProxy().getPluginManager().dispatchCommand(RedisBungeeCommandSender.instance, message);
        }
    }
}
