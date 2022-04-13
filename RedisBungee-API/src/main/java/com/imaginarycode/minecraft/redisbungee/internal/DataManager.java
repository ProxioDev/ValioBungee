package com.imaginarycode.minecraft.redisbungee.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.InetAddresses;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class manages all the data that RedisBungee fetches from Redis, along with updates to that data.
 *
 * @since 0.3.3
 */
public abstract class DataManager<P, PS, PL, PD> {
    private final RedisBungeePlugin<P> plugin;
    private final Cache<UUID, String> serverCache = createCache();
    private final Cache<UUID, String> proxyCache = createCache();
    private final Cache<UUID, InetAddress> ipCache = createCache();
    private final Cache<UUID, Long> lastOnlineCache = createCache();
    private final Gson gson = new Gson();

    public DataManager(RedisBungeePlugin<P> plugin) {
        this.plugin = plugin;
    }

    private static <K, V> Cache<K, V> createCache() {
        // TODO: Allow customization via cache specification, ala ServerListPlus
        return CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    private final JsonParser parser = new JsonParser();

    public String getServer(final UUID uuid) {
        P player = plugin.getPlayer(uuid);

        if (player != null)
            return plugin.isPlayerOnAServer(player) ? plugin.getPlayerServerName(player) : null;

        try {
            return serverCache.get(uuid, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try (Jedis tmpRsc = plugin.requestJedis()) {
                        return Objects.requireNonNull(tmpRsc.hget("player:" + uuid, "server"), "user not found");
                    }
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
            return plugin.getConfiguration().getServerId();

        try {
            return proxyCache.get(uuid, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try (Jedis tmpRsc = plugin.requestJedis()) {
                        return Objects.requireNonNull(tmpRsc.hget("player:" + uuid, "proxy"), "user not found");
                    }
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
            return ipCache.get(uuid, new Callable<InetAddress>() {
                @Override
                public InetAddress call() throws Exception {
                    try (Jedis tmpRsc = plugin.requestJedis()) {
                        String result = tmpRsc.hget("player:" + uuid, "ip");
                        if (result == null)
                            throw new NullPointerException("user not found");
                        return InetAddresses.forString(result);
                    }
                }
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof NullPointerException && e.getCause().getMessage().equals("user not found"))
                return null; // HACK
            plugin.logFatal( "Unable to get IP");
            throw new RuntimeException("Unable to get IP for " + uuid, e);
        }
    }

    public long getLastOnline(final UUID uuid) {
        P player = plugin.getPlayer(uuid);

        if (player != null)
            return 0;

        try {
            return lastOnlineCache.get(uuid, new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    try (Jedis tmpRsc = plugin.requestJedis()) {
                        String result = tmpRsc.hget("player:" + uuid, "online");
                        return result == null ? -1 : Long.valueOf(result);
                    }
                }
            });
        } catch (ExecutionException e) {
            plugin.logFatal("Unable to get last time online");
            throw new RuntimeException("Unable to get last time online for " + uuid, e);
        }
    }

    private void invalidate(UUID uuid) {
        ipCache.invalidate(uuid);
        lastOnlineCache.invalidate(uuid);
        serverCache.invalidate(uuid);
        proxyCache.invalidate(uuid);
    }


    public void onPostLogin(PL event) {

    }

    public void onPlayerDisconnect(PD event) {

    }

    public abstract void onPubSubMessage(PS event);

    protected void handlePubSubMessage(String channel, String message) {
        if (!channel.equals("redisbungee-data"))
            return;

        // Partially deserialize the message so we can look at the action
        JsonObject jsonObject = parser.parse(message).getAsJsonObject();

        String source = jsonObject.get("source").getAsString();

        if (source.equals(plugin.getConfiguration().getServerId()))
            return;

        DataManagerMessage.Action action = DataManagerMessage.Action.valueOf(jsonObject.get("action").getAsString());

        switch (action) {
            case JOIN:
                final DataManagerMessage message1 = gson.fromJson(jsonObject, new TypeToken<DataManagerMessage>() {
                }.getType());
                proxyCache.put(message1.getTarget(), message1.getSource());
                lastOnlineCache.put(message1.getTarget(), (long) 0);
                ipCache.put(message1.getTarget(), ((LoginPayload)message1.getPayload()).getAddress());
                plugin.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        //plugin.getProxy().getPluginManager().callEvent(new PlayerJoinedNetworkEvent(message1.getTarget()));
                    }
                });
                break;
            case LEAVE:
                final DataManagerMessage message2 = gson.fromJson(jsonObject, new TypeToken<DataManagerMessage>() {
                }.getType());
                invalidate(message2.getTarget());
                lastOnlineCache.put(message2.getTarget(), ((LogoutPayload)message2.getPayload()).getTimestamp());
                plugin.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                       // plugin.getProxy().getPluginManager().callEvent(new PlayerLeftNetworkEvent(message2.getTarget()));
                    }
                });
                break;
            case SERVER_CHANGE:
                final DataManagerMessage message3 = gson.fromJson(jsonObject, new TypeToken<DataManagerMessage>() {
                }.getType());
                serverCache.put(message3.getTarget(), ((ServerChangePayload)message3.getPayload()).getServer());
                plugin.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        //plugin.getProxy().getPluginManager().callEvent(new PlayerChangedServerNetworkEvent(message3.getTarget(), message3.getPayload().getOldServer(), message3.getPayload().getServer()));
                    }
                });
                break;
        }
    }



    public static class DataManagerMessage {
        private final UUID target;
        private final String source;
        private final Action action; // for future use!
        private final Payload payload;

        public DataManagerMessage(UUID target, String source, Action action, Payload payload) {
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

        public Payload getPayload() {
            return payload;
        }

        public enum Action {
            JOIN,
            LEAVE,
            SERVER_CHANGE
        }
    }

    public static abstract class Payload {
    }


    public static class LoginPayload extends Payload{
        private final InetAddress address;

        public LoginPayload(InetAddress address) {
            this.address = address;
        }

        public InetAddress getAddress() {
            return address;
        }
    }

    public static class ServerChangePayload extends Payload{
        private final String server;
        private final String oldServer;

        ServerChangePayload(String server, String oldServer) {
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