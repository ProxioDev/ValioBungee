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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.imaginarycode.minecraft.redisbungee.api.*;
import com.imaginarycode.minecraft.redisbungee.api.config.ConfigLoader;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerChangedServerNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerJoinedNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerLeftNetworkEvent;
import com.imaginarycode.minecraft.redisbungee.api.events.IPubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.tasks.*;
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
import org.slf4j.Logger;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;


import java.io.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(id = "redisbungee", name = "RedisBungee", version = PomData.VERSION, url = "https://github.com/ProxioDev/RedisBungee", authors = {"astei", "ProxioDev"})
public class RedisBungeeVelocityPlugin implements RedisBungeePlugin<Player>, ConfigLoader {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;
    private final AbstractRedisBungeeAPI api;
    private final PubSubListener psl;
    private Summoner<?> jedisSummoner;
    private RedisBungeeMode redisBungeeMode;
    private final UUIDTranslator uuidTranslator;
    private RedisBungeeConfiguration configuration;
    private final VelocityDataManager dataManager;
    private final OkHttpClient httpClient;
    private volatile List<String> proxiesIds;
    private final AtomicInteger globalPlayerCount = new AtomicInteger();
    private ScheduledTask integrityCheck;
    private ScheduledTask heartbeatTask;

    private static final Object SERVER_TO_PLAYERS_KEY = new Object();
    public static final List<ChannelIdentifier> IDENTIFIERS = List.of(
            MinecraftChannelIdentifier.create("legacy", "redisbungee"),
            new LegacyChannelIdentifier("RedisBungee"),
            // This is needed for clients before 1.13
            new LegacyChannelIdentifier("legacy:redisbungee")
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
            loadConfig(this, dataDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        this.api = new RedisBungeeAPI(this);
        InitialUtils.checkRedisVersion(this);
        // check if this proxy is recovering from a crash and start heart the beat.
        InitialUtils.checkIfRecovering(this, getDataFolder());
        uuidTranslator = new UUIDTranslator(this);
        dataManager = new VelocityDataManager(this);
        psl = new PubSubListener(this);
        this.httpClient = new OkHttpClient();
        Dispatcher dispatcher = new Dispatcher(Executors.newFixedThreadPool(6));
        this.httpClient.setDispatcher(dispatcher);
        NameFetcher.setHttpClient(httpClient);
        UUIDFetcher.setHttpClient(httpClient);
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
    public Summoner<?> getSummoner() {
        return this.jedisSummoner;
    }

    @Override
    public AbstractRedisBungeeAPI getAbstractRedisBungeeApi() {
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
    public void initialize() {
        updateProxiesIds();
        // start heartbeat task
        heartbeatTask = getProxy().getScheduler().buildTask(this, new HeartbeatTask(this, this.globalPlayerCount)).repeat(HeartbeatTask.INTERVAL, HeartbeatTask.REPEAT_INTERVAL_TIME_UNIT).schedule();

        getProxy().getEventManager().register(this, new RedisBungeeVelocityListener(this, configuration.getExemptAddresses()));
        getProxy().getEventManager().register(this, dataManager);
        getProxy().getScheduler().buildTask(this, psl).schedule();

        IntegrityCheckTask integrityCheckTask = new IntegrityCheckTask(this) {
            @Override
            public void handlePlatformPlayer(String player, UnifiedJedis unifiedJedis) {
                Player playerProxied = getProxy().getPlayer(UUID.fromString(player)).orElse(null);
                if (playerProxied == null)
                    return; // We'll deal with it later.
                VelocityPlayerUtils.createVelocityPlayer(playerProxied, unifiedJedis, false);
            }
        };
        integrityCheck = getProxy().getScheduler().buildTask(this, integrityCheckTask::execute).repeat(30, TimeUnit.SECONDS).schedule();


        // register plugin messages
        IDENTIFIERS.forEach(getProxy().getChannelRegistrar()::register);

        // register legacy commands
        if (configuration.doRegisterLegacyCommands()) {
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
        ShutdownUtils.shutdownCleanup(this);
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
    public void onConfigLoad(RedisBungeeConfiguration configuration, Summoner<?> summoner, RedisBungeeMode mode) {
        this.jedisSummoner = summoner;
        this.configuration = configuration;
        this.redisBungeeMode = mode;
    }


    @Override
    public RedisBungeeMode getRedisBungeeMode() {
        return this.redisBungeeMode;
    }

    @Override
    public void updateProxiesIds() {
        this.proxiesIds = this.getCurrentProxiesIds(false);
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
