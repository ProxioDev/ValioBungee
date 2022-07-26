package com.imaginarycode.minecraft.redisbungee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.tasks.*;
import com.imaginarycode.minecraft.redisbungee.commands.RedisBungeeCommands;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.*;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
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
import redis.clients.jedis.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;


public class RedisBungee extends Plugin implements RedisBungeePlugin<ProxiedPlayer> {

    private static RedisBungeeAPI apiStatic;

    private RedisBungeeAPI api;
    private RedisBungeeMode redisBungeeMode;
    private PubSubListener psl = null;
    private Summoner<?> summoner;
    private UUIDTranslator uuidTranslator;
    private RedisBungeeConfiguration configuration;
    private BungeeDataManager dataManager;
    private OkHttpClient httpClient;
    private volatile List<String> proxiesIds;
    private final AtomicInteger globalPlayerCount = new AtomicInteger();
    private Future<?> integrityCheck;
    private Future<?> heartbeatTask;
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
    public RedisBungeeAPI getRedisBungeeApi() {
        return this.api;
    }

    @Override
    public UUIDTranslator getUuidTranslator() {
        return this.uuidTranslator;
    }

    @Override
    public Multimap<String, UUID> serverToPlayersCache() {
        try {
            return this.serverToPlayersCache.get(SERVER_TO_PLAYERS_KEY, this::serversToPlayers);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getProxiesIds() {
        return proxiesIds;
    }

    @Override
    public PubSubListener getPubSubListener() {
        return this.psl;
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
            loadConfig(this, getDataFolder());
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        }
        // init the api class
        this.api = new RedisBungeeAPI(this);
        apiStatic = this.api;
        // init the http lib
        httpClient = new OkHttpClient();
        Dispatcher dispatcher = new Dispatcher(getExecutorService());
        httpClient.setDispatcher(dispatcher);
        NameFetcher.setHttpClient(httpClient);
        UUIDFetcher.setHttpClient(httpClient);
        InitialUtils.checkRedisVersion(this);
        // check if this proxy is recovering from a crash and start heart the beat.
        InitialUtils.checkIfRecovering(this, getDataFolder().toPath());
        updateProxiesIds();
        uuidTranslator = new UUIDTranslator(this);
        heartbeatTask = service.scheduleAtFixedRate(new HeartbeatTask(this, this.globalPlayerCount), 0, HeartbeatTask.INTERVAL, HeartbeatTask.REPEAT_INTERVAL_TIME_UNIT);
        dataManager = new BungeeDataManager(this);
        getProxy().getPluginManager().registerListener(this, new RedisBungeeBungeeListener(this, configuration.getExemptAddresses()));
        getProxy().getPluginManager().registerListener(this, dataManager);
        psl = new PubSubListener(this);
        getProxy().getScheduler().runAsync(this, psl);

        IntegrityCheckTask integrityCheckTask = new IntegrityCheckTask(this) {
            @Override
            public void handlePlatformPlayer(String player, UnifiedJedis unifiedJedis) {
                ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(UUID.fromString(player));
                if (proxiedPlayer == null)
                    return; // We'll deal with it later.

                BungeePlayerUtils.createPlayer(proxiedPlayer, unifiedJedis, false);
            }
        };

        integrityCheck = service.scheduleAtFixedRate(integrityCheckTask::execute, 0, IntegrityCheckTask.INTERVAL, IntegrityCheckTask.TIMEUNIT);

        // register plugin messages channel.
        getProxy().registerChannel("legacy:redisbungee");
        getProxy().registerChannel("RedisBungee");
        if (configuration.doRegisterLegacyCommands()) {
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
        ShutdownUtils.shutdownCleanup(this);
        try {
            this.summoner.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Summoner<?> getSummoner() {
        return this.summoner;
    }

    @Override
    public RedisBungeeMode getRedisBungeeMode() {
        return this.redisBungeeMode;
    }


    @Override
    public void updateProxiesIds() {
        proxiesIds = getCurrentProxiesIds(false);
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

    @Override
    public void onConfigLoad(RedisBungeeConfiguration configuration, Summoner<?> summoner, RedisBungeeMode mode) {
        this.configuration = configuration;
        this.redisBungeeMode = mode;
        this.summoner = summoner;
    }

    /**
     * This returns an instance of {@link RedisBungeeAPI}
     *
     * @deprecated Please use {@link RedisBungeeAPI#getRedisBungeeApi()} this class intended to for old plugins that no longer updated.
     *
     * @return the {@link RedisBungeeAPI} object instance.
     */
    @Deprecated
    public static RedisBungeeAPI getApi() {
        return apiStatic;
    }

    @Deprecated
    public JedisPool getPool() {
        return api.getJedisPool();
    }
}
