package com.imaginarycode.minecraft.redisbungee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.*;
import com.imaginarycode.minecraft.redisbungee.internal.summoners.JedisSummoner;
import com.imaginarycode.minecraft.redisbungee.internal.summoners.SinglePoolJedisSummoner;
import com.imaginarycode.minecraft.redisbungee.internal.util.IOUtil;
import com.imaginarycode.minecraft.redisbungee.internal.util.LuaManager;
import com.imaginarycode.minecraft.redisbungee.internal.util.uuid.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.internal.util.uuid.UUIDFetcher;
import com.imaginarycode.minecraft.redisbungee.internal.util.uuid.UUIDTranslator;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisConnectionException;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(id = "redisbungee", name = "RedisBungee", version = RBUtils.VERSION, url = "https://github.com/ProxioDev/RedisBungee", authors = "ProxioDev")
public class RedisBungeeVelocityPlugin implements RedisBungeePlugin<Player> {

    private static final Gson gson = new Gson();
    private final ProxyServer server;
    private final Logger logger;
    private final File dataFolder;
    private RedisBungeeAPI api;
    private PubSubListener psl = null;
    private JedisSummoner jedisSummoner;
    private UUIDTranslator uuidTranslator;
    private RedisBungeeConfiguration configuration;
    private VelocityDataManager dataManager;
    private OkHttpClient httpClient;
    private volatile List<String> serverIds;
    private final AtomicInteger nagAboutServers = new AtomicInteger();
    private final AtomicInteger globalPlayerCount = new AtomicInteger();
    private ScheduledTask integrityCheck;
    private ScheduledTask heartbeatTask;
    private LuaManager.Script serverToPlayersScript;
    private LuaManager.Script getPlayerCountScript;

    private static final Object SERVER_TO_PLAYERS_KEY = new Object();
    private final Cache<Object, Multimap<String, UUID>> serverToPlayersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();


