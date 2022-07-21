package com.imaginarycode.minecraft.redisbungee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.imaginarycode.minecraft.redisbungee.api.summoners.ClusterJedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.tasks.HeartbeatTask;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import com.imaginarycode.minecraft.redisbungee.api.util.RedisUtil;
import com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils;
import com.imaginarycode.minecraft.redisbungee.commands.RedisBungeeCommands;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.*;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.util.io.IOUtil;
import com.imaginarycode.minecraft.redisbungee.api.util.lua.LuaManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDTranslator;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkArgument;

public class RedisBungeeBungeePlugin extends Plugin implements RedisBungeePlugin<ProxiedPlayer> {

    private RedisBungeeAPI api;
    private RedisBungeeMode redisBungeeMode;
    private PubSubListener psl = null;
    private Summoner<?> jedisSummoner;
    private UUIDTranslator uuidTranslator;
    private RedisBungeeConfiguration configuration;
    private BungeeDataManager dataManager;
    private OkHttpClient httpClient;
    private volatile List<String> proxiesIds;
    private final AtomicInteger nagAboutServers = new AtomicInteger();
    private final AtomicInteger globalPlayerCount = new AtomicInteger();
    private Future<?> integrityCheck;
    private Future<?> heartbeatTask;
    private LuaManager.Script getRedisClusterTimeScript;
    private static final Object SERVER_TO_PLAYERS_KEY = new Object();
    private final Cache<Object, Multimap<String, UUID>> serverToPlayersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();


    @Override
    public RedisBungeeConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public int getCount() {
        return this.globalPlayerCount.get();
    }

    @Override
    public int getCurrentCount() {
        return new RedisTask<Long>(api) {
            @Override
            public Long jedisTask(Jedis jedis) {
                long total = 0;
                long redisTime = getRedisTime(jedis.time());
                Map<String, String> heartBeats = jedis.hgetAll("heartbeats");
                for (Map.Entry<String, String> stringStringEntry : heartBeats.entrySet()) {
                    String k = stringStringEntry.getKey();
                    String v = stringStringEntry.getValue();

                    long heartbeatTime = Long.parseLong(v);
                    if (heartbeatTime + 30 >= redisTime) {
                        total = total + jedis.scard("proxy:" + k + ":usersOnline");
                    }
                }
                return total;
            }

            @Override
            public Long clusterJedisTask(JedisCluster jedisCluster) {
                long total = 0;
                long redisTime = getRedisClusterTime();
                Map<String, String> heartBeats = jedisCluster.hgetAll("heartbeats");
                for (Map.Entry<String, String> stringStringEntry : heartBeats.entrySet()) {
                    String k = stringStringEntry.getKey();
                    String v = stringStringEntry.getValue();

                    long heartbeatTime = Long.parseLong(v);
                    if (heartbeatTime + 30 >= redisTime) {
                        total = total + jedisCluster.scard("proxy:" + k + ":usersOnline");
                    }
                }
                return total;
            }
        }.execute().intValue();

    }

