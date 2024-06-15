/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.InetAddresses;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisPipelineTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.json.JSONObject;
import redis.clients.jedis.ClusterPipeline;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.UnifiedJedis;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class PlayerDataManager<P, LE, DE, PS extends IPubSubMessageEvent, SC extends IPlayerChangedServerNetworkEvent, NJE extends IPlayerLeftNetworkEvent, CE> {

    protected final RedisBungeePlugin<P> plugin;
    private final Object SERVERS_TO_PLAYERS_KEY = new Object();
    private final UnifiedJedis unifiedJedis;
    private final String proxyId;
    private final String networkId;
    private final LoadingCache<UUID, String> serverCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(this::getServerFromRedis);
    private final LoadingCache<UUID, String> lastServerCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(this::getLastServerFromRedis);
    private final LoadingCache<UUID, String> proxyCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(this::getProxyFromRedis);
    private final LoadingCache<UUID, InetAddress> ipCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(this::getIpAddressFromRedis);
    private final LoadingCache<Object, Multimap<String, UUID>> serverToPlayersCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(this::serversToPlayersBuilder);
    private final JSONComponentSerializer COMPONENT_SERIALIZER = JSONComponentSerializer.json();

    public PlayerDataManager(RedisBungeePlugin<P> plugin) {
        this.plugin = plugin;
        this.unifiedJedis = plugin.proxyDataManager().unifiedJedis();
        this.proxyId = plugin.proxyDataManager().proxyId();
        this.networkId = plugin.proxyDataManager().networkId();
    }

    // handle network wide
    // server change
    public abstract void onPlayerChangedServerNetworkEvent(SC event);

    public abstract void onNetworkPlayerQuit(NJE event);

    // local events
    public abstract void onPubSubMessageEvent(PS event);

    public abstract void onServerConnectedEvent(CE event);

    public abstract void onLoginEvent(LE event);

    public abstract void onDisconnectEvent(DE event);

    protected void handleNetworkPlayerServerChange(IPlayerChangedServerNetworkEvent event) {
        this.serverCache.invalidate(event.getUuid());
        this.lastServerCache.invalidate(event.getUuid());

        //TODO: We could also rely on redisbungee-serverchange pubsub messages to update the cache in-place without querying redis. That would be a lot more efficient.
        this.serverToPlayersCache.invalidate(SERVERS_TO_PLAYERS_KEY);
    }

    protected void handleNetworkPlayerQuit(IPlayerLeftNetworkEvent event) {
        this.proxyCache.invalidate(event.getUuid());
        this.serverCache.invalidate(event.getUuid());
        this.ipCache.invalidate(event.getUuid());

        //TODO: We could also rely on redisbungee-serverchange pubsub messages to update the cache in-place without querying redis. That would be a lot more efficient.
        this.serverToPlayersCache.invalidate(SERVERS_TO_PLAYERS_KEY);
    }

    protected void handlePubSubMessageEvent(IPubSubMessageEvent event) {
        // kick api
        if (event.getChannel().equals("redisbungee-kick")) {
            JSONObject data = new JSONObject(event.getMessage());
            String proxy = data.getString("proxy");
            if (proxy.equals(this.proxyId)) {
                return;
            }
            UUID uuid = UUID.fromString(data.getString("uuid"));
            String message = data.getString("message");
            plugin.handlePlatformKick(uuid, COMPONENT_SERIALIZER.deserialize(message));
            return;
        }
        if (event.getChannel().equals("redisbungee-serverchange")) {
            JSONObject data = new JSONObject(event.getMessage());
            String proxy = data.getString("proxy");
            if (proxy.equals(this.proxyId)) {
                return;
            }
            UUID uuid = UUID.fromString(data.getString("uuid"));
            String from = null;
            if (data.has("from")) from = data.getString("from");
            String to = data.getString("to");
            plugin.fireEvent(plugin.createPlayerChangedServerNetworkEvent(uuid, from, to));
            return;
        }
        if (event.getChannel().equals("redisbungee-player-join")) {
            JSONObject data = new JSONObject(event.getMessage());
            String proxy = data.getString("proxy");
            if (proxy.equals(this.proxyId)) {
                return;
            }
            UUID uuid = UUID.fromString(data.getString("uuid"));
            plugin.fireEvent(plugin.createPlayerJoinedNetworkEvent(uuid));
            return;
        }
        if (event.getChannel().equals("redisbungee-player-leave")) {
            JSONObject data = new JSONObject(event.getMessage());
            String proxy = data.getString("proxy");
            if (proxy.equals(this.proxyId)) {
                return;
            }
            UUID uuid = UUID.fromString(data.getString("uuid"));
            plugin.fireEvent(plugin.createPlayerLeftNetworkEvent(uuid));
        }

    }

    protected void playerChangedServer(UUID uuid, String from, String to) {
        JSONObject data = new JSONObject();
        data.put("proxy", this.proxyId);
        data.put("uuid", uuid);
        data.put("from", from);
        data.put("to", to);
        plugin.proxyDataManager().sendChannelMessage("redisbungee-serverchange", data.toString());
        plugin.fireEvent(plugin.createPlayerChangedServerNetworkEvent(uuid, from, to));
        handleServerChangeRedis(uuid, to);
    }

    public void kickPlayer(UUID uuid, Component message) {
        if (!plugin.handlePlatformKick(uuid, message)) { // handle locally before SENDING a message
            JSONObject data = new JSONObject();
            data.put("proxy", this.proxyId);
            data.put("uuid", uuid);
            data.put("message", COMPONENT_SERIALIZER.serialize(message));
            plugin.proxyDataManager().sendChannelMessage("redisbungee-kick", data.toString());
        }
    }

    private void handleServerChangeRedis(UUID uuid, String server) {
        Map<String, String> data = new HashMap<>();
        data.put("server", server);
        data.put("last-server", server);
        unifiedJedis.hset("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", data);
    }

    protected void addPlayer(final UUID uuid, final String name, final InetAddress inetAddress) {
        Map<String, String> redisData = new HashMap<>();
        redisData.put("last-online", String.valueOf(0));
        redisData.put("proxy", this.proxyId);
        redisData.put("ip", inetAddress.getHostAddress());
        unifiedJedis.hset("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", redisData);
        plugin.getUuidTranslator().persistInfo(name, uuid, this.unifiedJedis);
        JSONObject data = new JSONObject();
        data.put("proxy", this.proxyId);
        data.put("uuid", uuid);
        plugin.proxyDataManager().sendChannelMessage("redisbungee-player-join", data.toString());
        plugin.fireEvent(plugin.createPlayerJoinedNetworkEvent(uuid));
        this.plugin.proxyDataManager().addPlayer(uuid);
    }

    protected void removePlayer(UUID uuid) {
        unifiedJedis.hset("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", "last-online", String.valueOf(System.currentTimeMillis()));
        unifiedJedis.hdel("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", "server", "proxy", "ip");
        JSONObject data = new JSONObject();
        data.put("proxy", this.proxyId);
        data.put("uuid", uuid);
        plugin.proxyDataManager().sendChannelMessage("redisbungee-player-leave", data.toString());
        plugin.fireEvent(plugin.createPlayerLeftNetworkEvent(uuid));
        this.plugin.proxyDataManager().removePlayer(uuid);
    }


    protected String getProxyFromRedis(UUID uuid) {
        return unifiedJedis.hget("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", "proxy");
    }

    protected String getServerFromRedis(UUID uuid) {
        return unifiedJedis.hget("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", "server");
    }

    protected String getLastServerFromRedis(UUID uuid) {
        return unifiedJedis.hget("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", "last-server");
    }

    protected InetAddress getIpAddressFromRedis(UUID uuid) {
        String ip = unifiedJedis.hget("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", "ip");
        if (ip == null) return null;
        return InetAddresses.forString(ip);
    }

    protected long getLastOnlineFromRedis(UUID uuid) {
        String unixString = unifiedJedis.hget("redis-bungee::" + this.networkId + "::player::" + uuid + "::data", "last-online");
        if (unixString == null) return -1;
        return Long.parseLong(unixString);
    }

    public String getLastServerFor(UUID uuid) {
        return this.lastServerCache.get(uuid);
    }

    public String getServerFor(UUID uuid) {
        return this.serverCache.get(uuid);
    }

    public String getProxyFor(UUID uuid) {
        return this.proxyCache.get(uuid);
    }

    public InetAddress getIpFor(UUID uuid) {
        return this.ipCache.get(uuid);
    }

    public long getLastOnline(UUID uuid) {
        return getLastOnlineFromRedis(uuid);
    }

    public Multimap<String, UUID> serversToPlayers() {
        return this.serverToPlayersCache.get(SERVERS_TO_PLAYERS_KEY);
    }

    protected Multimap<String, UUID> serversToPlayersBuilder(Object o) {
        try {
            return new RedisPipelineTask<Multimap<String, UUID>>(plugin) {
                private final Set<UUID> uuids = plugin.proxyDataManager().networkPlayers();
                private final ImmutableMultimap.Builder<String, UUID> builder = ImmutableMultimap.builder();

                @Override
                public Multimap<String, UUID> doPooledPipeline(Pipeline pipeline) {
                    HashMap<UUID, Response<String>> responses = new HashMap<>();
                    for (UUID uuid : uuids) {
                        responses.put(uuid, pipeline.hget("redis-bungee::" + networkId + "::player::" + uuid + "::data", "server"));
                    }
                    pipeline.sync();
                    responses.forEach((uuid, response) -> {
                        String key = response.get();
                        if (key == null) return;

                        builder.put(key, uuid);
                    });
                    return builder.build();
                }

                @Override
                public Multimap<String, UUID> clusterPipeline(ClusterPipeline pipeline) {
                    HashMap<UUID, Response<String>> responses = new HashMap<>();
                    for (UUID uuid : uuids) {
                        responses.put(uuid, pipeline.hget("redis-bungee::" + networkId + "::player::" + uuid + "::data", "server"));
                    }
                    pipeline.sync();
                    responses.forEach((uuid, response) -> {
                        String key = response.get();
                        if (key == null) return;

                        builder.put(key, uuid);
                    });
                    return builder.build();
                }
            }.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
