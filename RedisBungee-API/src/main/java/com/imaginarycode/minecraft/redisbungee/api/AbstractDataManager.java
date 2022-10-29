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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.InetAddresses;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import redis.clients.jedis.UnifiedJedis;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class manages all the data that RedisBungee fetches from Redis, along with updates to that data.
 *
 * @since 0.3.3
 */
public abstract class AbstractDataManager<P, PL, PD, PS> {
    protected final RedisBungeePlugin<P> plugin;
    private final Cache<UUID, String> serverCache = createCache();
    private final Cache<UUID, String> proxyCache = createCache();
    private final Cache<UUID, InetAddress> ipCache = createCache();
    private final Cache<UUID, Long> lastOnlineCache = createCache();
    private final Gson gson = new Gson();

    public AbstractDataManager(RedisBungeePlugin<P> plugin) {
        this.plugin = plugin;
    }

    private static <K, V> Cache<K, V> createCache() {
        // TODO: Allow customization via cache specification, ala ServerListPlus
        return CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    public String getServer(final UUID uuid) {
        P player = plugin.getPlayer(uuid);

        if (player != null)
            return plugin.isPlayerOnAServer(player) ? plugin.getPlayerServerName(player) : null;

        try {
            return serverCache.get(uuid, new RedisTask<String>(plugin.getAbstractRedisBungeeApi()) {
                @Override
                public String unifiedJedisTask(UnifiedJedis unifiedJedis) {
                    return Objects.requireNonNull(unifiedJedis.hget("player:" + uuid, "server"), "user not found");

                }
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof NullPointerException && e.getCause().getMessage().equals("user not found"))
                return null; // HACK
            plugin.logFatal("Unable to get server");
            throw new RuntimeException("Unable to get server for " + uuid, e);
        }
    }


    public String getProxy(final UUID uuid) {
        P player = plugin.getPlayer(uuid);

        if (player != null)
            return plugin.getConfiguration().getProxyId();

        try {
            return proxyCache.get(uuid, new RedisTask<String>(plugin.getAbstractRedisBungeeApi()) {
                @Override
                public String unifiedJedisTask(UnifiedJedis unifiedJedis) {
                    return Objects.requireNonNull(unifiedJedis.hget("player:" + uuid, "proxy"), "user not found");
                }
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof NullPointerException && e.getCause().getMessage().equals("user not found"))
                return null; // HACK
            plugin.logFatal("Unable to get proxy");
            throw new RuntimeException("Unable to get proxy for " + uuid, e);
        }
    }

    public InetAddress getIp(final UUID uuid) {
        P player = plugin.getPlayer(uuid);

        if (player != null)
            return plugin.getPlayerIp(player);

        try {
            return ipCache.get(uuid, new RedisTask<InetAddress>(plugin.getAbstractRedisBungeeApi()) {
                @Override
                public InetAddress unifiedJedisTask(UnifiedJedis unifiedJedis) {
                    String result = unifiedJedis.hget("player:" + uuid, "ip");
                    if (result == null)
                        throw new NullPointerException("user not found");
                    return InetAddresses.forString(result);
                }
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof NullPointerException && e.getCause().getMessage().equals("user not found"))
                return null; // HACK
            plugin.logFatal("Unable to get IP");
            throw new RuntimeException("Unable to get IP for " + uuid, e);
        }
    }

    public long getLastOnline(final UUID uuid) {
        P player = plugin.getPlayer(uuid);

        if (player != null)
            return 0;

        try {
            return lastOnlineCache.get(uuid, new RedisTask<Long>(plugin.getAbstractRedisBungeeApi()) {

                @Override
                public Long unifiedJedisTask(UnifiedJedis unifiedJedis) {
                    String result = unifiedJedis.hget("player:" + uuid, "online");
                    return result == null ? -1 : Long.parseLong(result);
                }
            });
        } catch (ExecutionException e) {
            plugin.logFatal("Unable to get last time online");
            throw new RuntimeException("Unable to get last time online for " + uuid, e);
        }
    }

    protected void invalidate(UUID uuid) {
        ipCache.invalidate(uuid);
        lastOnlineCache.invalidate(uuid);
        serverCache.invalidate(uuid);
        proxyCache.invalidate(uuid);
    }

    // Invalidate all entries related to this player, since they now lie. (call invalidate(uuid))
    public abstract void onPostLogin(PL event);

    // Invalidate all entries related to this player, since they now lie. (call invalidate(uuid))
    public abstract void onPlayerDisconnect(PD event);

    public abstract void onPubSubMessage(PS event);

    public abstract boolean handleKick(UUID target, String message);

    protected void handlePubSubMessage(String channel, String message) {
        if (!channel.equals("redisbungee-data"))
            return;

        // Partially deserialize the message so we can look at the action
        JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();

        final String source = jsonObject.get("source").getAsString();

        if (source.equals(plugin.getConfiguration().getProxyId()))
            return;

        DataManagerMessage.Action action = DataManagerMessage.Action.valueOf(jsonObject.get("action").getAsString());

        switch (action) {
            case JOIN:
                final DataManagerMessage<LoginPayload> message1 = gson.fromJson(jsonObject, new TypeToken<DataManagerMessage<LoginPayload>>() {
                }.getType());
                proxyCache.put(message1.getTarget(), message1.getSource());
                lastOnlineCache.put(message1.getTarget(), (long) 0);
                ipCache.put(message1.getTarget(), message1.getPayload().getAddress());
                plugin.executeAsync(() -> {
                    Object event = plugin.createPlayerJoinedNetworkEvent(message1.getTarget());
                    plugin.fireEvent(event);
                });
                break;
            case LEAVE:
                final DataManagerMessage<LogoutPayload> message2 = gson.fromJson(jsonObject, new TypeToken<DataManagerMessage<LogoutPayload>>() {
                }.getType());
                invalidate(message2.getTarget());
                lastOnlineCache.put(message2.getTarget(), message2.getPayload().getTimestamp());
                plugin.executeAsync(() -> {
                    Object event = plugin.createPlayerLeftNetworkEvent(message2.getTarget());
                    plugin.fireEvent(event);
                });
                break;
            case SERVER_CHANGE:
                final DataManagerMessage<ServerChangePayload> message3 = gson.fromJson(jsonObject, new TypeToken<DataManagerMessage<ServerChangePayload>>() {
                }.getType());
                serverCache.put(message3.getTarget(), message3.getPayload().getServer());
                plugin.executeAsync(() -> {
                    Object event = plugin.createPlayerChangedServerNetworkEvent(message3.getTarget(), message3.getPayload().getOldServer(), message3.getPayload().getServer());
                    plugin.fireEvent(event);
                });
                break;
            case KICK:
                final DataManagerMessage<KickPayload> kickPayload = gson.fromJson(jsonObject, new TypeToken<DataManagerMessage<KickPayload>>() {
                }.getType());
                plugin.executeAsync(() -> handleKick(kickPayload.target, kickPayload.payload.message));
                break;

        }
    }

    public static class DataManagerMessage<T extends Payload> {
        private final UUID target;
        private final String source;
        private final Action action; // for future use!
        private final T payload;

        public DataManagerMessage(UUID target, String source, Action action, T payload) {
            this.target = target;
            this.source = source;
            this.action = action;
            this.payload = payload;
        }

        public UUID getTarget() {
            return target;
        }

        public String getSource() {
            return source;
        }

        public Action getAction() {
            return action;
        }

        public T getPayload() {
            return payload;
        }

        public enum Action {
            JOIN,
            LEAVE,
            KICK,
            SERVER_CHANGE
        }
    }

    public static abstract class Payload {
    }

    public static class KickPayload extends Payload {

        private final String message;

        public KickPayload(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class LoginPayload extends Payload {
        private final InetAddress address;

        public LoginPayload(InetAddress address) {
            this.address = address;
        }

        public InetAddress getAddress() {
            return address;
        }
    }

    public static class ServerChangePayload extends Payload {
        private final String server;
        private final String oldServer;

        public ServerChangePayload(String server, String oldServer) {
            this.server = server;
            this.oldServer = oldServer;
        }

        public String getServer() {
            return server;
        }

        public String getOldServer() {
            return oldServer;
        }
    }


    public static class LogoutPayload extends Payload {
        private final long timestamp;

        public LogoutPayload(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}