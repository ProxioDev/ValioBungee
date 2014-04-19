/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The RedisBungee plugin.
 * <p>
 * The only function of interest is {@link #getApi()}, which exposes some functions in this class.
 */
public final class RedisBungee extends Plugin {
    private static Configuration configuration;
    @Getter
    private JedisPool pool;
    private static RedisBungeeAPI api;
    private static PubSubListener psl = null;
    private List<String> serverIds;
    private int globalCount;

    /**
     * Fetch the {@link RedisBungeeAPI} object created on plugin start.
     *
     * @return the {@link RedisBungeeAPI} object
     */
    public static RedisBungeeAPI getApi() {
        return api;
    }

    static Configuration getConfiguration() {
        return configuration;
    }

    final List<String> getServerIds() {
        return serverIds;
    }

    final List<String> getCurrentServerIds() {
        Jedis jedis = pool.getResource();
        try {
            ImmutableList.Builder<String> servers = ImmutableList.builder();
            Map<String, String> heartbeats = jedis.hgetAll("heartbeats");
            for (Map.Entry<String, String> entry : heartbeats.entrySet()) {
                try {
                    long stamp = Long.valueOf(entry.getValue());
                    if (stamp + 30000 < System.currentTimeMillis())
                        servers.add(entry.getKey());
                } catch (NumberFormatException ignored) {
                }
            }
            return servers.build();
        } catch (JedisConnectionException e) {
            getLogger().log(Level.SEVERE, "Unable to fetch all server IDs", e);
            return Collections.singletonList(configuration.getString("server-id"));
        } finally {
            pool.returnResource(jedis);
        }
    }

    static PubSubListener getPubSubListener() {
        return psl;
    }

    final Multimap<String, String> serversToPlayers() {
        ImmutableMultimap.Builder<String, String> multimapBuilder = ImmutableMultimap.builder();
        for (String p : getPlayers()) {
            ServerInfo si = getServerFor(p);
            if (si != null)
                multimapBuilder = multimapBuilder.put(si.getName(), p);
        }
        return multimapBuilder.build();
    }

    final int getCount() {
        return globalCount;
    }

    final int getCurrentCount() {
        int c = getProxy().getOnlineCount();
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String i : getServerIds()) {
                    if (i.equals(configuration.getString("server-id"))) continue;
                    if (rsc.exists("server:" + i + ":playerCount"))
                        try {
                            c += Integer.valueOf(rsc.get("server:" + i + ":playerCount"));
                        } catch (NumberFormatException e) {
                            getLogger().severe("I found a funny number for " + i + "'s player count. Resetting it to 0.");
                            rsc.set("server:" + i + ":playerCount", "0");
                        }
                }
            } catch (JedisConnectionException e) {
                // Redis server has disappeared!
                getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                pool.returnBrokenResource(rsc);
                throw new RuntimeException("Unable to get total player count", e);
            } finally {
                pool.returnResource(rsc);
            }
        }
        return c;
    }

    final Set<String> getLocalPlayers() {
        ImmutableSet.Builder<String> setBuilder = ImmutableSet.builder();
        for (ProxiedPlayer pp : getProxy().getPlayers())
            setBuilder = setBuilder.add(pp.getName());
        return setBuilder.build();
    }

    final Set<String> getPlayers() {
        ImmutableSet.Builder<String> setBuilder = ImmutableSet.<String>builder().addAll(getLocalPlayers());
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String i : getServerIds()) {
                    if (i.equals(configuration.getString("server-id"))) continue;
                    Set<String> users = rsc.smembers("server:" + i + ":usersOnline");
                    if (users != null && !users.isEmpty())
                        setBuilder = setBuilder.addAll(users);
                }
            } catch (JedisConnectionException e) {
                // Redis server has disappeared!
                getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                pool.returnBrokenResource(rsc);
                throw new RuntimeException("Unable to get all players online", e);
            } finally {
                pool.returnResource(rsc);
            }
        }
        return setBuilder.build();
    }

    final Set<String> getPlayersOnServer(@NonNull String server) {
        checkArgument(getProxy().getServerInfo(server) != null, "server doesn't exist");
        return ImmutableSet.copyOf(serversToPlayers().get(server));
    }

    final ServerInfo getServerFor(@NonNull String name) {
        ServerInfo server = null;
        if (getProxy().getPlayer(name) != null) return getProxy().getPlayer(name).getServer().getInfo();
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                if (tmpRsc.hexists("player:" + name, "server"))
                    server = getProxy().getServerInfo(tmpRsc.hget("player:" + name, "server"));
            } catch (JedisConnectionException e) {
                // Redis server has disappeared!
                getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                pool.returnBrokenResource(tmpRsc);
                throw new RuntimeException("Unable to get server for " + name, e);
            } finally {
                pool.returnResource(tmpRsc);
            }
        }
        return server;
    }

    final long getLastOnline(@NonNull String name) {
        long time = -1L;
        if (getProxy().getPlayer(name) != null) return 0;
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                if (tmpRsc.hexists("player:" + name, "online"))
                    try {
                        time = Long.valueOf(tmpRsc.hget("player:" + name, "online"));
                    } catch (NumberFormatException e) {
                        getLogger().info("I found a funny number for when " + name + " was last online!");
                        boolean found = false;
                        for (String proxyId : getServerIds()) {
                            if (proxyId.equals(configuration.getString("server-id"))) continue;
                            if (tmpRsc.sismember("server:" + proxyId + ":usersOnline", name)) {
                                found = true;
                                break;
                            }
                        }
                        String value = "0";
                        if (!found) {
                            value = String.valueOf(System.currentTimeMillis());
                            getLogger().info(name + " isn't online. Setting to current time.");
                        } else {
                            getLogger().info(name + " is online. Setting to 0. Please check your BungeeCord instances.");
                            getLogger().info("If they are working properly, and this error does not resolve in a few minutes, please let Tux know!");
                        }
                        tmpRsc.hset("player:" + name, "online", value);
                    }
            } catch (JedisConnectionException e) {
                // Redis server has disappeared!
                getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                pool.returnBrokenResource(tmpRsc);
                throw new RuntimeException("Unable to get last time online for " + name, e);
            } finally {
                pool.returnResource(tmpRsc);
            }
        }
        return time;
    }

    final InetAddress getIpAddress(@NonNull String name) {
        if (getProxy().getPlayer(name) != null)
            return getProxy().getPlayer(name).getAddress().getAddress();
        InetAddress ia = null;
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                if (tmpRsc.hexists("player:" + name, "ip"))
                    ia = InetAddress.getByName(tmpRsc.hget("player:" + name, "ip"));
            } catch (JedisConnectionException e) {
                // Redis server has disappeared!
                getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
                pool.returnBrokenResource(tmpRsc);
                throw new RuntimeException("Unable to fetch IP address for " + name, e);
            } catch (UnknownHostException ignored) {
                // Best to just return null
            } finally {
                pool.returnResource(tmpRsc);
            }
        }
        return ia;
    }

    final void sendProxyCommand(@NonNull String proxyId, @NonNull String command) {
        checkArgument(getServerIds().contains(proxyId) || proxyId.equals("allservers"), "proxyId is invalid");
        Jedis jedis = pool.getResource();
        try {
            jedis.publish("redisbungee-" + proxyId, command);
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            getLogger().log(Level.SEVERE, "Unable to get connection from pool - did your Redis server go away?", e);
            pool.returnBrokenResource(jedis);
            throw new RuntimeException("Unable to publish command", e);
        } finally {
            pool.returnResource(jedis);
        }
    }

    @Override
    public void onEnable() {
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save config", e);
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                tmpRsc.set("server:" + configuration.getString("server-id") + ":playerCount", "0"); // reset
                tmpRsc.hset("heartbeats", configuration.getString("server-id"), String.valueOf(System.currentTimeMillis()));
                if (tmpRsc.scard("server:" + configuration.getString("server-id") + ":usersOnline") > 0) {
                    for (String member : tmpRsc.smembers("server:" + configuration.getString("server-id") + ":usersOnline")) {
                        // Are they simply on a different proxy?
                        boolean found = false;
                        for (String proxyId : tmpRsc.smembers("servers")) {
                            if (proxyId.equals(configuration.getString("server-id"))) continue;
                            if (tmpRsc.sismember("server:" + proxyId + ":usersOnline", member)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found)
                            RedisUtil.cleanUpPlayer(member, tmpRsc);
                        else
                            tmpRsc.srem("server:" + configuration.getString("server-id") + ":usersOnline", member);
                    }
                }
            } finally {
                pool.returnResource(tmpRsc);
            }
            globalCount = getCurrentCount();
            serverIds = getCurrentServerIds();
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    Jedis rsc = pool.getResource();
                    try {
                        rsc.set("server:" + configuration.getString("server-id") + ":playerCount", String.valueOf(getProxy().getOnlineCount()));
                        rsc.hset("heartbeats", configuration.getString("server-id"), String.valueOf(System.currentTimeMillis()));
                    } catch (JedisConnectionException e) {
                        // Redis server has disappeared!
                        getLogger().log(Level.SEVERE, "Unable to update proxy counts - did your Redis server go away?", e);
                        pool.returnBrokenResource(rsc);
                    } finally {
                        pool.returnResource(rsc);
                    }
                    globalCount = getCurrentCount();
                    serverIds = getCurrentServerIds();
                }
            }, 0, 3, TimeUnit.SECONDS);
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.GlistCommand());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.FindCommand());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.LastSeenCommand());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.IpCommand());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.SendToAll());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerId());
            getProxy().getPluginManager().registerListener(this, new RedisBungeeListener(this));
            api = new RedisBungeeAPI(this);
            psl = new PubSubListener();
            getProxy().getScheduler().runAsync(this, psl);
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    Jedis tmpRsc = pool.getResource();
                    try {
                        Set<String> players = getLocalPlayers();
                        for (String member : tmpRsc.smembers("server:" + configuration.getString("server-id") + ":usersOnline"))
                            if (!players.contains(member)) {
                                // Are they simply on a different proxy?
                                boolean found = false;
                                for (String proxyId : getServerIds()) {
                                    if (proxyId.equals(configuration.getString("server-id"))) continue;
                                    if (tmpRsc.sismember("server:" + proxyId + ":usersOnline", member)) {
                                        // Just clean up the set.
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    RedisUtil.cleanUpPlayer(member, tmpRsc);
                                    getLogger().warning("Player found in set that was not found locally and globally: " + member);
                                } else {
                                    tmpRsc.srem("server:" + configuration.getString("server-id") + ":usersOnline", member);
                                    getLogger().warning("Player found in set that was not found locally, but is on another proxy: " + member);
                                }
                            }
                    } finally {
                        pool.returnResource(tmpRsc);
                    }
                }
            }, 1, 3, TimeUnit.MINUTES);
        }
        getProxy().registerChannel("RedisBungee");
    }

    @Override
    public void onDisable() {
        if (pool != null) {
            // Poison the PubSub listener
            getProxy().getScheduler().cancel(this);
            Jedis tmpRsc = pool.getResource();
            try {
                tmpRsc.set("server:" + configuration.getString("server-id") + ":playerCount", "0"); // reset
                if (tmpRsc.scard("server:" + configuration.getString("server-id") + ":usersOnline") > 0) {
                    for (String member : tmpRsc.smembers("server:" + configuration.getString("server-id") + ":usersOnline"))
                        RedisUtil.cleanUpPlayer(member, tmpRsc);
                }
                tmpRsc.srem("servers", configuration.getString("server-id"));
            } finally {
                pool.returnResource(tmpRsc);
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

        configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

        String redisServer = configuration.getString("redis-server", "localhost");
        int redisPort = configuration.getInt("redis-port", 6379);
        String redisPassword = configuration.getString("redis-password");

        if (redisPassword != null && (redisPassword.equals("") || redisPassword.equals("none"))) {
            redisPassword = null;
        }

        // Configuration sanity checks.
        if (configuration.get("server-id") == null || configuration.getString("server-id").equals("")) {
            throw new RuntimeException("server-id is not specified in the configuration or is empty");
        }

        if (redisServer != null) {
            if (!redisServer.equals("")) {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(configuration.getInt("max-redis-connections", -1));
                pool = new JedisPool(config, redisServer, redisPort, 0, redisPassword);
                // Test the connection
                Jedis rsc = null;
                try {
                    rsc = pool.getResource();
                    rsc.exists(String.valueOf(System.currentTimeMillis()));
                    // If that worked, now we can check for an existing, alive Bungee:
                    File crashFile = new File(getDataFolder(), "restarted_from_crash.txt");
                    if (crashFile.exists())
                        crashFile.delete();
                    else if (rsc.sismember("servers", configuration.getString("server-id"))) {
                        getLogger().severe("You have launched a possible imposter BungeeCord instance. Another instance is already running.");
                        getLogger().severe("For data consistency reasons, RedisBungee will now disable itself.");
                        getLogger().severe("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                        throw new RuntimeException("Possible imposter instance!");
                    }
                    rsc.sadd("servers", configuration.getString("server-id"));
                    getLogger().log(Level.INFO, "Successfully connected to Redis.");
                } catch (JedisConnectionException e) {
                    if (rsc != null)
                        pool.returnBrokenResource(rsc);
                    pool.destroy();
                    pool = null;
                    rsc = null;
                    throw e;
                } finally {
                    if (rsc != null && pool != null) {
                        pool.returnResource(rsc);
                    }
                }
            }
        } else {
            throw new RuntimeException("No redis server specified!");
        }
    }

    class PubSubListener implements Runnable {
        private Jedis rsc;
        private JedisPubSubHandler jpsh;

        private PubSubListener() {
        }

        @Override
        public void run() {
            try {
                rsc = pool.getResource();
                jpsh = new JedisPubSubHandler();
                rsc.subscribe(jpsh, "redisbungee-" + configuration.getString("server-id"), "redisbungee-allservers");
            } catch (JedisException | ClassCastException ignored) {
            }
        }

        public void addChannel(String... channel) {
            jpsh.subscribe(channel);
        }

        public void removeChannel(String... channel) {
            jpsh.unsubscribe(channel);
        }
    }

    class JedisPubSubHandler extends JedisPubSub {
        private ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("RedisBungee PubSub Handler - #%d").build());

        @Override
        public void onMessage(final String s, final String s2) {
            if (s2.trim().length() == 0) return;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    getProxy().getPluginManager().callEvent(new PubSubMessageEvent(s, s2));
                }
            });
        }

        @Override
        public void onPMessage(String s, String s2, String s3) {
        }

        @Override
        public void onSubscribe(String s, int i) {
        }

        @Override
        public void onUnsubscribe(String s, int i) {
        }

        @Override
        public void onPUnsubscribe(String s, int i) {
        }

        @Override
        public void onPSubscribe(String s, int i) {
        }
    }
}
