/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee;

import co.aikar.commands.BungeeCommandManager;
import com.imaginarycode.minecraft.redisbungee.api.PlayerDataManager;
import com.imaginarycode.minecraft.redisbungee.api.ProxyDataManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.config.LangConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.config.loaders.ConfigLoader;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.config.loaders.LangConfigLoader;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.util.InitialUtils;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDTranslator;
import com.imaginarycode.minecraft.redisbungee.commands.CommandLoader;
import com.imaginarycode.minecraft.redisbungee.commands.utils.CommandPlatformHelper;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.sql.Date;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;


public class RedisBungee extends Plugin implements RedisBungeePlugin<ProxiedPlayer>, ConfigLoader, LangConfigLoader {

    private static RedisBungeeAPI apiStatic;
    private RedisBungeeAPI api;
    private RedisBungeeMode redisBungeeMode;
    private ProxyDataManager proxyDataManager;
    private BungeePlayerDataManager playerDataManager;
    private ScheduledTask heartbeatTask;
    private ScheduledTask cleanupTask;
    private Summoner<?> summoner;
    private UUIDTranslator uuidTranslator;
    private RedisBungeeConfiguration configuration;
    private LangConfiguration langConfiguration;
    private OkHttpClient httpClient;
    private BungeeCommandManager commandManager;

    private final Logger logger = LoggerFactory.getLogger("RedisBungee");


    @Override
    public RedisBungeeConfiguration configuration() {
        return this.configuration;
    }

    @Override
    public LangConfiguration langConfiguration() {
        return this.langConfiguration;
    }

    @Override
    public AbstractRedisBungeeAPI getAbstractRedisBungeeApi() {
        return this.api;
    }

    @Override
    public ProxyDataManager proxyDataManager() {
        return this.proxyDataManager;
    }

    @Override
    public PlayerDataManager<ProxiedPlayer, ?, ?, ?, ?, ?, ?> playerDataManager() {
        return this.playerDataManager;
    }

    @Override
    public UUIDTranslator getUuidTranslator() {
        return this.uuidTranslator;
    }

    @Override
    public void fireEvent(Object event) {
        this.getProxy().getPluginManager().callEvent((Event) event);
    }

    @Override
    public boolean isOnlineMode() {
        return this.getProxy().getConfig().isOnlineMode();
    }

    @Override
    public void logInfo(String msg) {
        this.logger.info(msg);
    }

    @Override
    public void logInfo(String format, Object... object) {
        this.logger.info(format, object);
    }

    @Override
    public void logWarn(String msg) {
        this.logger.warn(msg);
    }

    @Override
    public void logWarn(String format, Object... object) {
        this.logger.warn(format, object);
    }

    @Override
    public void logFatal(String msg) {
        this.logger.error(msg);
    }

    @Override
    public void logFatal(String format, Throwable throwable) {
        this.logger.error(format, throwable);
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
    public boolean handlePlatformKick(UUID uuid, Component message) {
        ProxiedPlayer player = getPlayer(uuid);
        if (player == null) return false;
        if (!player.isConnected()) return false;
        player.disconnect(BungeeComponentSerializer.get().serialize(message));
        return true;
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
        logInfo("Initializing RedisBungee.....");
        logInfo("Version: {}", Constants.VERSION);
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
            getLogger().log(Level.WARNING, "skipping replacement.....");
        }
        try {
            loadConfig(this, getDataFolder().toPath());
            loadLangConfig(this, getDataFolder().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        }
        // init the proxy data manager
        this.proxyDataManager = new ProxyDataManager(this) {
            @Override
            public Set<UUID> getLocalOnlineUUIDs() {
                HashSet<UUID> uuids = new HashSet<>();
                ProxyServer.getInstance().getPlayers().forEach((proxiedPlayer) -> uuids.add(proxiedPlayer.getUniqueId()));
                return uuids;
            }

            @Override
            protected void handlePlatformCommandExecution(String command) {
                logInfo("Dispatching {}", command);
                ProxyServer.getInstance().getPluginManager().dispatchCommand(RedisBungeeCommandSender.getSingleton(), command);
            }
        };
        this.playerDataManager = new BungeePlayerDataManager(this);

        getProxy().getPluginManager().registerListener(this, this.playerDataManager);
        getProxy().getPluginManager().registerListener(this, new RedisBungeeListener(this));
        // start listening
        getProxy().getScheduler().runAsync(this, proxyDataManager);
        // heartbeat
        this.heartbeatTask = getProxy().getScheduler().schedule(this, () -> this.proxyDataManager.publishHeartbeat(), 0, 1, TimeUnit.SECONDS);
        // cleanup
        this.cleanupTask = getProxy().getScheduler().schedule(this, () -> this.proxyDataManager.correctionTask(), 0, 60, TimeUnit.SECONDS);
        // init the http lib
        httpClient = new OkHttpClient();
        Dispatcher dispatcher = new Dispatcher(getExecutorService());
        httpClient.setDispatcher(dispatcher);
        NameFetcher.setHttpClient(httpClient);
        UUIDFetcher.setHttpClient(httpClient);
        InitialUtils.checkRedisVersion(this);
        uuidTranslator = new UUIDTranslator(this);

        // register plugin messages channel.
        getProxy().registerChannel("legacy:redisbungee");
        getProxy().registerChannel("RedisBungee");

        // init the api
        this.api = new RedisBungeeAPI(this);
        apiStatic = (RedisBungeeAPI) this.api;

        // commands
        CommandPlatformHelper.init(new BungeeCommandPlatformHelper());
        this.commandManager = new BungeeCommandManager(this);
        CommandLoader.initCommands(this.commandManager, this);

        logInfo("RedisBungee initialized successfully ");
    }

    @Override
    public void stop() {
        logInfo("Turning off redis connections.....");
        getProxy().getPluginManager().unregisterListeners(this);

        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        try {
            this.proxyDataManager.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            this.summoner.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (this.commandManager != null) {
            this.commandManager.unregisterCommands();
        }
        logInfo("RedisBungee shutdown successfully");
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
    public void executeAsync(Runnable runnable) {
        this.getProxy().getScheduler().runAsync(this, runnable);
    }

    @Override
    public void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int time) {
        this.getProxy().getScheduler().schedule(this, runnable, time, timeUnit);
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
    public IPlayerChangedServerNetworkEvent createPlayerChangedServerNetworkEvent(UUID uuid, String previousServer, String server) {
        return new PlayerChangedServerNetworkEvent(uuid, previousServer, server);
    }

    @Override
    public IPlayerJoinedNetworkEvent createPlayerJoinedNetworkEvent(UUID uuid) {
        return new PlayerJoinedNetworkEvent(uuid);
    }

    @Override
    public IPlayerLeftNetworkEvent createPlayerLeftNetworkEvent(UUID uuid) {
        return new PlayerLeftNetworkEvent(uuid);
    }

    @Override
    public IPubSubMessageEvent createPubSubEvent(String channel, String message) {
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
     * @return the {@link AbstractRedisBungeeAPI} object instance.
     * @deprecated Please use {@link RedisBungeeAPI#getRedisBungeeApi()} this class intended to for old plugins that no longer updated.
     */
    @Deprecated
    public static RedisBungeeAPI getApi() {
        return apiStatic;
    }

    @Deprecated
    public JedisPool getPool() {
        return api.getJedisPool();
    }


    @Override
    public void onLangConfigLoad(LangConfiguration langConfiguration) {
        this.langConfiguration = langConfiguration;
    }
}
