package com.imaginarycode.minecraft.redisbungee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.imaginarycode.minecraft.redisbungee.api.*;
import com.imaginarycode.minecraft.redisbungee.api.summoners.ClusterJedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import com.imaginarycode.minecraft.redisbungee.api.util.RedisUtil;
import com.imaginarycode.minecraft.redisbungee.api.util.io.IOUtil;
import com.imaginarycode.minecraft.redisbungee.api.util.lua.LuaManager;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDTranslator;
import com.imaginarycode.minecraft.redisbungee.commands.RedisBungeeCommands;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

@Plugin(id = "redisbungee", name = "RedisBungee", version = PomData.VERSION, url = "https://github.com/ProxioDev/RedisBungee", authors = "ProxioDev")
public class RedisBungeeVelocityPlugin implements RedisBungeePlugin<Player> {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;
    private final RedisBungeeAPI api;
    private final PubSubListener psl;
    private Summoner<?> jedisSummoner;
    private RedisBungeeMode redisBungeeMode;
    private final UUIDTranslator uuidTranslator;
    private RedisBungeeConfiguration configuration;
    private final VelocityDataManager dataManager;
    private final OkHttpClient httpClient;
    private volatile List<String> serverIds;
    private final AtomicInteger nagAboutServers = new AtomicInteger();
    private final AtomicInteger globalPlayerCount = new AtomicInteger();
    private ScheduledTask integrityCheck;
    private ScheduledTask heartbeatTask;

    private LuaManager.Script getRedisClusterTimeScript;

    private static final Object SERVER_TO_PLAYERS_KEY = new Object();
    public static final List<ChannelIdentifier> IDENTIFIERS = List.of(
            MinecraftChannelIdentifier.create("legacy", "redisbungee"),
            new LegacyChannelIdentifier("RedisBungee")
    );
    private final Cache<Object, Multimap<String, UUID>> serverToPlayersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();


