/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.net.InetAddresses;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * This class manages all the data that RedisBungee fetches from Redis, along with updates to that data.
 *
 * @since 0.3.3
 */
@RequiredArgsConstructor
public class DataManager implements Listener {
    private final RedisBungee plugin;
    private final ConcurrentMap<UUID, String> serverCache = new ConcurrentHashMap<>(192, 0.65f, 4);
    private final ConcurrentMap<UUID, String> proxyCache = new ConcurrentHashMap<>(192, 0.65f, 4);
    private final ConcurrentMap<UUID, InetAddress> ipCache = new ConcurrentHashMap<>(192, 0.65f, 4);
    private final ConcurrentMap<UUID, Long> lastOnlineCache = new ConcurrentHashMap<>(192, 0.65f, 4);

    private final JsonParser parser = new JsonParser();

    public String getServer(UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return player.getServer() != null ? player.getServer().getInfo().getName() : null;

        String server = serverCache.get(uuid);

        if (server != null)
            return server;

        Jedis tmpRsc = plugin.getPool().getResource();
        try {
            server = tmpRsc.hget("player:" + uuid, "server");

            if (server == null)
                return null;

            serverCache.put(uuid, server);
            return server;
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            plugin.getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
            if (tmpRsc != null)
                plugin.getPool().returnBrokenResource(tmpRsc);
            throw new RuntimeException("Unable to get server for " + uuid, e);
        } finally {
            plugin.getPool().returnResource(tmpRsc);
        }
    }

    public String getProxy(UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return plugin.getServerId();

        String server = proxyCache.get(uuid);

        if (server != null)
            return server;

        Jedis tmpRsc = plugin.getPool().getResource();
        try {
            server = tmpRsc.hget("player:" + uuid, "proxy");

            if (server == null)
                return null;

            proxyCache.put(uuid, server);
            return server;
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            plugin.getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
            if (tmpRsc != null)
                plugin.getPool().returnBrokenResource(tmpRsc);
            throw new RuntimeException("Unable to get server for " + uuid, e);
        } finally {
            plugin.getPool().returnResource(tmpRsc);
        }
    }

    public InetAddress getIp(UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return player.getAddress().getAddress();

        InetAddress address = ipCache.get(uuid);

        if (address != null)
            return address;

        Jedis tmpRsc = plugin.getPool().getResource();
        try {
            String result = tmpRsc.hget("player:" + uuid, "ip");
            if (result != null) {
                address = InetAddresses.forString(result);
                ipCache.put(uuid, address);
                return address;
            }
            return null;
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            plugin.getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
            if (tmpRsc != null)
                plugin.getPool().returnBrokenResource(tmpRsc);
            throw new RuntimeException("Unable to get server for " + uuid, e);
        } finally {
            plugin.getPool().returnResource(tmpRsc);
        }
    }

    public long getLastOnline(UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return 0;

        Long time = lastOnlineCache.get(uuid);

        if (time != null)
            return time;

        Jedis tmpRsc = plugin.getPool().getResource();
        try {
            String result = tmpRsc.hget("player:" + uuid, "online");
            if (result != null)
                try {
                    time = Long.valueOf(result);

                    if (time == null)
                        return -1;

                    lastOnlineCache.put(uuid, time);
                    return time;
                } catch (NumberFormatException e) {
                    plugin.getLogger().info("I found a funny number for when " + uuid + " was last online!");
                    boolean found = false;
                    for (String proxyId : plugin.getServerIds()) {
                        if (proxyId.equals(plugin.getServerId())) continue;
                        if (tmpRsc.sismember("proxy:" + proxyId + ":usersOnline", uuid.toString())) {
                            found = true;
                            break;
                        }
                    }

                    long value = 0;

                    if (!found) {
                        value = System.currentTimeMillis();
                        plugin.getLogger().info(uuid + " isn't online. Setting to current time.");
                    } else {
                        plugin.getLogger().info(uuid + " is online. Setting to 0. Please check your BungeeCord instances.");
                        plugin.getLogger().info("If they are working properly, and this error does not resolve in a few minutes, please let Tux know!");
                    }
                    tmpRsc.hset("player:" + uuid, "online", Long.toString(value));
                    return value;
                }
            return (long) -1;
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            plugin.getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
            if (tmpRsc != null)
                plugin.getPool().returnBrokenResource(tmpRsc);
            throw new RuntimeException("Unable to get server for " + uuid, e);
        } finally {
            plugin.getPool().returnResource(tmpRsc);
        }
    }

    private void invalidate(UUID uuid) {
        ipCache.remove(uuid);
        lastOnlineCache.remove(uuid);
        serverCache.remove(uuid);
        proxyCache.remove(uuid);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        // Invalidate all entries related to this player, since they now lie.
        invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // Invalidate all entries related to this player, since they now lie.
        invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (!event.getChannel().equals("redisbungee-data"))
            return;

        // Partially deserialize the message so we can look at the action
        JsonObject jsonObject = parser.parse(event.getMessage()).getAsJsonObject();

        String source = jsonObject.get("source").getAsString();

        if (source.equals(plugin.getServerId()))
            return;

        DataManagerMessage.Action action = DataManagerMessage.Action.valueOf(jsonObject.get("action").getAsString());

        switch (action) {
            case JOIN:
                final DataManagerMessage<LoginPayload> message1 = RedisBungee.getGson().fromJson(jsonObject, new TypeToken<DataManagerMessage<LoginPayload>>() {
                }.getType());
                proxyCache.put(message1.getTarget(), message1.getSource());
                lastOnlineCache.put(message1.getTarget(), (long) 0);
                ipCache.put(message1.getTarget(), message1.getPayload().getAddress());
                plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.getProxy().getPluginManager().callEvent(new PlayerJoinedNetworkEvent(message1.getTarget()));
                    }
                });
                break;
            case LEAVE:
                final DataManagerMessage<LogoutPayload> message2 = RedisBungee.getGson().fromJson(jsonObject, new TypeToken<DataManagerMessage<LogoutPayload>>() {
                }.getType());
                invalidate(message2.getTarget());
                lastOnlineCache.put(message2.getTarget(), message2.getPayload().getTimestamp());
                plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.getProxy().getPluginManager().callEvent(new PlayerLeftNetworkEvent(message2.getTarget()));
                    }
                });
                break;
            case SERVER_CHANGE:
                final DataManagerMessage<ServerChangePayload> message3 = RedisBungee.getGson().fromJson(jsonObject, new TypeToken<DataManagerMessage<ServerChangePayload>>()
                {
                }.getType());
                final String oldServer = serverCache.put(message3.getTarget(), message3.getPayload().getServer());
                plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.getProxy().getPluginManager().callEvent(new PlayerChangedServerNetworkEvent(message3.getTarget(), oldServer, message3.getPayload().getServer()));
                    }
                });
                break;
        }
    }

    @Getter
    @RequiredArgsConstructor
    static class DataManagerMessage<T> {
        private final UUID target;
        private final String source = RedisBungee.getApi().getServerId();
        private final Action action; // for future use!
        private final T payload;
        enum Action {
            JOIN,
            LEAVE,
            SERVER_CHANGE
        }
    }

    @Getter
    @RequiredArgsConstructor
    static class LoginPayload {
        private final InetAddress address;
    }

    @Getter
    @RequiredArgsConstructor
    static class ServerChangePayload {
        private final String server;
    }

    @Getter
    @RequiredArgsConstructor
    static class LogoutPayload {
        private final long timestamp;
    }
}