    @Inject
    public RedisBungeeVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataFolder = dataDirectory.toFile();

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
        Long count = (Long) getPlayerCountScript.eval(ImmutableList.<String>of(), ImmutableList.<String>of());
        return count.intValue();
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
        ImmutableSet.Builder<UUID> setBuilder = ImmutableSet.builder();
        if (isJedisAvailable()) {
            try (Jedis rsc = requestJedis()) {
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
                getLogger().error("Unable to get connection from pool - did your Redis server go away?", e);
                throw new RuntimeException("Unable to get all players online", e);
            }
        }
        return setBuilder.build();
    }

    @Override
    public Jedis requestJedis() {
        return this.jedisSummoner.requestJedis();
    }

    @Override
    public boolean isJedisAvailable() {
        return this.jedisSummoner.isJedisAvailable();
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

    @Override
    public Set<UUID> getPlayersOnProxy(String proxyId) {
        checkArgument(getServerIds().contains(proxyId), proxyId + " is not a valid proxy ID");
        try (Jedis jedis = requestJedis()) {
            Set<String> users = jedis.smembers("proxy:" + proxyId + ":usersOnline");
            ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();
            for (String user : users) {
                builder.add(UUID.fromString(user));
            }
            return builder.build();
        }
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
        try (Jedis jedis = requestJedis()) {
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
                        getLogger().warn(entry.getKey() + " is " + (time - stamp) + " seconds behind! (Time not synchronized or server down?) and was removed from heartbeat.");
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
    public PubSubListener getPubSubListener() {
        return this.psl;
    }

    @Override
    public void sendChannelMessage(String channel, String message) {
        try (Jedis jedis = requestJedis()) {
            jedis.publish(channel, message);
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            getLogger().error("Unable to get connection from pool - did your Redis server go away?", e);
            throw new RuntimeException("Unable to publish channel message", e);
        }
    }

    @Override
    public void executeAsync(Runnable runnable) {
        this.getProxy().getScheduler().buildTask(this, runnable).schedule();
    }

    @Override
    public void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int time) {
        this.getProxy().getScheduler().buildTask(this, runnable).delay(time, timeUnit);
    }

    @Override
    public void callEvent(Object event) {
        this.getProxy().getEventManager().fire(event);
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
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        this.api = new RedisBungeeAPI(this);
        // call old plugin class to support old plugins
        new RedisBungee(api);
        if (isJedisAvailable()) {
            try (Jedis tmpRsc = requestJedis()) {
                // This is more portable than INFO <section>
                String info = tmpRsc.info();
                for (String s : info.split("\r\n")) {
                    if (s.startsWith("redis_version:")) {
                        String version = s.split(":")[1];
                        getLogger().info(version + " <- redis version");
                        if (!RedisUtil.isRedisVersionRight(version)) {
                            getLogger().warn("Your version of Redis (" + version + ") is not at least version 6.0 RedisBungee requires a newer version of Redis.");
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
            heartbeatTask = getProxy().getScheduler().buildTask(this, new Runnable() {
                @Override
                public void run() {
                    try (Jedis rsc = requestJedis()) {
                        long redisTime = getRedisTime(rsc.time());
                        rsc.hset("heartbeats", configuration.getServerId(), String.valueOf(redisTime));
                    } catch (JedisConnectionException e) {
                        // Redis server has disappeared!
                        getLogger().error("Unable to update heartbeat - did your Redis server go away?", e);
                        return;
                    }
                    try {
                        serverIds = getCurrentServerIds(true, false);
                        globalPlayerCount.set(getCurrentCount());
                    } catch (Throwable e) {
                        getLogger().error("Unable to update data - did your Redis server go away?", e);
                    }
                }
            }).repeat(3, TimeUnit.SECONDS).schedule();
            dataManager = new VelocityDataManager(this);
            getProxy().getEventManager().register(this, new RedisBungeeListener(this, configuration.getExemptAddresses()));
            getProxy().getEventManager().register(this, dataManager);
            psl = new PubSubListener(this);
            getProxy().getScheduler().buildTask(this, psl).schedule();
            integrityCheck = getProxy().getScheduler().buildTask(this,new Runnable() {
                @Override
                public void run() {
                    try (Jedis tmpRsc = requestJedis()) {
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
                                getLogger().warn("Player found in set that was not found locally and globally: " + member);
                            } else {
                                tmpRsc.srem("proxy:" + configuration.getServerId() + ":usersOnline", member);
                                getLogger().warn("Player found in set that was not found locally, but is on another proxy: " + member);
                            }
                        }

                        Pipeline pipeline = tmpRsc.pipelined();

                        for (String player : absentInRedis) {
                            // Player not online according to Redis but not BungeeCord.
                            getLogger().warn("Player " + player + " is on the proxy but not in Redis.");

                            Player playerProxied = getProxy().getPlayer(UUID.fromString(player)).orElse(null);
                            if (playerProxied == null)
                                continue; // We'll deal with it later.

                            RBUtils.createPlayer(playerProxied, pipeline, true);
                        }

                        pipeline.sync();
                    } catch (Throwable e) {
                        getLogger().error("Unable to fix up stored player data", e);
                    }
                }
            }).repeat(1, TimeUnit.MINUTES).schedule();
        }
        // plugin messages are disabled for now
        //getProxy().registerChannel("legacy:redisbungee");
        //getProxy().registerChannel("RedisBungee");

        // register commands
        // those still bungeecord commands will migrate them later.
        // if (configuration.doOverrideBungeeCommands()) {
        //     getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.GlistCommand(this));
        //     getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.FindCommand(this));
        //     getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.LastSeenCommand(this));
        //     getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.IpCommand(this));
        //
        // }
        // getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.SendToAll(this));
        // getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerId(this));
        // getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerIds(this));
        // getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.PlayerProxyCommand(this));
        // getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.PlistCommand(this));
    }

    @Override
    public void stop() {
        if (isJedisAvailable()) {
            // Poison the PubSub listener
            psl.poison();
            integrityCheck.cancel();
            heartbeatTask.cancel();
            try (Jedis tmpRsc = requestJedis()) {
                tmpRsc.hdel("heartbeats", configuration.getServerId());
                if (tmpRsc.scard("proxy:" + configuration.getServerId() + ":usersOnline") > 0) {
                    Set<String> players = tmpRsc.smembers("proxy:" + configuration.getServerId() + ":usersOnline");
                    for (String member : players)
                        RedisUtil.cleanUpPlayer(member, tmpRsc);
                }
            }
            try {
                this.jedisSummoner.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            file.createNewFile();
            try (InputStream in = getResourceAsStream("example_config.yml");
                 OutputStream out = Files.newOutputStream(file.toPath())) {
                ByteStreams.copy(in, out);
            }
        }

        final YAMLConfigurationLoader yamlConfiguration = YAMLConfigurationLoader.builder().setFile(file).build();

        ConfigurationNode node = yamlConfiguration.load();
        final String redisServer = node.getNode("redis-server").getString();
        final int redisPort = node.getNode("redis-port").getInt();
        final boolean useSSL = node.getNode("useSSL").getBoolean();
        String redisPassword = node.getNode("redis-password").getString();
        String serverId =  node.getNode("server-id").getString();

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
            getLogger().info("Generated server id " + genId + " and saving it to config.");
            node.getNode("server-id").setValue(genId);
            yamlConfiguration.save(node);
            getLogger().info("Server id was generated: " + serverId);
        } else {
            getLogger().info("Loaded server id " + serverId + '.');
        }
        try {
            this.configuration = new RedisBungeeConfiguration(serverId, node.getNode("exempt-ip-addresses").getList(TypeToken.of(String.class)), node.getNode("register-bungee-commands").getBoolean());
        } catch (ObjectMappingException e) {
            throw new RuntimeException(e);
        }

        if (redisServer != null && !redisServer.isEmpty()) {
            try {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(node.getNode("max-redis-connections").getInt());
                this.jedisSummoner = new SinglePoolJedisSummoner(new JedisPool(config, redisServer, redisPort, 0, redisPassword, useSSL));

            } catch (JedisConnectionException e) {
                throw new RuntimeException("Unable to create Redis pool", e);
            }

            // Test the connection
            try (Jedis rsc = requestJedis()) {
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
                            getLogger().error("You have launched a possible impostor Velocity / Bungeecord instance. Another instance is already running.");
                            getLogger().error("For data consistency reasons, RedisBungee will now disable itself.");
                            getLogger().error("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }


                httpClient = new OkHttpClient();
                Dispatcher dispatcher = new Dispatcher(Executors.newFixedThreadPool(6));
                httpClient.setDispatcher(dispatcher);
                NameFetcher.setHttpClient(httpClient);
                UUIDFetcher.setHttpClient(httpClient);

                getLogger().info("Successfully connected to Redis.");
            } catch (JedisConnectionException e) {
                this.jedisSummoner.close();
                throw e;
            }
        } else {
            throw new RuntimeException("No redis server specified!");
        }
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

    public File getDataFolder() {
        return this.dataFolder;
    }

    public final InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
}
