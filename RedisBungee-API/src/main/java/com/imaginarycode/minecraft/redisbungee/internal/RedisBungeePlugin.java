package com.imaginarycode.minecraft.redisbungee.internal;

import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.util.uuid.UUIDTranslator;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface RedisBungeePlugin<P> {

    default void enable() {

    }

    default void disable() {

    }

    RedisBungeeConfiguration getConfiguration();

    int getCount();

    int getCurrentCount();

    Set<String> getLocalPlayersAsUuidStrings();

    DataManager<P, ?, ?, ?> getDataManager();

    Set<UUID> getPlayers();

    Jedis requestJedis();

    boolean isJedisAvailable();

    RedisBungeeAPI getApi();

    UUIDTranslator getUuidTranslator();

    Multimap<String, UUID> serversToPlayers();

    Set<UUID> getPlayersOnProxy(String proxyId);

    void sendProxyCommand(String serverId, String command);

    List<String> getServerIds();

    List<String > getCurrentServerIds(boolean nag, boolean lagged);

    PubSubListener getPubSubListener();

    void sendChannelMessage(String channel, String message);

    void executeAsync(Runnable runnable);

    void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int time);

    void callEvent(Object event);

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

    void sendProxyCommand(String cmd);

    default Class<?> getPubSubEventClass() {
        return PubSubMessageEvent.class;
    }

    default Class<?> getNetworkJoinEventClass() {
        return PlayerJoinedNetworkEvent.class;
    }

    default Class<?> getServerChangeEventClass() {
        return PlayerChangedServerNetworkEvent.class;
    }

    default Class<?> getNetworkQuitEventClass() {
        return PlayerLeftNetworkEvent.class;
    }

    long getRedisTime(List<String> timeRes);

    void loadConfig() throws Exception;

}
