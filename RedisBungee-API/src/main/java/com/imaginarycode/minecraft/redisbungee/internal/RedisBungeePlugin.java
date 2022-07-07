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



/**
 * This Class has all internal methods needed by every redis bungee plugin, and it can be used to implement another platforms than bungeecord
 *
 * @author Ham1255
 * @since 0.7.0
 *
 */
public interface RedisBungeePlugin<P> extends EventsPlatform{

    default void initialize() {

    }

    default void stop() {

    }

    Jedis requestJedis();

    boolean isJedisAvailable();

    RedisBungeeConfiguration getConfiguration();

    int getCount();

    int getCurrentCount();

    Set<String> getLocalPlayersAsUuidStrings();

    AbstractDataManager<P, ?, ?, ?> getDataManager();

    Set<UUID> getPlayers();

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

    long getRedisTime(List<String> timeRes);

    void loadConfig() throws Exception;

}
