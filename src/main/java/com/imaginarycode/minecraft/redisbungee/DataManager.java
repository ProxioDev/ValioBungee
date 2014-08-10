/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This class manages all the data that RedisBungee fetches from Redis, along with updates to that data.
 *
 * @since 0.3.3
 */
@RequiredArgsConstructor
public class DataManager implements Listener {
    private final RedisBungee plugin;

    /*
     * Caches of player data in Redis.
     *
     * Most of these are purged only based on size limits but are also invalidated on certain actions.
     */
    private final Cache<UUID, String> serverCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(2000)
            .concurrencyLevel(2)
            .build();

    private final Cache<UUID, String> proxyCache = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .concurrencyLevel(2)
            .build();

    private final Cache<UUID, InetAddress> ipCache = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .concurrencyLevel(2)
            .build();

    private final Cache<UUID, Long> lastOnlineCache = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .concurrencyLevel(2)
            .build();

    static final UUID source = UUID.randomUUID();

    public String getServer(UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return player.getServer() != null ? player.getServer().getInfo().getName() : null;

        String server = serverCache.getIfPresent(uuid);

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

        String server = proxyCache.getIfPresent(uuid);

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

        InetAddress address = ipCache.getIfPresent(uuid);

        if (address != null)
            return address;

        Jedis tmpRsc = plugin.getPool().getResource();
        try {
            String result = tmpRsc.hget("player:" + uuid, "ip");
            if (result != null) {
                address = InetAddress.getByName(result);

                if (address == null)
                    return null;

                ipCache.put(uuid, address);
                return address;
            }
            return null;
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            plugin.getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
            plugin.getPool().returnBrokenResource(tmpRsc);
            throw new RuntimeException("Unable to get server for " + uuid, e);
        } catch (UnknownHostException e) {
            return null;
        } finally {
            plugin.getPool().returnResource(tmpRsc);
        }
    }

    public long getLastOnline(UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return 0;

        Long time = lastOnlineCache.getIfPresent(uuid);

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
            plugin.getPool().returnBrokenResource(tmpRsc);
            throw new RuntimeException("Unable to get server for " + uuid, e);
        } finally {
            plugin.getPool().returnResource(tmpRsc);
        }
    }

    private void invalidate(UUID uuid) {
        ipCache.invalidate(uuid);
        lastOnlineCache.invalidate(uuid);
        serverCache.invalidate(uuid);
        proxyCache.invalidate(uuid);
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

        DataManagerMessage message = RedisBungee.getGson().fromJson(event.getMessage(), DataManagerMessage.class);

        if (message.getSource().equals(source))
            return;

        // For now we will just invalidate the caches. In a future version the action scope will be expanded ;)
        invalidate(message.getTarget());
    }

    @Getter
    @RequiredArgsConstructor
    static class DataManagerMessage {
        enum Action {
            JOIN,
            LEAVE
        }

        private final UUID target;
        private final UUID source = DataManager.source;
        private final Action action; // for future use!
    }
}