    @Inject
    public RedisBungeeVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataFolder = dataDirectory;
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        this.api = new RedisBungeeAPI(this);
        LuaManager luaManager = new LuaManager(this);
        new RedisTask<Void>(this) {
            @Override
            public Void jedisTask(Jedis jedis) {
                // This is more portable than INFO <section>
                String info = jedis.info();
                for (String s : info.split("\r\n")) {
                    if (s.startsWith("redis_version:")) {
                        String version = s.split(":")[1];
                        getLogger().info("{} <- redis version", version);
                        if (!RedisUtil.isRedisVersionRight(version)) {
                            getLogger().error("Your version of Redis ({}) is not at least version 6.0 RedisBungee requires a newer version of Redis.", version);
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
                        getLogger().info("{} <- redis version", version);
                        if (!RedisUtil.isRedisVersionRight(version)) {
                            getLogger().error("Your version of Redis ({}) is not at least version 6.0 RedisBungee requires a newer version of Redis.", version);
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
                Path crashFile = getDataFolder().resolve("restarted_from_crash.txt");
                if (Files.exists(crashFile)) {
                    try {
                        Files.delete(crashFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    getLogger().info("crash file was deleted");
                } else if (jedis.hexists("heartbeats", configuration.getServerId())) {
                    try {
                        long value = Long.parseLong(jedis.hget("heartbeats", configuration.getServerId()));
                        long redisTime = getRedisTime(jedis.time());
                        if (redisTime < value + 20) {
                            getLogger().error("You have launched a possible impostor Velocity / Bungeecord instance. Another instance is already running.");
                            getLogger().error("For data consistency reasons, RedisBungee will now disable itself.");
                            getLogger().error("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    jedis.hset("heartbeats", configuration.getServerId(), jedis.time().get(0));
                }

                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                Path crashFile = getDataFolder().resolve("restarted_from_crash.txt");
                if (Files.exists(crashFile)) {
                    try {
                        Files.delete(crashFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    getLogger().info("crash file was deleted");
                } else if (jedisCluster.hexists("heartbeats", configuration.getServerId())) {
                    try {
                        long value = Long.parseLong(jedisCluster.hget("heartbeats", configuration.getServerId()));
                        long redisTime = getRedisClusterTime();

                        if (redisTime < value + 20) {
                            getLogger().error("You have launched a possible impostor Velocity / Bungeecord instance. Another instance is already running.");
                            getLogger().error("For data consistency reasons, RedisBungee will now disable itself.");
                            getLogger().error("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    jedisCluster.hset("heartbeats", configuration.getServerId(), String.valueOf(getRedisClusterTime()));
                }
                return null;
            }
        }.execute();
        uuidTranslator = new UUIDTranslator(this);
        dataManager = new VelocityDataManager(this);
        psl = new PubSubListener(this);
        this.httpClient = new OkHttpClient();
        Dispatcher dispatcher = new Dispatcher(Executors.newFixedThreadPool(6));
        this.httpClient.setDispatcher(dispatcher);
        NameFetcher.setHttpClient(httpClient);
        UUIDFetcher.setHttpClient(httpClient);
        // keeping this lol
        new RedisBungee(api);


    }


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
        for (Player player : getProxy().getAllPlayers()) {
            builder.add(player.getUniqueId().toString());
        }
        return builder.build();
    }

    @Override
    public AbstractDataManager<Player, ?, ?, ?> getDataManager() {
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
                    for (String i : getServerIds()) {
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
                    getLogger().error("Unable to get connection from pool - did your Redis server go away?", e);
                    throw new RuntimeException("Unable to get all players online", e);
                }
                return setBuilder.build();
            }

            @Override
            public Set<UUID> clusterJedisTask(JedisCluster jedisCluster) {
                ImmutableSet.Builder<UUID> setBuilder = ImmutableSet.builder();
                try {
                    List<String> keys = new ArrayList<>();
                    for (String i : getServerIds()) {
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
                    getLogger().error("Unable to get connection from pool - did your Redis server go away?", e);
                    throw new RuntimeException("Unable to get all players online", e);
                }
                return setBuilder.build();
            }
        }.execute();
    }


    @Override
    public Summoner<?> getSummoner() {
        return this.jedisSummoner;
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
                    for (String serverId : getServerIds()) {
                        Set<String> players = jedis.smembers("proxy:" + serverId + ":usersOnline");
                        for (String player : players) {
                            builder.put(serverId, UUID.fromString(player));
                        }
                    }
                    return builder.build();
                }

                @Override
                public Multimap<String, UUID> clusterJedisTask(JedisCluster jedisCluster) {
                    ImmutableMultimap.Builder<String, UUID> builder = ImmutableMultimap.builder();
                    for (String serverId : getServerIds()) {
                        Set<String> players = jedisCluster.smembers("proxy:" + serverId + ":usersOnline");
                        for (String player : players) {
                            builder.put(serverId, UUID.fromString(player));
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
        checkArgument(getServerIds().contains(proxyId), proxyId + " is not a valid proxy ID");
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
    public void sendProxyCommand(String serverId, String command) {
        checkArgument(getServerIds().contains(serverId) || serverId.equals("allservers"), "proxyId is invalid");
        sendChannelMessage("redisbungee-" + serverId, command);
    }

    @Override
    public List<String> getServerIds() {
        return serverIds;
    }


    @Override
    public List<String> getCurrentServerIds(boolean nag, boolean lagged) {
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
                                getLogger().warn("{} is {} seconds behind! (Time not synchronized or server down?) and was removed from heartbeat.", entry.getKey(), (time - stamp));
                                jedis.hdel("heartbeats", entry.getKey());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    return servers.build();
                } catch (JedisConnectionException e) {
                    getLogger().error("Unable to fetch server IDs", e);
                    return Collections.singletonList(configuration.getServerId());
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
                                getLogger().warn("{} is {} seconds behind! (Time not synchronized or server down?) and was removed from heartbeat.", entry.getKey(), (time - stamp));
                                jedisCluster.hdel("heartbeats", entry.getKey());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    return servers.build();
                } catch (JedisConnectionException e) {
                    getLogger().error("Unable to fetch server IDs", e);
                    return Collections.singletonList(configuration.getServerId());
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
                    getLogger().error("Unable to get connection from pool - did your Redis server go away?", e);
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
                    getLogger().error("Unable to get connection from pool - did your Redis server go away?", e);
                    throw new RuntimeException("Unable to publish channel message", e);
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void executeAsync(Runnable runnable) {
        this.getProxy().getScheduler().buildTask(this, runnable).schedule();
    }

    @Override
    public void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int time) {
        this.getProxy().getScheduler().buildTask(this, runnable).delay(time, timeUnit).schedule();
    }

    @Override
    public void callEvent(Object event) {
        this.getProxy().getEventManager().fireAndForget(event);
    }

    @Override
    public boolean isOnlineMode() {
        return this.getProxy().getConfiguration().isOnlineMode();
    }

    @Override
    public void logInfo(String msg) {
        this.getLogger().info(msg);
    }

    @Override
    public void logWarn(String msg) {
        this.getLogger().warn(msg);
    }

    @Override
    public void logFatal(String msg) {
        this.getLogger().error(msg);
    }

    @Override
    public Player getPlayer(UUID uuid) {
        return this.getProxy().getPlayer(uuid).orElse(null);
    }

    @Override
    public Player getPlayer(String name) {
        return this.getProxy().getPlayer(name).orElse(null);
    }

    @Override
    public UUID getPlayerUUID(String player) {
        return this.getProxy().getPlayer(player).map(Player::getUniqueId).orElse(null);
    }

    @Override
    public String getPlayerName(UUID player) {
        return this.getProxy().getPlayer(player).map(Player::getUsername).orElse(null);
    }

    @Override
    public String getPlayerServerName(Player player) {
        return player.getCurrentServer().map(serverConnection -> serverConnection.getServerInfo().getName()).orElse(null);
    }

    @Override
    public boolean isPlayerOnAServer(Player player) {
        return player.getCurrentServer().isPresent();
    }

    @Override
    public InetAddress getPlayerIp(Player player) {
        return player.getRemoteAddress().getAddress();
    }

    @Override
    public void sendProxyCommand(String cmd) {
        checkArgument(getServerIds().contains(this.configuration.getServerId()) || this.configuration.getServerId().equals("allservers"), "proxyId is invalid");
        sendChannelMessage("redisbungee-" + this.configuration.getServerId(), cmd);
    }

    @Override
    public long getRedisTime(List<String> timeRes) {
        return Long.parseLong(timeRes.get(0));
    }


    @Override
    public void initialize() {
        serverIds = getCurrentServerIds(true, false);
        // start heartbeat task
        RedisTask<Void> heartBeatRedisTask = new RedisTask<Void>(api) {
            @Override
            public Void jedisTask(Jedis jedis) {
                try {
                    long redisTime = getRedisTime(jedis.time());
                    jedis.hset("heartbeats", configuration.getServerId(), String.valueOf(redisTime));
                } catch (JedisConnectionException e) {
                    // Redis server has disappeared!
                    getLogger().error("Unable to update heartbeat - did your Redis server go away?", e);
                    return null;
                }
                try {
                    serverIds = getCurrentServerIds(true, false);
                    globalPlayerCount.set(getCurrentCount());
                } catch (Throwable e) {
                    getLogger().error("Unable to update data - did your Redis server go away?", e);
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                try {
                    long redisTime = getRedisClusterTime();
                    jedisCluster.hset("heartbeats", configuration.getServerId(), String.valueOf(redisTime));
                } catch (JedisConnectionException e) {
                    // Redis server has disappeared!
                    getLogger().error("Unable to update heartbeat - did your Redis server go away?", e);
                    return null;
                }
                try {
                    serverIds = getCurrentServerIds(true, false);
                    globalPlayerCount.set(getCurrentCount());
                } catch (Throwable e) {
                    getLogger().error("Unable to update data - did your Redis server go away?", e);
                }
                return null;
            }
        };
        heartbeatTask = getProxy().getScheduler().buildTask(this, heartBeatRedisTask::execute).repeat(3, TimeUnit.SECONDS).schedule();

        getProxy().getEventManager().register(this, new RedisBungeeVelocityListener(this, configuration.getExemptAddresses()));
        getProxy().getEventManager().register(this, dataManager);
        getProxy().getScheduler().buildTask(this, psl).schedule();
        RedisTask<Void> integrityCheckRedisTask = new RedisTask<Void>(api) {
            @Override
            public Void jedisTask(Jedis jedis) {
                try {
                    Set<String> players = getLocalPlayersAsUuidStrings();
                    Set<String> playersInRedis = jedis.smembers("proxy:" + configuration.getServerId() + ":usersOnline");
                    List<String> lagged = getCurrentServerIds(false, true);

                    // Clean up lagged players.
                    for (String s : lagged) {
                        Set<String> laggedPlayers = jedis.smembers("proxy:" + s + ":usersOnline");
                        jedis.del("proxy:" + s + ":usersOnline");
                        if (!laggedPlayers.isEmpty()) {
                            getLogger().info("Cleaning up lagged proxy {} ({} players)...", s, laggedPlayers.size());
                            for (String laggedPlayer : laggedPlayers) {
                                RedisUtil.cleanUpPlayer(laggedPlayer, jedis);
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
                            if (jedis.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                                // Just clean up the set.
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            RedisUtil.cleanUpPlayer(member, jedis);
                            getLogger().warn("Player found in set that was not found locally and globally: {}", member);
                        } else {
                            jedis.srem("proxy:" + configuration.getServerId() + ":usersOnline", member);
                            getLogger().warn("Player found in set that was not found locally, but is on another proxy: {}", member);
                        }
                    }

                    Pipeline pipeline = jedis.pipelined();

                    for (String player : absentInRedis) {
                        // Player not online according to Redis but not BungeeCord.
                        getLogger().warn("Player {} is on the proxy but not in Redis.", player);

                        Player playerProxied = getProxy().getPlayer(UUID.fromString(player)).orElse(null);
                        if (playerProxied == null)
                            continue; // We'll deal with it later.

                        PlayerUtils.createPlayer(playerProxied, pipeline, true);
                    }

                    pipeline.sync();
                } catch (Throwable e) {
                    getLogger().error("Unable to fix up stored player data", e);
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                try {
                    Set<String> players = getLocalPlayersAsUuidStrings();
                    Set<String> playersInRedis = jedisCluster.smembers("proxy:" + configuration.getServerId() + ":usersOnline");
                    List<String> lagged = getCurrentServerIds(false, true);

                    // Clean up lagged players.
                    for (String s : lagged) {
                        Set<String> laggedPlayers = jedisCluster.smembers("proxy:" + s + ":usersOnline");
                        jedisCluster.del("proxy:" + s + ":usersOnline");
                        if (!laggedPlayers.isEmpty()) {
                            getLogger().info("Cleaning up lagged proxy {} ({} players)...", s, laggedPlayers.size());
                            for (String laggedPlayer : laggedPlayers) {
                                RedisUtil.cleanUpPlayer(laggedPlayer, jedisCluster);
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
                            if (jedisCluster.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                                // Just clean up the set.
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            RedisUtil.cleanUpPlayer(member, jedisCluster);
                            getLogger().warn("Player found in set that was not found locally and globally: {}", member);
                        } else {
                            jedisCluster.srem("proxy:" + configuration.getServerId() + ":usersOnline", member);
                            getLogger().warn("Player found in set that was not found locally, but is on another proxy: {}", member);
                        }
                    }

                    for (String player : absentInRedis) {
                        // Player not online according to Redis but not BungeeCord.
                        getLogger().warn("Player {} is on the proxy but not in Redis.", player);

                        Player playerProxied = getProxy().getPlayer(UUID.fromString(player)).orElse(null);
                        if (playerProxied == null)
                            continue; // We'll deal with it later.

                        PlayerUtils.createPlayer(playerProxied, jedisCluster, true);
                    }
                } catch (Throwable e) {
                    getLogger().error("Unable to fix up stored player data", e);
                }
                return null;
            }
        };
        integrityCheck = getProxy().getScheduler().buildTask(this, integrityCheckRedisTask::execute).repeat(1, TimeUnit.MINUTES).schedule();

        // register plugin messages
        IDENTIFIERS.forEach(getProxy().getChannelRegistrar()::register);

        // register commands
        // Override Velocity commands
        if (configuration.doOverrideBungeeCommands()) {
            getProxy().getCommandManager().register("glist", new RedisBungeeCommands.GlistCommand(this), "redisbungee", "rglist");
        }

        getProxy().getCommandManager().register("sendtoall", new RedisBungeeCommands.SendToAll(this), "rsendtoall");
        getProxy().getCommandManager().register("serverid", new RedisBungeeCommands.ServerId(this), "rserverid");
        getProxy().getCommandManager().register("serverids", new RedisBungeeCommands.ServerIds(this));
        getProxy().getCommandManager().register("pproxy", new RedisBungeeCommands.PlayerProxyCommand(this));
        getProxy().getCommandManager().register("plist", new RedisBungeeCommands.PlistCommand(this), "rplist");
        getProxy().getCommandManager().register("lastseen", new RedisBungeeCommands.LastSeenCommand(this), "rlastseen");
        getProxy().getCommandManager().register("ip", new RedisBungeeCommands.IpCommand(this), "playerip", "rip", "rplayerip");
        getProxy().getCommandManager().register("find", new RedisBungeeCommands.FindCommand(this), "rfind");
    }

    @Override
    public void stop() {
        // Poison the PubSub listener
        if (psl != null) {
            psl.poison();
        }
        if (integrityCheck != null) {
            integrityCheck.cancel();
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        new RedisTask<Void>(api) {
            @Override
            public Void jedisTask(Jedis jedis) {
                jedis.hdel("heartbeats", configuration.getServerId());
                if (jedis.scard("proxy:" + configuration.getServerId() + ":usersOnline") > 0) {
                    Set<String> players = jedis.smembers("proxy:" + configuration.getServerId() + ":usersOnline");
                    for (String member : players)
                        RedisUtil.cleanUpPlayer(member, jedis);
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                jedisCluster.hdel("heartbeats", configuration.getServerId());
                if (jedisCluster.scard("proxy:" + configuration.getServerId() + ":usersOnline") > 0) {
                    Set<String> players = jedisCluster.smembers("proxy:" + configuration.getServerId() + ":usersOnline");
                    for (String member : players)
                        RedisUtil.cleanUpPlayer(member, jedisCluster);
                }
                return null;
            }
        }.execute();
        try {
            this.jedisSummoner.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.httpClient.getDispatcher().getExecutorService().shutdown();
        try {
            this.httpClient.getDispatcher().getExecutorService().awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void loadConfig() throws IOException {
        if (Files.notExists(getDataFolder())) {
            try {
                Files.createDirectory(getDataFolder());
                getLogger().info("data folder was created");
            } catch (IOException e) {
                getLogger().error("Cannot create data folder", e);
            }

        }
        Path file = getDataFolder().resolve("config.yml");
        if (Files.notExists(file)) {
            try (InputStream in = getResourceAsStream("example_config.yml")) {
                Files.createFile(file);
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("config file was created");
            } catch (IOException e) {
                getLogger().error("Cannot create configuration", e);
            }
        }
        final YAMLConfigurationLoader yamlConfiguration = YAMLConfigurationLoader.builder().setPath(file).build();
        ConfigurationNode node = yamlConfiguration.load();
        final String redisServer = node.getNode("redis-server").getString("127.0.0.1");
        final int redisPort = node.getNode("redis-port").getInt(6379);
        final boolean useSSL = node.getNode("useSSL").getBoolean(false);
        String redisPassword = node.getNode("redis-password").getString(null);
        String serverId = node.getNode("server-id").getString("test-1");

        // check redis password
        if (redisPassword != null && (redisPassword.isEmpty() || redisPassword.equals("none"))) {
            redisPassword = null;
            getLogger().warn("INSECURE setup was detected Please set password for your redis instance.");
        }
        if (!useSSL) {
            getLogger().warn("INSECURE setup was detected Please setup ssl for your redis instance.");
        }
        // Configuration sanity checks.
        if (serverId == null || serverId.isEmpty()) {
            /*
             *  this check causes the config comments to disappear somehow
             *  I think due snake yaml limitations so as todo: write our own yaml parser?
             */
            String genId = UUID.randomUUID().toString();
            getLogger().info("Generated server id {} and saving it to config.", genId);
            node.getNode("server-id").setValue(genId);
            yamlConfiguration.save(node);
            getLogger().info("Server id was generated: {}", serverId);
        } else {
            getLogger().info("Loaded server id {}.", serverId);
        }
        try {
            this.configuration = new RedisBungeeConfiguration(serverId, node.getNode("exempt-ip-addresses").getList(TypeToken.of(String.class)), node.getNode("register-bungee-commands").getBoolean(true));
        } catch (ObjectMappingException e) {
            throw new RuntimeException(e);
        }

        if (redisServer != null && !redisServer.isEmpty()) {
            if (node.getNode("cluster-mode-enabled").getBoolean(false)) {
                GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
                poolConfig.setMaxTotal(node.getNode("max-redis-connections").getInt(8));
                if (redisPassword != null) {
                    this.jedisSummoner = new ClusterJedisSummoner(new JedisCluster(new HostAndPort(redisServer, redisPort), 5000, 5000, 60, serverId, redisPassword, poolConfig, useSSL));
                } else {
                    getLogger().warn("SSL option is ignored in Cluster mode if no PASSWORD is set");
                    this.jedisSummoner = new ClusterJedisSummoner(new JedisCluster(new HostAndPort(redisServer, redisPort), 5000, 5000, 60, poolConfig));
                }
                this.redisBungeeMode = RedisBungeeMode.CLUSTER;
                getLogger().info("RedisBungee MODE: CLUSTER");
            } else {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(node.getNode("max-redis-connections").getInt(8));
                this.jedisSummoner = new JedisSummoner(new JedisPool(config, redisServer, redisPort, 0, redisPassword, useSSL));
                this.redisBungeeMode = RedisBungeeMode.SINGLE;
                getLogger().info("RedisBungee MODE: SINGLE");
            }
            getLogger().info("Successfully connected to Redis.");

        } else {
            throw new RuntimeException("No redis server specified!");
        }
    }

    @Override
    public RedisBungeeMode getRedisBungeeMode() {
        return this.redisBungeeMode;
    }

    @Override
    public Long getRedisClusterTime() {
        return getRedisTime((List<String>) this.getRedisClusterTimeScript.eval(Collections.singletonList("0"), Collections.emptyList()));
    }

    @Subscribe
    public void proxyInit(ProxyInitializeEvent event) {
        initialize();
    }

    @Subscribe
    public void proxyShutdownEvent(ProxyShutdownEvent event) {
        stop();
    }


    @Override
    public Class<?> getPubSubEventClass() {
        return PubSubMessageEvent.class;
    }

    @Override
    public Class<?> getNetworkJoinEventClass() {
        return PlayerJoinedNetworkEvent.class;
    }

    @Override
    public Class<?> getServerChangeEventClass() {
        return PlayerChangedServerNetworkEvent.class;
    }

    @Override
    public Class<?> getNetworkQuitEventClass() {
        return PlayerJoinedNetworkEvent.class;
    }


    public ProxyServer getProxy() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataFolder() {
        return this.dataFolder;
    }

    public final InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
}
