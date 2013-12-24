/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * The RedisBungee plugin.
 * <p/>
 * The only function of interest is {@link #getApi()}, which exposes some functions in this class.
 */
public final class RedisBungee extends Plugin implements Listener {
    private static final ServerPing.PlayerInfo[] EMPTY_PLAYERINFO = new ServerPing.PlayerInfo[]{};
    private static Configuration configuration;
    private JedisPool pool;
    private RedisBungee plugin;
    private static RedisBungeeAPI api;
    private PubSubListener psl = null;

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

    final int getCount() {
        int c = plugin.getProxy().getOnlineCount();
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String i : configuration.getStringList("linked-servers")) {
                    if (i.equals(configuration.getString("server-id"))) continue;
                    if (rsc.exists("server:" + i + ":playerCount"))
                        c += Integer.valueOf(rsc.get("server:" + i + ":playerCount"));
                }
            } finally {
                pool.returnResource(rsc);
            }
        }
        return c;
    }

    final Set<String> getPlayers() {
        Set<String> players = new HashSet<>();
        for (ProxiedPlayer pp : plugin.getProxy().getPlayers()) {
            players.add(pp.getName());
        }
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String i : configuration.getStringList("linked-servers")) {
                    if (i.equals(configuration.getString("server-id"))) continue;
                    players.addAll(rsc.smembers("server:" + i + ":usersOnline"));
                }
            } finally {
                pool.returnResource(rsc);
            }
        }
        return ImmutableSet.copyOf(players);
    }

    final ServerInfo getServerFor(String name) {
        ServerInfo server = null;
        if (plugin.getProxy().getPlayer(name) != null) return plugin.getProxy().getPlayer(name).getServer().getInfo();
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                if (tmpRsc.hexists("player:" + name, "server"))
                    server = plugin.getProxy().getServerInfo(tmpRsc.hget("player:" + name, "server"));
            } finally {
                pool.returnResource(tmpRsc);
            }
        }
        return server;
    }

    private long getUnixTimestamp() {
        return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    final long getLastOnline(String name) {
        long time = -1L;
        if (plugin.getProxy().getPlayer(name) != null) return 0;
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                if (tmpRsc.hexists("player:" + name, "online"))
                    time = Long.valueOf(tmpRsc.hget("player:" + name, "online"));
            } finally {
                pool.returnResource(tmpRsc);
            }
        }
        return time;
    }

    final InetAddress getIpAddress(String name) {
        if (plugin.getProxy().getPlayer(name) != null)
            return plugin.getProxy().getPlayer(name).getAddress().getAddress();
        InetAddress ia = null;
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                if (tmpRsc.hexists("player:" + name, "ip"))
                    ia = InetAddress.getByName(tmpRsc.hget("player:" + name, "ip"));
            } catch (UnknownHostException ignored) {
                // Best to just return null
            } finally {
                pool.returnResource(tmpRsc);
            }
        }
        return ia;
    }

    @Override
    public void onEnable() {
        plugin = this;
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                tmpRsc.set("server:" + configuration.getString("server-id") + ":playerCount", "0"); // reset
                if (tmpRsc.scard("server:" + configuration.getString("server-id") + ":usersOnline") > 0) {
                    for (String member : tmpRsc.smembers("server:" + configuration.getString("server-id") + ":usersOnline"))
                        cleanUpPlayer(member, tmpRsc);
                }
            } finally {
                pool.returnResource(tmpRsc);
            }
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    Jedis rsc = pool.getResource();
                    try {
                        rsc.set("server:" + configuration.getString("server-id")+ ":playerCount", String.valueOf(getProxy().getOnlineCount()));
                    } finally {
                        pool.returnResource(rsc);
                    }
                }
            }, 1, 3, TimeUnit.SECONDS);
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.GlistCommand());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.FindCommand());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.LastSeenCommand());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.IpCommand());
            getProxy().getPluginManager().registerListener(this, this);
            api = new RedisBungeeAPI(this);
            psl = new PubSubListener();
            psl.start();
        }
    }

    @Override
    public void onDisable() {
        if (pool != null) {
            // Poison the PubSub listener
            psl.poison();
            getProxy().getScheduler().cancel(this);
            Jedis tmpRsc = pool.getResource();
            try {
                tmpRsc.set("server:" + configuration.getString("server-id") + ":playerCount", "0"); // reset
                if (tmpRsc.scard("server:" + configuration.getString("server-id") + ":usersOnline") > 0) {
                    for (String member : tmpRsc.smembers("server:" + configuration.getString("server-id") + ":usersOnline"))
                        cleanUpPlayer(member, tmpRsc);
                }
            } catch (JedisException | ClassCastException ignored) {
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
        if (configuration.getString("server-id").equals("")) {
            throw new RuntimeException("server-id is not specified in the configuration or is empty");
        }

        if (configuration.getStringList("linked-servers").equals(Collections.EMPTY_LIST)) {
            throw new RuntimeException("linked-servers is not specified in the configuration or is empty");
        }

        if (redisServer != null) {
            if (!redisServer.equals("")) {
                pool = new JedisPool(new JedisPoolConfig(), redisServer, redisPort, Protocol.DEFAULT_TIMEOUT, redisPassword);
                // Test the connection
                Jedis rsc = null;
                try {
                    rsc = pool.getResource();
                    rsc.exists(String.valueOf(System.currentTimeMillis()));
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

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String server : configuration.getStringList("linked-servers")) {
                    if (rsc.sismember("server:" + server + ":usersOnline", event.getConnection().getName())) {
                        event.setCancelled(true);
                        event.setCancelReason("You are already logged on to this server.");
                    }
                }
            } finally {
                pool.returnResource(rsc);
            }
        }
    }

    @EventHandler
    public void onPlayerConnect(final PostLoginEvent event) {
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                rsc.sadd("server:" + configuration.getString("server-id", "") + ":usersOnline", event.getPlayer().getName());
                rsc.hset("player:" + event.getPlayer().getName(), "online", "0");
                rsc.hset("player:" + event.getPlayer().getName(), "ip", event.getPlayer().getAddress().getAddress().getHostAddress());
            } finally {
                pool.returnResource(rsc);
            }
        }
        // I used to have a task that eagerly waited for the user to be connected.
        // Well, upon further inspection of BungeeCord's source code, this turned
        // out to not be needed at all, since ServerConnectedEvent is called anyway.
    }

    @EventHandler
    public void onPlayerDisconnect(final PlayerDisconnectEvent event) {
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                rsc.hset("player:" + event.getPlayer().getName(), "online", String.valueOf(getUnixTimestamp()));
                cleanUpPlayer(event.getPlayer().getName(), rsc);
            } finally {
                pool.returnResource(rsc);
            }
        }
    }

    @EventHandler
    public void onServerChange(final ServerConnectedEvent event) {
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                rsc.hset("player:" + event.getPlayer().getName(), "server", event.getServer().getInfo().getName());
            } finally {
                pool.returnResource(rsc);
            }
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing old = event.getResponse();
        ServerPing reply = new ServerPing();
        if (configuration.getBoolean("player-list-in-ping", false)) {
            Set<String> players = getPlayers();
            ServerPing.PlayerInfo[] info = new ServerPing.PlayerInfo[players.size()];
            int idx = 0;
            for (String player : players) {
                info[idx] = new ServerPing.PlayerInfo(player, "");
                idx++;
            }
            reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), players.size(), info));
        } else {
            reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), getCount(), EMPTY_PLAYERINFO));
        }
        reply.setDescription(old.getDescription());
        reply.setFavicon(old.getFavicon());
        reply.setVersion(old.getVersion());
        event.setResponse(reply);
    }

    private void cleanUpPlayer(String player, Jedis rsc) {
        rsc.srem("server:" + configuration.getString("server-id") + ":usersOnline", player);
        rsc.hdel("player:" + player, "server");
        rsc.hdel("player:" + player, "ip");
    }

    private class PubSubListener extends Thread {
        private Jedis rsc;
        private JedisPubSubHandler jpsh;

        private PubSubListener() {
            super("RedisBungee PubSub Listener");
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

        public void poison() {
            jpsh.unsubscribe();
            pool.returnResource(rsc);
        }
    }

    private class JedisPubSubHandler extends JedisPubSub {
        @Override
        public void onMessage(String s, String s2) {
            String cmd;
            if (s2.startsWith("/")) {
                cmd = s2.substring(1);
            } else {
                cmd = s2;
            }
            getLogger().info("Invoking command from PubSub: /" + s2);
            getProxy().getPluginManager().dispatchCommand(RedisBungeeCommandSender.instance, cmd);
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
