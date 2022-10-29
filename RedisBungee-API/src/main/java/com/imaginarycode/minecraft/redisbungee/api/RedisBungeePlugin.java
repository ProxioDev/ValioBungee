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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.redisbungee.AbstractRedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.events.EventsPlatform;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import com.imaginarycode.minecraft.redisbungee.api.util.RedisUtil;
import com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDTranslator;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * This Class has all internal methods needed by every redis bungee plugin, and it can be used to implement another platforms than bungeecord or another forks of RedisBungee
 *
 * @author Ham1255
 * @since 0.7.0
 */
public interface RedisBungeePlugin<P> extends EventsPlatform {

    default void initialize() {

    }

    default void stop() {

    }

    Summoner<?> getSummoner();

    RedisBungeeConfiguration getConfiguration();

    int getCount();

    default int getCurrentCount() {
        return new RedisTask<Long>(this) {
            @Override
            public Long unifiedJedisTask(UnifiedJedis unifiedJedis) {
                long total = 0;
                long redisTime = getRedisTime(unifiedJedis);
                Map<String, String> heartBeats = unifiedJedis.hgetAll("heartbeats");
                for (Map.Entry<String, String> stringStringEntry : heartBeats.entrySet()) {
                    String k = stringStringEntry.getKey();
                    String v = stringStringEntry.getValue();

                    long heartbeatTime = Long.parseLong(v);
                    if (heartbeatTime + 30 >= redisTime) {
                        total = total + unifiedJedis.scard("proxy:" + k + ":usersOnline");
                    }
                }
                return total;
            }
        }.execute().intValue();
    }

    Set<String> getLocalPlayersAsUuidStrings();

    AbstractDataManager<P, ?, ?, ?> getDataManager();

