/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
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
import com.imaginarycode.minecraft.redisbungee.util.InternalCache;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This class manages all the data that RedisBungee fetches from Redis, along with updates to that data.
 *
 * @since 0.3.3
 */
public class DataManager implements Listener {
    private final RedisBungee plugin;
    // TODO: Add cleanup for this.
    private final InternalCache<UUID, String> serverCache = createCache();
    private final InternalCache<UUID, String> proxyCache = createCache(TimeUnit.MINUTES.toMillis(60));
    private final InternalCache<UUID, InetAddress> ipCache = createCache(TimeUnit.MINUTES.toMillis(60));
    private final InternalCache<UUID, Long> lastOnlineCache = createCache(TimeUnit.MINUTES.toMillis(60));

    public DataManager(RedisBungee plugin) {
        this.plugin = plugin;
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                proxyCache.cleanup();
                ipCache.cleanup();
                lastOnlineCache.cleanup();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public static <K, V> InternalCache<K, V> createCache() {
        return new InternalCache<>();
    }

    public static <K, V> InternalCache<K, V> createCache(long entryWriteExpiry) {
        return new InternalCache<>(entryWriteExpiry);
    }

    private final JsonParser parser = new JsonParser();

    public String getServer(final UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return player.getServer() != null ? player.getServer().getInfo().getName() : null;

        try {
            return serverCache.get(uuid, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try (Jedis tmpRsc = plugin.getPool().getResource()) {
                        return tmpRsc.hget("player:" + uuid, "server");
                    }
                }
            });
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get server", e);
            throw new RuntimeException("Unable to get server for " + uuid, e);
        }
    }

    public String getProxy(final UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return RedisBungee.getConfiguration().getServerId();

        try {
            return proxyCache.get(uuid, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try (Jedis tmpRsc = plugin.getPool().getResource()) {
                        return tmpRsc.hget("player:" + uuid, "proxy");
                    }
                }
            });
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get proxy", e);
            throw new RuntimeException("Unable to get proxy for " + uuid, e);
        }
    }

    public InetAddress getIp(final UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return player.getAddress().getAddress();

        try {
            return ipCache.get(uuid, new Callable<InetAddress>() {
                @Override
                public InetAddress call() throws Exception {
                    try (Jedis tmpRsc = plugin.getPool().getResource()) {
                        String result = tmpRsc.hget("player:" + uuid, "ip");
                        return result == null ? null : InetAddresses.forString(result);
                    }
                }
            });
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get IP", e);
            throw new RuntimeException("Unable to get IP for " + uuid, e);
        }
    }

    public long getLastOnline(final UUID uuid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return 0;

        try {
            return lastOnlineCache.get(uuid, new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    try (Jedis tmpRsc = plugin.getPool().getResource()) {
                        String result = tmpRsc.hget("player:" + uuid, "online");
                        return result == null ? -1 : Long.valueOf(result);
                    }
                }
            });
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get last time online", e);
            throw new RuntimeException("Unable to get last time online for " + uuid, e);
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

        // Partially deserialize the message so we can look at the action
        JsonObject jsonObject = parser.parse(event.getMessage()).getAsJsonObject();

        String source = jsonObject.get("source").getAsString();

        if (source.equals(RedisBungee.getConfiguration().getServerId()))
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
                final DataManagerMessage<ServerChangePayload> message3 = RedisBungee.getGson().fromJson(jsonObject, new TypeToken<DataManagerMessage<ServerChangePayload>>() {
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
