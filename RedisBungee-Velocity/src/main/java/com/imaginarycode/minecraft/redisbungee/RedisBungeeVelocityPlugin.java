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

import com.google.inject.Inject;
import com.imaginarycode.minecraft.redisbungee.api.PlayerDataManager;
import com.imaginarycode.minecraft.redisbungee.api.ProxyDataManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.config.ConfigLoader;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.util.InitialUtils;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDFetcher;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDTranslator;
import com.imaginarycode.minecraft.redisbungee.commands.RedisBungeeCommands;
import com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import com.velocitypowered.api.event.PostOrder;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Plugin(id = "redisbungee", name = "RedisBungee", version = Constants.VERSION, url = "https://github.com/ProxioDev/RedisBungee", authors = {"astei", "ProxioDev"})
public class RedisBungeeVelocityPlugin implements RedisBungeePlugin<Player>, ConfigLoader {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;
    private final AbstractRedisBungeeAPI api;
    private Summoner<?> jedisSummoner;
    private RedisBungeeMode redisBungeeMode;
    private final UUIDTranslator uuidTranslator;
    private RedisBungeeConfiguration configuration;
    private final OkHttpClient httpClient;

    private final ProxyDataManager proxyDataManager;

    private final VelocityPlayerDataManager playerDataManager;

    private ScheduledTask cleanUpTask;
    private ScheduledTask heartbeatTask;

    public static final List<ChannelIdentifier> IDENTIFIERS = List.of(
            MinecraftChannelIdentifier.create("legacy", "redisbungee"),
            new LegacyChannelIdentifier("RedisBungee"),
            // This is needed for clients before 1.13
            new LegacyChannelIdentifier("legacy:redisbungee")
    );

    @Inject
    public RedisBungeeVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataFolder = dataDirectory;
        logInfo("Version: {}", Constants.VERSION);
        logInfo("Build date: {}", Constants.BUILD_DATE);
        try {
            loadConfig(this, dataDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        this.api = new RedisBungeeAPI(this);
        InitialUtils.checkRedisVersion(this);
        this.proxyDataManager = new ProxyDataManager(this) {
            @Override
            public Set<UUID> getLocalOnlineUUIDs() {
                HashSet<UUID> players = new HashSet<>();
                server.getAllPlayers().forEach(player -> players.add(player.getUniqueId()));
                return players;
            }

            @Override
            protected void handlePlatformCommandExecution(String command) {
                server.getCommandManager().executeAsync(RedisBungeeCommandSource.getSingleton(), command);
            }
        };
        this.playerDataManager = new VelocityPlayerDataManager(this);
        uuidTranslator = new UUIDTranslator(this);
        this.httpClient = new OkHttpClient();
        Dispatcher dispatcher = new Dispatcher(Executors.newFixedThreadPool(6));
        this.httpClient.setDispatcher(dispatcher);
        NameFetcher.setHttpClient(httpClient);
        UUIDFetcher.setHttpClient(httpClient);
    }


    @Override
    public Summoner<?> getSummoner() {
        return this.jedisSummoner;
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
    public PlayerDataManager<Player, ?, ?, ?, ?, ?, ?> playerDataManager() {
        return this.playerDataManager;
    }

    @Override
    public UUIDTranslator getUuidTranslator() {
        return this.uuidTranslator;
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
    public void fireEvent(Object event) {
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
    public void logInfo(String format, Object... object) {
        logger.info(format, object);
    }

    @Override
    public void logWarn(String msg) {
        this.getLogger().warn(msg);
    }

    @Override
    public void logWarn(String format, Object... object) {
        logger.warn(format, object);
    }

    @Override
    public void logFatal(String msg) {
        this.getLogger().error(msg);
    }

    @Override
    public void logFatal(String format, Throwable throwable) {
        logger.error(format, throwable);
    }

    @Override
    public RedisBungeeConfiguration configuration() {
        return this.configuration;
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
    public boolean handlePlatformKick(UUID uuid, Component message) {
        Player player = getPlayer(uuid);
        if (player == null) return false;
        player.disconnect(message);
        return true;
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
    public void initialize() {
        logInfo("Initializing RedisBungee.....");
        ;
        // start heartbeat task
        // heartbeat and clean up
        this.heartbeatTask = server.getScheduler().buildTask(this, this.proxyDataManager::publishHeartbeat).repeat(Duration.ofSeconds(1)).schedule();
        this.cleanUpTask = server.getScheduler().buildTask(this, this.proxyDataManager::correctionTask).repeat(Duration.ofSeconds(60)).schedule();

        server.getEventManager().register(this, this.playerDataManager);
        server.getEventManager().register(this, new RedisBungeeListener(this));

        // subscribe
        server.getScheduler().buildTask(this, this.proxyDataManager).schedule();

        // register plugin messages
        IDENTIFIERS.forEach(getProxy().getChannelRegistrar()::register);

        // register legacy commands
        if (configuration.doRegisterLegacyCommands()) {
            // Override Velocity commands
            getProxy().getCommandManager().register("glist", new RedisBungeeCommands.GlistCommand(this), "redisbungee", "rglist");
            getProxy().getCommandManager().register("sendtoall", new RedisBungeeCommands.SendToAll(this), "rsendtoall");
            getProxy().getCommandManager().register("serverid", new RedisBungeeCommands.ServerId(this), "rserverid");
            getProxy().getCommandManager().register("serverids", new RedisBungeeCommands.ServerIds(this));
            getProxy().getCommandManager().register("pproxy", new RedisBungeeCommands.PlayerProxyCommand(this));
            getProxy().getCommandManager().register("plist", new RedisBungeeCommands.PlistCommand(this), "rplist");
            getProxy().getCommandManager().register("lastseen", new RedisBungeeCommands.LastSeenCommand(this), "rlastseen");
            getProxy().getCommandManager().register("ip", new RedisBungeeCommands.IpCommand(this), "playerip", "rip", "rplayerip");
            getProxy().getCommandManager().register("find", new RedisBungeeCommands.FindCommand(this), "rfind");
        }
        logInfo("RedisBungee initialized successfully ");
    }

    @Override
    public void stop() {
        logInfo("Turning off redis connections.....");
        // Poison the PubSub listener
        if (cleanUpTask != null) {
            cleanUpTask.cancel();
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }


        try {
            this.proxyDataManager.close();
            this.jedisSummoner.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.httpClient.getDispatcher().getExecutorService().shutdown();
        try {
            logInfo("waiting for httpclient thread-pool termination.....");
            this.httpClient.getDispatcher().getExecutorService().awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logInfo("RedisBungee shutdown complete");
    }

    @Override
    public void onConfigLoad(RedisBungeeConfiguration configuration, Summoner<?> summoner, RedisBungeeMode mode) {
        this.jedisSummoner = summoner;
        this.configuration = configuration;
        this.redisBungeeMode = mode;
    }


    @Override
    public RedisBungeeMode getRedisBungeeMode() {
        return this.redisBungeeMode;
    }


    @Subscribe(order = PostOrder.FIRST)
    public void onProxyInitializeEvent(ProxyInitializeEvent event) {
        initialize();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyShutdownEvent(ProxyShutdownEvent event) {
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

    public ProxyServer getProxy() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataFolder() {
        return this.dataFolder;
    }

    public InputStream getResourceAsStream(String name) {
        return this.getClass().getClassLoader().getResourceAsStream(name);
    }
}