    default Set<UUID> getPlayers() {
        return new RedisTask<Set<UUID>>(this) {
            @Override
            public Set<UUID> unifiedJedisTask(UnifiedJedis unifiedJedis) {
                ImmutableSet.Builder<UUID> setBuilder = ImmutableSet.builder();
                try {
                    List<String> keys = new ArrayList<>();
                    for (String i : getProxiesIds()) {
                        keys.add("proxy:" + i + ":usersOnline");
                    }
                    if (!keys.isEmpty()) {
                        Set<String> users = unifiedJedis.sunion(keys.toArray(new String[0]));
                        if (users != null && !users.isEmpty()) {
                            for (String user : users) {
                                try {
                                    setBuilder = setBuilder.add(UUID.fromString(user));
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }
                    }
                } catch (JedisConnectionException e) {
                    // Redis server has disappeared!
                    logFatal("Unable to get connection from pool - did your Redis server go away?");
                    throw new RuntimeException("Unable to get all players online", e);
                }
                return setBuilder.build();
            }
        }.execute();
    }

    AbstractRedisBungeeAPI getAbstractRedisBungeeApi();

    UUIDTranslator getUuidTranslator();

    Multimap<String, UUID> serverToPlayersCache();

    default Multimap<String, UUID> serversToPlayers() {
        return new RedisTask<Multimap<String, UUID>>(this) {
            @Override
            public Multimap<String, UUID> unifiedJedisTask(UnifiedJedis unifiedJedis) {
                ImmutableMultimap.Builder<String, UUID> builder = ImmutableMultimap.builder();
                for (String serverId : getProxiesIds()) {
                    Set<String> players = unifiedJedis.smembers("proxy:" + serverId + ":usersOnline");
                    for (String player : players) {
                        String playerServer = unifiedJedis.hget("player:" + player, "server");
                        if (playerServer == null) {
                            continue;
                        }
                        builder.put(playerServer, UUID.fromString(player));
                    }
                }
                return builder.build();
            }
        }.execute();
    }

    default Set<UUID> getPlayersOnProxy(String proxyId) {
        checkArgument(getProxiesIds().contains(proxyId), proxyId + " is not a valid proxy ID");
        return new RedisTask<Set<UUID>>(this) {
            @Override
            public Set<UUID> unifiedJedisTask(UnifiedJedis unifiedJedis) {
                Set<String> users = unifiedJedis.smembers("proxy:" + proxyId + ":usersOnline");
                ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();
                for (String user : users) {
                    builder.add(UUID.fromString(user));
                }
                return builder.build();
            }
        }.execute();
    }

    default void sendProxyCommand(String proxyId, String command) {
        checkArgument(getProxiesIds().contains(proxyId) || proxyId.equals("allservers"), "proxyId is invalid");
        sendChannelMessage("redisbungee-" + proxyId, command);
    }

    List<String> getProxiesIds();

    default List<String> getCurrentProxiesIds(boolean lagged) {
        return new RedisTask<List<String>>(this) {
            @Override
            public List<String> unifiedJedisTask(UnifiedJedis unifiedJedis) {
                try {
                    long time = getRedisTime(unifiedJedis);
                    ImmutableList.Builder<String> servers = ImmutableList.builder();
                    Map<String, String> heartbeats = unifiedJedis.hgetAll("heartbeats");
                    for (Map.Entry<String, String> entry : heartbeats.entrySet()) {
                        try {
                            long stamp = Long.parseLong(entry.getValue());
                            if (lagged ? time >= stamp + RedisUtil.PROXY_TIMEOUT : time <= stamp + RedisUtil.PROXY_TIMEOUT) {
                                servers.add(entry.getKey());
                            } else if (time > stamp + RedisUtil.PROXY_TIMEOUT) {
                                logWarn(entry.getKey() + " is " + (time - stamp) + " seconds behind! (Time not synchronized or server down?) and was removed from heartbeat.");
                                unifiedJedis.hdel("heartbeats", entry.getKey());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    return servers.build();
                } catch (JedisConnectionException e) {
                    logFatal("Unable to fetch server IDs");
                    e.printStackTrace();
                    return Collections.singletonList(getConfiguration().getProxyId());
                }
            }
        }.execute();
    }

    PubSubListener getPubSubListener();

    default void sendChannelMessage(String channel, String message) {
        new RedisTask<Void>(this) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                try {
                    unifiedJedis.publish(channel, message);
                } catch (JedisConnectionException e) {
                    // Redis server has disappeared!
                    logFatal("Unable to get connection from pool - did your Redis server go away?");
                    throw new RuntimeException("Unable to publish channel message", e);
                }
                return null;
            }
        }.execute();
    }

    void executeAsync(Runnable runnable);

    void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int time);

    boolean isOnlineMode();

    void logInfo(String msg);

    void logWarn(String msg);

    void logFatal(String msg);

    P getPlayer(UUID uuid);

    P getPlayer(String name);

    UUID getPlayerUUID(String player);

    String getPlayerName(UUID player);

    String getPlayerServerName(P player);

    boolean isPlayerOnAServer(P player);

    InetAddress getPlayerIp(P player);

    default void sendProxyCommand(String cmd) {
        sendProxyCommand(getConfiguration().getProxyId(), cmd);
    }

    default Long getRedisTime(UnifiedJedis unifiedJedis) {
        List<Object> data = (List<Object>) unifiedJedis.sendCommand(Protocol.Command.TIME);
        List<String> times = new ArrayList<>();
        data.forEach((o) -> times.add(new String((byte[])o)));
        return getRedisTime(times);
    }
    default long getRedisTime(List<String> timeRes) {
        return Long.parseLong(timeRes.get(0));
    }

    default void kickPlayer(UUID playerUniqueId, String message) {
        // first handle on origin proxy if player not found publish the payload
        if (!getDataManager().handleKick(playerUniqueId, message)) {
            new RedisTask<Void>(this) {
                @Override
                public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                    PayloadUtils.kickPlayerPayload(playerUniqueId, message, unifiedJedis);
                    return null;
                }
            }.execute();
        }
    }

    default void kickPlayer(String playerName, String message) {
        // fetch the uuid from name
        UUID playerUUID = getUuidTranslator().getTranslatedUuid(playerName, true);
        kickPlayer(playerUUID, message);
    }

    RedisBungeeMode getRedisBungeeMode();

    void updateProxiesIds();

}
