package com.imaginarycode.minecraft.redisbungee.internal;

import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.internal.util.uuid.UUIDTranslator;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface RedisBungeePlugin<P> {

    void enable();

    void disable();

    RedisBungeeConfiguration getConfiguration();

    int getCount();

    DataManager<P, ?, ?, ?> getDataManager();

    Set<UUID> getPlayers();

    Jedis requestJedis();

    RedisBungeeAPI getApi();

    UUIDTranslator getUuidTranslator();

    Multimap<String, UUID> serversToPlayers();

    Set<UUID> getPlayersOnProxy(String proxyId);

    void sendProxyCommand(String serverId, String command);

    List<String> getServerIds();

    PubSubListener getPubSubListener();

    void sendChannelMessage(String channel, String message);

    void executeAsync(Runnable runnable);

    void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int seconds);

    void callEvent(Object object);

    boolean isOnlineMode();

    void logInfo(String msg);

    void logWarn(String msg);

    void logFatal(String msg);

    boolean isPlayerServerNull(P player);

    P getPlayer(UUID uuid);

    P getPlayer(String name);

    UUID getPlayerUUID(String player);

    String getPlayerName(UUID player);

    String getPlayerServerName(P player);

    boolean isPlayerOnAServer(P player);

    InetAddress getPlayerIp(P player);

    void executeProxyCommand(String cmd);


}