    @Override
    public Set<String> getLocalPlayersAsUuidStrings() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            builder.add(player.getUniqueId().toString());
        }
        return builder.build();
    }

    @Override
    public AbstractDataManager<ProxiedPlayer, ?, ?, ?> getDataManager() {
        return this.dataManager;
    }

    @Override
    public Set<UUID> getPlayers() {
        return new RedisTask<Set<UUID>>(api) {
            @Override
            public Set<UUID> jedisTask(Jedis jedis) {
                ImmutableSet.Builder<UUID> setBuilder = ImmutableSet.builder();
                try {
                    List<String> keys = new ArrayList<>();
                    for (String i : getProxiesIds()) {
                        keys.add("proxy:" + i + ":usersOnline");
                    }
                    if (!keys.isEmpty()) {
                        Set<String> users = jedis.sunion(keys.toArray(new String[0]));
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
                return setBuilder.build();
            }

            @Override
            public Set<UUID> clusterJedisTask(JedisCluster jedisCluster) {
                ImmutableSet.Builder<UUID> setBuilder = ImmutableSet.builder();
                try {
                    List<String> keys = new ArrayList<>();
                    for (String i : getProxiesIds()) {
                        keys.add("proxy:" + i + ":usersOnline");
                    }
                    if (!keys.isEmpty()) {
                        Set<String> users = jedisCluster.sunion(keys.toArray(new String[0]));
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
                return setBuilder.build();
            }
        }.execute();
    }

    @Override
    public RedisBungeeAPI getApi() {
        return this.api;
    }

    @Override
    public UUIDTranslator getUuidTranslator() {
        return this.uuidTranslator;
    }

    @Override
    public Multimap<String, UUID> serversToPlayers() {
        try {
            return serverToPlayersCache.get(SERVER_TO_PLAYERS_KEY, new RedisTask<Multimap<String, UUID>>(api) {
                @Override
                public Multimap<String, UUID> jedisTask(Jedis jedis) {
                    ImmutableMultimap.Builder<String, UUID> builder = ImmutableMultimap.builder();
                    for (String serverId : getProxiesIds()) {
                        Set<String> players = jedis.smembers("proxy:" + serverId + ":usersOnline");
                        for (String player : players) {
                            String playerServer = jedis.hget("player:" + player, "server");
                            if (playerServer == null) {
                                continue;
                            }
                            builder.put(playerServer, UUID.fromString(player));
                        }
                    }
                    return builder.build();
                }

                @Override
                public Multimap<String, UUID> clusterJedisTask(JedisCluster jedisCluster) {
                    ImmutableMultimap.Builder<String, UUID> builder = ImmutableMultimap.builder();
                    for (String serverId : getProxiesIds()) {
                        Set<String> players = jedisCluster.smembers("proxy:" + serverId + ":usersOnline");
                        for (String player : players) {
                            String playerServer = jedisCluster.hget("player:" + player, "server");
                            if (playerServer == null) {
                                continue;
                            }
                            builder.put(playerServer, UUID.fromString(player));
                        }
                    }
                    return builder.build();
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<UUID> getPlayersOnProxy(String proxyId) {
        checkArgument(getProxiesIds().contains(proxyId), proxyId + " is not a valid proxy ID");
        return new RedisTask<Set<UUID>>(api) {
            @Override
            public Set<UUID> jedisTask(Jedis jedis) {
                Set<String> users = jedis.smembers("proxy:" + proxyId + ":usersOnline");
                ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();
                for (String user : users) {
                    builder.add(UUID.fromString(user));
                }
                return builder.build();
            }

            @Override
            public Set<UUID> clusterJedisTask(JedisCluster jedisCluster) {
                Set<String> users = jedisCluster.smembers("proxy:" + proxyId + ":usersOnline");
                ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();
                for (String user : users) {
                    builder.add(UUID.fromString(user));
                }
                return builder.build();
            }
        }.execute();
    }

    @Override
    public void sendProxyCommand(String proxyId, String command) {
        checkArgument(getProxiesIds().contains(proxyId) || proxyId.equals("allservers"), "proxyId is invalid");
        sendChannelMessage("redisbungee-" + proxyId, command);
    }

    @Override
    public List<String> getProxiesIds() {
        return proxiesIds;
    }

    @Override
    public List<String> getCurrentProxiesIds(boolean nag, boolean lagged) {
        return new RedisTask<List<String>>(api) {
            @Override
            public List<String> jedisTask(Jedis jedis) {
                try {
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
                                getLogger().warning(entry.getKey() + " is " + (time - stamp) + " seconds behind! (Time not synchronized or server down?) and was removed from heartbeat.");
                                jedis.hdel("heartbeats", entry.getKey());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    return servers.build();
                } catch (JedisConnectionException e) {
                    getLogger().log(Level.SEVERE, "Unable to fetch server IDs", e);
                    return Collections.singletonList(configuration.getProxyId());
                }
            }

            @Override
            public List<String> clusterJedisTask(JedisCluster jedisCluster) {
                try {
                    long time = getRedisClusterTime();
                    int nagTime = 0;
                    if (nag) {
                        nagTime = nagAboutServers.decrementAndGet();
                        if (nagTime <= 0) {
                            nagAboutServers.set(10);
                        }
                    }
                    ImmutableList.Builder<String> servers = ImmutableList.builder();
                    Map<String, String> heartbeats = jedisCluster.hgetAll("heartbeats");
                    for (Map.Entry<String, String> entry : heartbeats.entrySet()) {
                        try {
                            long stamp = Long.parseLong(entry.getValue());
                            if (lagged ? time >= stamp + 30 : time <= stamp + 30)
                                servers.add(entry.getKey());
                            else if (nag && nagTime <= 0) {
                                getLogger().warning(entry.getKey() + " is " + (time - stamp) + " seconds behind! (Time not synchronized or server down?) and was removed from heartbeat.");
                                jedisCluster.hdel("heartbeats", entry.getKey());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    return servers.build();
                } catch (JedisConnectionException e) {
                    getLogger().log(Level.SEVERE, "Unable to fetch server IDs", e);
                    return Collections.singletonList(configuration.getProxyId());
                }
            }
        }.execute();
    }

    @Override
    public PubSubListener getPubSubListener() {
        return this.psl;
    }

    @Override
    public void sendChannelMessage(String channel, String message) {
        new RedisTask<Void>(api) {
            @Override
            public Void jedisTask(Jedis jedis) {
                try {
                    jedis.publish(channel, message);
                } catch (JedisConnectionException e) {
                    // Redis server has disappeared!
                    getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                    throw new RuntimeException("Unable to publish channel message", e);
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                try {
                    jedisCluster.publish(channel, message);
                } catch (JedisConnectionException e) {
                    // Redis server has disappeared!
                    getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                    throw new RuntimeException("Unable to publish channel message", e);
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void executeAsync(Runnable runnable) {
        this.getProxy().getScheduler().runAsync(this, runnable);
    }

    @Override
    public void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int time) {
        this.getProxy().getScheduler().schedule(this, runnable, time, timeUnit);
    }

    @Override
    public void callEvent(Object event) {
        this.getProxy().getPluginManager().callEvent((Event) event);
    }

    @Override
    public boolean isOnlineMode() {
        return this.getProxy().getConfig().isOnlineMode();
    }

    @Override
    public void logInfo(String msg) {
        this.getLogger().info(msg);
    }

    @Override
    public void logWarn(String msg) {
        this.getLogger().warning(msg);
    }

    @Override
    public void logFatal(String msg) {
        this.getLogger().severe(msg);
    }

    @Override
    public ProxiedPlayer getPlayer(UUID uuid) {
        return this.getProxy().getPlayer(uuid);
    }

    @Override
    public ProxiedPlayer getPlayer(String name) {
        return this.getProxy().getPlayer(name);
    }

    @Override
    public UUID getPlayerUUID(String player) {
        return this.getProxy().getPlayer(player).getUniqueId();
    }

    @Override
    public String getPlayerName(UUID player) {
        return this.getProxy().getPlayer(player).getName();
    }

    @Override
    public String getPlayerServerName(ProxiedPlayer player) {
        return player.getServer().getInfo().getName();
    }

    @Override
    public boolean isPlayerOnAServer(ProxiedPlayer player) {
        return player.getServer() != null;
    }

    @Override
    public InetAddress getPlayerIp(ProxiedPlayer player) {
        return player.getAddress().getAddress();
    }

    @Override
    public void sendProxyCommand(String cmd) {
        checkArgument(getProxiesIds().contains(this.configuration.getProxyId()) || this.configuration.getProxyId().equals("allservers"), "proxyId is invalid");
        sendChannelMessage("redisbungee-" + this.configuration.getProxyId(), cmd);
    }

    @Override
    public long getRedisTime(List<String> timeRes) {
        return Long.parseLong(timeRes.get(0));
    }


    @Override
    public void initialize() {
        ThreadFactory factory = ((ThreadPoolExecutor) getExecutorService()).getThreadFactory();
        ScheduledExecutorService service = Executors.newScheduledThreadPool(24, factory);
        try {
            Field field = Plugin.class.getDeclaredField("service");
            field.setAccessible(true);
            ExecutorService builtinService = (ExecutorService) field.get(this);
            field.set(this, service);
            builtinService.shutdownNow();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            getLogger().log(Level.WARNING, "Can't replace BungeeCord thread pool with our own");
            getLogger().log(Level.INFO, "skipping replacement.....");
        }
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        }
        // init the api class
        this.api = new RedisBungeeAPI(this);
        // init the http lib
        httpClient = new OkHttpClient();
        Dispatcher dispatcher = new Dispatcher(getExecutorService());
        httpClient.setDispatcher(dispatcher);
        NameFetcher.setHttpClient(httpClient);
        UUIDFetcher.setHttpClient(httpClient);
        // init lua manager
        LuaManager luaManager = new LuaManager(this);
        new RedisTask<Void>(this) {
            @Override
            public Void jedisTask(Jedis jedis) {
                // This is more portable than INFO <section>
                String info = jedis.info();
                for (String s : info.split("\r\n")) {
                    if (s.startsWith("redis_version:")) {
                        String version = s.split(":")[1];
                        getLogger().info(version + " <- redis version");
                        if (!RedisUtil.isRedisVersionRight(version)) {
                            getLogger().severe("Your version of Redis (" + version + ") is not at least version 3.0 RedisBungee requires a newer version of Redis.");
                            throw new RuntimeException("Unsupported Redis version detected");
                        }
                        long uuidCacheSize = jedis.hlen("uuid-cache");
                        if (uuidCacheSize > 750000) {
                            getLogger().info("Looks like you have a really big UUID cache! Run https://www.spigotmc.org/resources/redisbungeecleaner.8505/ as soon as possible.");
                        }
                        break;
                    }
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                // This is more portable than INFO <section>
                try {
                    getRedisClusterTimeScript = luaManager.createScript(IOUtil.readInputStreamAsString(getResourceAsStream("lua/get_cluster_time.lua")));
                } catch (JedisException e) {
                    throw new RuntimeException("possible not supported redis version", e);
                }
                String info = (String) luaManager.createScript(IOUtil.readInputStreamAsString(getResourceAsStream("lua/get_cluster_info.lua"))).eval(Collections.singletonList("0"), Collections.emptyList());
                for (String s : info.split("\r\n")) {
                    if (s.startsWith("redis_version:")) {
                        String version = s.split(":")[1];
                        getLogger().info(version + " <- redis version");
                        if (!RedisUtil.isRedisVersionRight(version)) {
                            getLogger().severe("Your version of Redis (" + version + ") is not at least version 3.0 RedisBungee requires a newer version of Redis.");
                            throw new RuntimeException("Unsupported Redis version detected");
                        }
                        long uuidCacheSize = jedisCluster.hlen("uuid-cache");
                        if (uuidCacheSize > 750000) {
                            getLogger().info("Looks like you have a really big UUID cache! Run https://www.spigotmc.org/resources/redisbungeecleaner.8505/ as soon as possible.");
                        }
                        break;
                    }
                }
                return null;
            }
        }.execute();
        getLogger().info("lua manager was loaded");
        // check if this proxy is recovering from a crash and start heart the beat.
        new RedisTask<Void>(api) {
            @Override
            public Void jedisTask(Jedis jedis) {
                File crashFile = new File(getDataFolder(), "restarted_from_crash.txt");
                if (crashFile.exists() && crashFile.delete()) {
                    getLogger().info("crash file was deleted");
                } else if (jedis.hexists("heartbeats", configuration.getProxyId())) {
                    try {
                        long value = Long.parseLong(jedis.hget("heartbeats", configuration.getProxyId()));
                        long redisTime = getRedisTime(jedis.time());
                        if (redisTime < value + 20) {
                            getLogger().severe("You have launched a possible impostor Velocity / Bungeecord instance. Another instance is already running.");
                            getLogger().severe("For data consistency reasons, RedisBungee will now disable itself.");
                            getLogger().severe("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                File crashFile = new File(getDataFolder(), "restarted_from_crash.txt");
                if (crashFile.exists() && crashFile.delete()) {
                    getLogger().info("crash file was deleted");
                } else if (jedisCluster.hexists("heartbeats", configuration.getProxyId())) {
                    try {
                        long value = Long.parseLong(jedisCluster.hget("heartbeats", configuration.getProxyId()));
                        long redisTime = getRedisClusterTime();

                        if (redisTime < value + 20) {
                            getLogger().severe("You have launched a possible impostor Velocity / Bungeecord instance. Another instance is already running.");
                            getLogger().severe("For data consistency reasons, RedisBungee will now disable itself.");
                            getLogger().severe("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return null;
            }
        }.execute();

        updateProxyIds();

        uuidTranslator = new UUIDTranslator(this);


        heartbeatTask = service.scheduleAtFixedRate(new HeartbeatTask(this, this.globalPlayerCount), 0, HeartbeatTask.INTERVAL, HeartbeatTask.REPEAT_INTERVAL_TIME_UNIT);

        dataManager = new BungeeDataManager(this);
        getProxy().getPluginManager().registerListener(this, new RedisBungeeBungeeListener(this, configuration.getExemptAddresses()));
        getProxy().getPluginManager().registerListener(this, dataManager);
        psl = new PubSubListener(this);
        getProxy().getScheduler().runAsync(this, psl);

        RedisTask<Void> integrityCheckRedisTask = new RedisTask<Void>(api) {
            @Override
            public Void jedisTask(Jedis jedis) {
                try {
                    Set<String> players = getLocalPlayersAsUuidStrings();
                    Set<String> playersInRedis = jedis.smembers("proxy:" + configuration.getProxyId() + ":usersOnline");
                    List<String> lagged = getCurrentProxiesIds(false, true);

                    // Clean up lagged players.
                    for (String s : lagged) {
                        Set<String> laggedPlayers = jedis.smembers("proxy:" + s + ":usersOnline");
                        jedis.del("proxy:" + s + ":usersOnline");
                        if (!laggedPlayers.isEmpty()) {
                            getLogger().info("Cleaning up lagged proxy " + s + " (" + laggedPlayers.size() + " players)...");
                            for (String laggedPlayer : laggedPlayers) {
                                GenericPlayerUtils.cleanUpPlayer(laggedPlayer, jedis, true);
                            }
                        }
                    }

                    Set<String> absentLocally = new HashSet<>(playersInRedis);
                    absentLocally.removeAll(players);
                    Set<String> absentInRedis = new HashSet<>(players);
                    absentInRedis.removeAll(playersInRedis);

                    for (String member : absentLocally) {
                        boolean found = false;
                        for (String proxyId : getProxiesIds()) {
                            if (proxyId.equals(configuration.getProxyId())) continue;
                            if (jedis.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                                // Just clean up the set.
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            GenericPlayerUtils.cleanUpPlayer(member, jedis, false);
                            getLogger().warning("Player found in set that was not found locally and globally: " + member);
                        } else {
                            jedis.srem("proxy:" + configuration.getProxyId() + ":usersOnline", member);
                            getLogger().warning("Player found in set that was not found locally, but is on another proxy: " + member);
                        }
                    }

                    Pipeline pipeline = jedis.pipelined();

                    for (String player : absentInRedis) {
                        // Player not online according to Redis but not BungeeCord.
                        getLogger().warning("Player " + player + " is on the proxy but not in Redis.");

                        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(UUID.fromString(player));
                        if (proxiedPlayer == null)
                            continue; // We'll deal with it later.

                        BungeePlayerUtils.createPlayer(proxiedPlayer, pipeline, false);
                    }

                    pipeline.sync();
                } catch (Throwable e) {
                    getLogger().log(Level.SEVERE, "Unable to fix up stored player data", e);
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                try {
                    Set<String> players = getLocalPlayersAsUuidStrings();
                    Set<String> playersInRedis = jedisCluster.smembers("proxy:" + configuration.getProxyId() + ":usersOnline");
                    List<String> lagged = getCurrentProxiesIds(false, true);

                    // Clean up lagged players.
                    for (String s : lagged) {
                        Set<String> laggedPlayers = jedisCluster.smembers("proxy:" + s + ":usersOnline");
                        jedisCluster.del("proxy:" + s + ":usersOnline");
                        if (!laggedPlayers.isEmpty()) {
                            getLogger().info("Cleaning up lagged proxy " + s + " (" + laggedPlayers.size() + " players)...");
                            for (String laggedPlayer : laggedPlayers) {
                                GenericPlayerUtils.cleanUpPlayer(laggedPlayer, jedisCluster, true);
                            }
                        }
                    }

                    Set<String> absentLocally = new HashSet<>(playersInRedis);
                    absentLocally.removeAll(players);
                    Set<String> absentInRedis = new HashSet<>(players);
                    absentInRedis.removeAll(playersInRedis);

                    for (String member : absentLocally) {
                        boolean found = false;
                        for (String proxyId : getProxiesIds()) {
                            if (proxyId.equals(configuration.getProxyId())) continue;
                            if (jedisCluster.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                                // Just clean up the set.
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            GenericPlayerUtils.cleanUpPlayer(member, jedisCluster, false);
                            getLogger().warning("Player found in set that was not found locally and globally: " + member);
                        } else {
                            jedisCluster.srem("proxy:" + configuration.getProxyId() + ":usersOnline", member);
                            getLogger().warning("Player found in set that was not found locally, but is on another proxy: " + member);
                        }
                    }
                    // due JedisCluster does not support pipelined.
                    //Pipeline pipeline = jedis.pipelined();

                    for (String player : absentInRedis) {
                        // Player not online according to Redis but not BungeeCord.
                        getLogger().warning("Player " + player + " is on the proxy but not in Redis.");

                        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(UUID.fromString(player));
                        if (proxiedPlayer == null)
                            continue; // We'll deal with it later.

                        BungeePlayerUtils.createPlayer(proxiedPlayer, jedisCluster, true);
                    }

                } catch (Throwable e) {
                    getLogger().log(Level.SEVERE, "Unable to fix up stored player data", e);
                }
                return null;
            }
        };

        integrityCheck = service.scheduleAtFixedRate(integrityCheckRedisTask::execute, 0, 30, TimeUnit.SECONDS);

        // register plugin messages channel.
        getProxy().registerChannel("legacy:redisbungee");
        getProxy().registerChannel("RedisBungee");
        // register commands
        if (configuration.doOverrideBungeeCommands()) {
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.GlistCommand(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.FindCommand(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.LastSeenCommand(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.IpCommand(this));
        }
        getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.SendToAll(this));
        getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerId(this));
        getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerIds(this));
        getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.PlayerProxyCommand(this));
        getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.PlistCommand(this));
    }

    @Override
    public void stop() {
        // Poison the PubSub listener
        if (psl != null) {
            psl.poison();
        }
        if (integrityCheck != null) {
            integrityCheck.cancel(true);
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }

        getProxy().getPluginManager().unregisterListeners(this);

        new RedisTask<Void>(api) {
            @Override
            public Void jedisTask(Jedis jedis) {
                jedis.hdel("heartbeats", configuration.getProxyId());
                if (jedis.scard("proxy:" + configuration.getProxyId() + ":usersOnline") > 0) {
                    Set<String> players = jedis.smembers("proxy:" + configuration.getProxyId() + ":usersOnline");
                    for (String member : players)
                        GenericPlayerUtils.cleanUpPlayer(member, jedis, true);
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                jedisCluster.hdel("heartbeats", configuration.getProxyId());
                if (jedisCluster.scard("proxy:" + configuration.getProxyId() + ":usersOnline") > 0) {
                    Set<String> players = jedisCluster.smembers("proxy:" + configuration.getProxyId() + ":usersOnline");
                    for (String member : players)
                        GenericPlayerUtils.cleanUpPlayer(member, jedisCluster, true);
                }
                return null;
            }
        }.execute();
        try {
            this.jedisSummoner.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Summoner<?> getSummoner() {
        return this.jedisSummoner;
    }

    @Override
    public void loadConfig() throws IOException {
        if (!getDataFolder().exists() && getDataFolder().mkdir()) {
            getLogger().info("data folder was created");
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists() && file.createNewFile()) {
            try (InputStream in = getResourceAsStream("example_config.yml");
                 OutputStream out = Files.newOutputStream(file.toPath())) {
                ByteStreams.copy(in, out);
            }
        }
        final Configuration yamlConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        final String redisServer = yamlConfiguration.getString("redis-server", "localhost");
        final int redisPort = yamlConfiguration.getInt("redis-port", 6379);
        final boolean useSSL = yamlConfiguration.getBoolean("useSSL", false);
        String redisPassword = yamlConfiguration.getString("redis-password", "");
        String serverId = yamlConfiguration.getString("server-id");
        // check redis password
        if (redisPassword != null && (redisPassword.isEmpty() || redisPassword.equals("none"))) {
            redisPassword = null;
            getLogger().warning("INSECURE setup was detected Please set password for your redis instance.");
        }
        if (!useSSL) {
            getLogger().warning("INSECURE setup was detected Please setup ssl for your redis instance.");
        }
        // Configuration sanity checks.
        if (serverId == null || serverId.isEmpty()) {
            /*
             *  this check causes the config comments to disappear somehow
             *  I think due snake yaml limitations so as todo: write our own yaml parser?
             */
            String genId = UUID.randomUUID().toString();
            getLogger().info("Generated server id " + genId + " and saving it to config.");
            yamlConfiguration.set("server-id", genId);
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(yamlConfiguration, new File(getDataFolder(), "config.yml"));
            getLogger().info("Server id was generated: " + serverId);
        } else {
            getLogger().info("server id: " + serverId + '.');
        }
        this.configuration = new RedisBungeeConfiguration(serverId, yamlConfiguration.getStringList("exempt-ip-addresses"), yamlConfiguration.getBoolean("register-bungee-commands", true));

        if (redisServer != null && !redisServer.isEmpty()) {
            if (yamlConfiguration.getBoolean("cluster-mode-enabled", false)) {
                GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
                poolConfig.setMaxTotal(yamlConfiguration.getInt("max-redis-connections", 8));
                if (redisPassword != null) {
                    this.jedisSummoner = new ClusterJedisSummoner(new JedisCluster(new HostAndPort(redisServer, redisPort), 5000, 5000, 60, serverId, redisPassword, poolConfig, useSSL));
                } else {
                    this.jedisSummoner = new ClusterJedisSummoner(new JedisCluster(new HostAndPort(redisServer, redisPort), 5000, 5000, 60, poolConfig));
                    getLogger().warning("SSL option is ignored in Cluster mode if no PASSWORD is set");
                }
                this.redisBungeeMode = RedisBungeeMode.CLUSTER;
                getLogger().log(Level.INFO, "RedisBungee MODE: CLUSTER");
            } else {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(yamlConfiguration.getInt("max-redis-connections", 8));
                this.jedisSummoner = new JedisSummoner(new JedisPool(config, redisServer, redisPort, 0, redisPassword, useSSL));
                this.redisBungeeMode = RedisBungeeMode.SINGLE;
                getLogger().log(Level.INFO, "RedisBungee MODE: SINGLE");
            }

            getLogger().log(Level.INFO, "Successfully connected to Redis.");

        } else {
            throw new RuntimeException("No redis server specified!");
        }
    }

    @Override
    public void kickPlayer(UUID playerUniqueId, String message) {
        // first handle on origin proxy if player not found publish the payload
        if (!dataManager.handleKick(playerUniqueId, message)) {
            new RedisTask<Void>(api) {
                @Override
                public Void jedisTask(Jedis jedis) {
                    PayloadUtils.kickPlayerPayload(playerUniqueId, message, jedis);
                    return null;
                }

                @Override
                public Void clusterJedisTask(JedisCluster jedisCluster) {
                    PayloadUtils.kickPlayerPayload(playerUniqueId, message, jedisCluster);
                    return null;
                }
            }.execute();
        }
    }

    @Override
    public void kickPlayer(String playerName, String message) {
        // fetch the uuid
        UUID playerUUID = this.uuidTranslator.getTranslatedUuid(playerName, true);
        kickPlayer(playerUUID, message);
    }

    @Override
    public RedisBungeeMode getRedisBungeeMode() {
        return this.redisBungeeMode;
    }

    @Override
    public Long getRedisClusterTime() {
        return getRedisTime((List<String>) this.getRedisClusterTimeScript.eval(Collections.singletonList("0"), Collections.emptyList()));
    }

    @Override
    public void updateProxyIds() {
        proxiesIds = getCurrentProxiesIds(true, false);
    }

    @Override
    public void onEnable() {
        initialize();
    }

    @Override
    public void onDisable() {
        stop();
    }

    @Override
    public Object createPlayerChangedNetworkEvent(UUID uuid, String previousServer, String server) {
        return new PlayerChangedServerNetworkEvent(uuid, previousServer, server);
    }

    @Override
    public Object createPlayerJoinedNetworkEvent(UUID uuid) {
        return new PlayerJoinedNetworkEvent(uuid);
    }

    @Override
    public Object createPlayerLeftNetworkEvent(UUID uuid) {
        return new PlayerLeftNetworkEvent(uuid);
    }

    @Override
    public Object createPubSubEvent(String channel, String message) {
        return new PubSubMessageEvent(channel, message);
    }
}
