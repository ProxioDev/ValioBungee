package com.imaginarycode.minecraft.redisbungee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.util.*;
import com.imaginarycode.minecraft.redisbungee.util.uuid.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.util.uuid.UUIDFetcher;
import com.imaginarycode.minecraft.redisbungee.util.uuid.UUIDTranslator;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The RedisBungee plugin.
 * <p>
 * The only function of interest is {@link #getApi()}, which exposes some functions in this class.
 */
public final class RedisBungee extends Plugin {
    @Getter
    private static Gson gson = new Gson();
    private static RedisBungeeAPI api;
    @Getter(AccessLevel.PACKAGE)
    private static PubSubListener psl = null;
    @Getter
    private JedisPool pool;
    @Getter
    private UUIDTranslator uuidTranslator;
    @Getter(AccessLevel.PACKAGE)
    private static RedisBungeeConfiguration configuration;
    @Getter
    private DataManager dataManager;
    @Getter
    private static OkHttpClient httpClient;
    private volatile List<String> serverIds;
    private final AtomicInteger nagAboutServers = new AtomicInteger();
    private final AtomicInteger globalPlayerCount = new AtomicInteger();
    private Future<?> integrityCheck;
    private Future<?> heartbeatTask;
    private boolean usingLua;
    private LuaManager.Script serverToPlayersScript;
    private LuaManager.Script getPlayerCountScript;

    private static final Object SERVER_TO_PLAYERS_KEY = new Object();
    private final Cache<Object, Multimap<String, UUID>> serverToPlayersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    /**
     * Fetch the {@link RedisBungeeAPI} object created on plugin start.
     *
     * @return the {@link RedisBungeeAPI} object
     */
    public static RedisBungeeAPI getApi() {
        return api;
    }

    static PubSubListener getPubSubListener() {
        return psl;
    }

    final List<String> getServerIds() {
        return serverIds;
    }

    private List<String> getCurrentServerIds(boolean nag, boolean lagged) {
        try (Jedis jedis = pool.getResource()) {
            long time = getRedisTime(jedis.time());
            int nagTime = 0;
            if (nag) {
                nagTime = nagAboutServers.decrementAndGet();
                if (nagTime <= 0) {
                    nagAboutServers.set(10);
                }
            }
            ImmutableList.Builder<String> servers = ImmutableList.builder();
            Map<String, String> heartbeats = jedis.hgetAll("heartbeats");
            for (Map.Entry<String, String> entry : heartbeats.entrySet()) {
                try {
                    long stamp = Long.parseLong(entry.getValue());
                    if (lagged ? time >= stamp + 30 : time <= stamp + 30)
                        servers.add(entry.getKey());
                    else if (nag && nagTime <= 0) {
                        getLogger().severe(entry.getKey() + " is " + (time - stamp) + " seconds behind! (Time not synchronized or server down?)");
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return servers.build();
        } catch (JedisConnectionException e) {
            getLogger().log(Level.SEVERE, "Unable to fetch server IDs", e);
            return Collections.singletonList(configuration.getServerId());
        }
    }

    public Set<UUID> getPlayersOnProxy(String server) {
        checkArgument(getServerIds().contains(server), server + " is not a valid proxy ID");
        try (Jedis jedis = pool.getResource()) {
            Set<String> users = jedis.smembers("proxy:" + server + ":usersOnline");
            ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();
            for (String user : users) {
                builder.add(UUID.fromString(user));
            }
            return builder.build();
        }
    }

    final Multimap<String, UUID> serversToPlayers() {
        try {
            return serverToPlayersCache.get(SERVER_TO_PLAYERS_KEY, new Callable<Multimap<String, UUID>>() {
                @Override
                public Multimap<String, UUID> call() throws Exception {
                    Collection<String> data = (Collection<String>) serverToPlayersScript.eval(ImmutableList.<String>of(), getServerIds());

                    ImmutableMultimap.Builder<String, UUID> builder = ImmutableMultimap.builder();
                    String key = null;
                    for (String s : data) {
                        if (key == null) {
                            key = s;
                            continue;
                        }

                        builder.put(key, UUID.fromString(s));
                        key = null;
                    }

                    return builder.build();
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    final int getCount() {
        return globalPlayerCount.get();
    }

    final int getCurrentCount() {
        Long count = (Long) getPlayerCountScript.eval(ImmutableList.<String>of(), ImmutableList.<String>of());
        return count.intValue();
    }

    private Set<String> getLocalPlayersAsUuidStrings() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            builder.add(player.getUniqueId().toString());
        }
        return builder.build();
    }

    final Set<UUID> getPlayers() {
        ImmutableSet.Builder<UUID> setBuilder = ImmutableSet.builder();
        if (pool != null) {
            try (Jedis rsc = pool.getResource()) {
                List<String> keys = new ArrayList<>();
                for (String i : getServerIds()) {
                    keys.add("proxy:" + i + ":usersOnline");
                }
                if (!keys.isEmpty()) {
                    Set<String> users = rsc.sunion(keys.toArray(new String[keys.size()]));
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
                getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                throw new RuntimeException("Unable to get all players online", e);
            }
        }
        return setBuilder.build();
    }

    final void sendProxyCommand(@NonNull String proxyId, @NonNull String command) {
        checkArgument(getServerIds().contains(proxyId) || proxyId.equals("allservers"), "proxyId is invalid");
        sendChannelMessage("redisbungee-" + proxyId, command);
    }

    final void sendChannelMessage(String channel, String message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
            throw new RuntimeException("Unable to publish channel message", e);
        }
    }

    private long getRedisTime(List<String> timeRes) {
        return Long.parseLong(timeRes.get(0));
    }

    @Override
    public void onEnable() {
        ThreadFactory factory = ((ThreadPoolExecutor) getExecutorService()).getThreadFactory();
        getExecutorService().shutdownNow();
        ScheduledExecutorService service;
        try {
            Field field = Plugin.class.getDeclaredField("service");
            field.setAccessible(true);
            field.set(this, service = Executors.newScheduledThreadPool(24, factory));
        } catch (Exception e) {
            throw new RuntimeException("Can't replace BungeeCord thread pool with our own", e);
        }
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        if (pool != null) {
            try (Jedis tmpRsc = pool.getResource()) {
                // This is more portable than INFO <section>
                String info = tmpRsc.info();
                for (String s : info.split("\r\n")) {
                    if (s.startsWith("redis_version:")) {
                        String version = s.split(":")[1];
                        if (!(usingLua = RedisUtil.canUseLua(version))) {
                            getLogger().warning("Your version of Redis (" + version + ") is not at least version 2.6. RedisBungee requires a newer version of Redis.");
                            throw new RuntimeException("Unsupported Redis version detected");
                        } else {
                            LuaManager manager = new LuaManager(this);
                            serverToPlayersScript = manager.createScript(IOUtil.readInputStreamAsString(getResourceAsStream("lua/server_to_players.lua")));
                            getPlayerCountScript = manager.createScript(IOUtil.readInputStreamAsString(getResourceAsStream("lua/get_player_count.lua")));
                        }
                        break;
                    }
                }

                tmpRsc.hset("heartbeats", configuration.getServerId(), tmpRsc.time().get(0));

                long uuidCacheSize = tmpRsc.hlen("uuid-cache");
                if (uuidCacheSize > 750000) {
                    getLogger().info("Looks like you have a really big UUID cache! Run https://www.spigotmc.org/resources/redisbungeecleaner.8505/ as soon as possible.");
                }
            }
            serverIds = getCurrentServerIds(true, false);
            uuidTranslator = new UUIDTranslator(this);
            heartbeatTask = service.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try (Jedis rsc = pool.getResource()) {
                        long redisTime = getRedisTime(rsc.time());
                        rsc.hset("heartbeats", configuration.getServerId(), String.valueOf(redisTime));
                    } catch (JedisConnectionException e) {
                        // Redis server has disappeared!
                        getLogger().log(Level.SEVERE, "Unable to update heartbeat - did your Redis server go away?", e);
                        return;
                    }
                    try {
                        serverIds = getCurrentServerIds(true, false);
                        globalPlayerCount.set(getCurrentCount());
                    } catch (Throwable e) {
                        getLogger().log(Level.SEVERE, "Unable to update data - did your Redis server go away?", e);
                    }
                }
            }, 0, 3, TimeUnit.SECONDS);
            dataManager = new DataManager(this);
            if (configuration.isRegisterBungeeCommands()) {
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.GlistCommand(this));
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.FindCommand(this));
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.LastSeenCommand(this));
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.IpCommand(this));
            }
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.SendToAll(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerId(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerIds());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.PlayerProxyCommand(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.PlistCommand(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.DebugCommand(this));
            api = new RedisBungeeAPI(this);
            getProxy().getPluginManager().registerListener(this, new RedisBungeeListener(this, configuration.getExemptAddresses()));
            getProxy().getPluginManager().registerListener(this, dataManager);
            psl = new PubSubListener();
            getProxy().getScheduler().runAsync(this, psl);
            integrityCheck = service.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try (Jedis tmpRsc = pool.getResource()) {
                        Set<String> players = getLocalPlayersAsUuidStrings();
                        Set<String> playersInRedis = tmpRsc.smembers("proxy:" + configuration.getServerId() + ":usersOnline");
                        List<String> lagged = getCurrentServerIds(false, true);

                        // Clean up lagged players.
                        for (String s : lagged) {
                            Set<String> laggedPlayers = tmpRsc.smembers("proxy:" + s + ":usersOnline");
                            tmpRsc.del("proxy:" + s + ":usersOnline");
                            if (!laggedPlayers.isEmpty()) {
                                getLogger().info("Cleaning up lagged proxy " + s + " (" + laggedPlayers.size() + " players)...");
                                for (String laggedPlayer : laggedPlayers) {
                                    RedisUtil.cleanUpPlayer(laggedPlayer, tmpRsc);
                                }
                            }
                        }

                        Set<String> absentLocally = new HashSet<>(playersInRedis);
                        absentLocally.removeAll(players);
                        Set<String> absentInRedis = new HashSet<>(players);
                        absentInRedis.removeAll(playersInRedis);

                        for (String member : absentLocally) {
                            boolean found = false;
                            for (String proxyId : getServerIds()) {
                                if (proxyId.equals(configuration.getServerId())) continue;
                                if (tmpRsc.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                                    // Just clean up the set.
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                RedisUtil.cleanUpPlayer(member, tmpRsc);
                                getLogger().warning("Player found in set that was not found locally and globally: " + member);
                            } else {
                                tmpRsc.srem("proxy:" + configuration.getServerId() + ":usersOnline", member);
                                getLogger().warning("Player found in set that was not found locally, but is on another proxy: " + member);
                            }
                        }

                        Pipeline pipeline = tmpRsc.pipelined();

                        for (String player : absentInRedis) {
                            // Player not online according to Redis but not BungeeCord.
                            getLogger().warning("Player " + player + " is on the proxy but not in Redis.");

                            ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(UUID.fromString(player));
                            if (proxiedPlayer == null)
                                continue; // We'll deal with it later.

                            RedisUtil.createPlayer(proxiedPlayer, pipeline, true);
                        }

                        pipeline.sync();
                    } catch (Throwable e) {
                        getLogger().log(Level.SEVERE, "Unable to fix up stored player data", e);
                    }
                }
            }, 0, 1, TimeUnit.MINUTES);
        }
        getProxy().registerChannel("legacy:redisbungee");
        getProxy().registerChannel("RedisBungee");
    }

    @Override
    public void onDisable() {
        if (pool != null) {
            // Poison the PubSub listener
            psl.poison();
            integrityCheck.cancel(true);
            heartbeatTask.cancel(true);
            getProxy().getPluginManager().unregisterListeners(this);

            try (Jedis tmpRsc = pool.getResource()) {
                tmpRsc.hdel("heartbeats", configuration.getServerId());
                if (tmpRsc.scard("proxy:" + configuration.getServerId() + ":usersOnline") > 0) {
                    Set<String> players = tmpRsc.smembers("proxy:" + configuration.getServerId() + ":usersOnline");
                    for (String member : players)
                        RedisUtil.cleanUpPlayer(member, tmpRsc);
                }
            }

            pool.destroy();
        }
    }

    private void loadConfig() throws IOException, JedisConnectionException {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            file.createNewFile();
            try (InputStream in = getResourceAsStream("example_config.yml");
                 OutputStream out = new FileOutputStream(file)) {
                ByteStreams.copy(in, out);
            }
        }

        final Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

        final String redisServer = configuration.getString("redis-server", "localhost");
        final int redisPort = configuration.getInt("redis-port", 6379);
        String redisPassword = configuration.getString("redis-password");
        String serverId = configuration.getString("server-id");

        if (redisPassword != null && (redisPassword.isEmpty() || redisPassword.equals("none"))) {
            redisPassword = null;
        }

        // Configuration sanity checks.
        if (serverId == null || serverId.isEmpty()) {
            throw new RuntimeException("server-id is not specified in the configuration or is empty");
        }

        if (redisServer != null && !redisServer.isEmpty()) {
            final String finalRedisPassword = redisPassword;
            FutureTask<JedisPool> task = new FutureTask<>(new Callable<JedisPool>() {
                @Override
                public JedisPool call() throws Exception {
                    // Create the pool...
                    JedisPoolConfig config = new JedisPoolConfig();
                    config.setMaxTotal(configuration.getInt("max-redis-connections", 8));
                    return new JedisPool(config, redisServer, redisPort, 0, finalRedisPassword);
                }
            });

            getProxy().getScheduler().runAsync(this, task);

            try {
                pool = task.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Unable to create Redis pool", e);
            }

            // Test the connection
            try (Jedis rsc = pool.getResource()) {
                rsc.ping();
                // If that worked, now we can check for an existing, alive Bungee:
                File crashFile = new File(getDataFolder(), "restarted_from_crash.txt");
                if (crashFile.exists()) {
                    crashFile.delete();
                } else if (rsc.hexists("heartbeats", serverId)) {
                    try {
                        long value = Long.parseLong(rsc.hget("heartbeats", serverId));
                        long redisTime = getRedisTime(rsc.time());
                        if (redisTime < value + 20) {
                            getLogger().severe("You have launched a possible impostor BungeeCord instance. Another instance is already running.");
                            getLogger().severe("For data consistency reasons, RedisBungee will now disable itself.");
                            getLogger().severe("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                FutureTask<Void> task2 = new FutureTask<>(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        httpClient = new OkHttpClient();
                        Dispatcher dispatcher = new Dispatcher(getExecutorService());
                        httpClient.setDispatcher(dispatcher);
                        NameFetcher.setHttpClient(httpClient);
                        UUIDFetcher.setHttpClient(httpClient);
                        RedisBungee.configuration = new RedisBungeeConfiguration(RedisBungee.this.getPool(), configuration);
                        return null;
                    }
                });

                getProxy().getScheduler().runAsync(this, task2);

                try {
                    task2.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Unable to create HTTP client", e);
                }

                getLogger().log(Level.INFO, "Successfully connected to Redis.");
            } catch (JedisConnectionException e) {
                pool.destroy();
                pool = null;
                throw e;
            }
        } else {
            throw new RuntimeException("No redis server specified!");
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class PubSubListener implements Runnable {
        private JedisPubSubHandler jpsh;

        private Set<String> addedChannels = new HashSet<String>();

        @Override
        public void run() {
            boolean broken = false;
            try (Jedis rsc = pool.getResource()) {
                try {
                    jpsh = new JedisPubSubHandler();
                    addedChannels.add("redisbungee-" + configuration.getServerId());
                    addedChannels.add("redisbungee-allservers");
                    addedChannels.add("redisbungee-data");
                    rsc.subscribe(jpsh, addedChannels.toArray(new String[0]));
                } catch (Exception e) {
                    // FIXME: Extremely ugly hack
                    // Attempt to unsubscribe this instance and try again.
                    getLogger().log(Level.INFO, "PubSub error, attempting to recover.", e);
                    try {
                        jpsh.unsubscribe();
                    } catch (Exception e1) {
                        /* This may fail with
                        - java.net.SocketException: Broken pipe
                        - redis.clients.jedis.exceptions.JedisConnectionException: JedisPubSub was not subscribed to a Jedis instance
                        */
                    }
                    broken = true;
                }
            } catch (JedisConnectionException e) {
                getLogger().log(Level.INFO, "PubSub error, attempting to recover in 5 secs.");
                getProxy().getScheduler().schedule(RedisBungee.this, PubSubListener.this, 5, TimeUnit.SECONDS);
            }

            if (broken) {
                run();
            }
        }

        public void addChannel(String... channel) {
            addedChannels.addAll(Arrays.asList(channel));
            jpsh.subscribe(channel);
        }

        public void removeChannel(String... channel) {
            addedChannels.removeAll(Arrays.asList(channel));
            jpsh.unsubscribe(channel);
        }

        public void poison() {
            addedChannels.clear();
            jpsh.unsubscribe();
        }
    }

    private class JedisPubSubHandler extends JedisPubSub {
        @Override
        public void onMessage(final String s, final String s2) {
            if (s2.trim().length() == 0) return;
            getProxy().getScheduler().runAsync(RedisBungee.this, new Runnable() {
                @Override
                public void run() {
                    getProxy().getPluginManager().callEvent(new PubSubMessageEvent(s, s2));
                }
            });
        }
    }
}
