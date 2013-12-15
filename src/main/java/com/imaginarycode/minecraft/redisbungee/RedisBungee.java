/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
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
    private RedisBungeeCommandSender commandSender = new RedisBungeeCommandSender();
    private static RedisBungeeConfiguration configuration = new RedisBungeeConfiguration();
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

    protected static RedisBungeeConfiguration getConfiguration() {
        return configuration;
    }

    protected final int getCount() {
        int c = plugin.getProxy().getOnlineCount();
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String i : getConfiguration().getLinkedServers()) {
                    if (i.equals(configuration.getServerId())) continue;
                    if (rsc.exists("server:" + i + ":playerCount"))
                        c += Integer.valueOf(rsc.get("server:" + i + ":playerCount"));
                }
            } finally {
                pool.returnResource(rsc);
            }
        }
        return c;
    }

    protected final Set<String> getPlayers() {
        Set<String> players = new HashSet<>();
        for (ProxiedPlayer pp : plugin.getProxy().getPlayers()) {
            players.add(pp.getName());
        }
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String i : getConfiguration().getLinkedServers()) {
                    if (i.equals(configuration.getServerId())) continue;
                    players.addAll(rsc.smembers("server:" + i + ":usersOnline"));
                }
            } finally {
                pool.returnResource(rsc);
            }
        }
        return ImmutableSet.copyOf(players);
    }

    protected final ServerInfo getServerFor(String name) {
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

    protected final long getLastOnline(String name) {
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

    protected final InetAddress getIpAddress(String name) {
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
                tmpRsc.set("server:" + configuration.getServerId() + ":playerCount", "0"); // reset
                if (tmpRsc.scard("server:" + configuration.getServerId() + ":usersOnline") > 0) {
                    Set<String> smembers = tmpRsc.smembers("server:" + configuration.getServerId() + ":usersOnline");
                    // Make sure more one time...
                    if (smembers.size() > 0)
                        try {
                            tmpRsc.srem("server:" + configuration.getServerId() + ":usersOnline", smembers.toArray(new String[smembers.size()]));
                        } catch (JedisDataException ignored) {}
                }
            } finally {
                pool.returnResource(tmpRsc);
            }
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    Jedis rsc = pool.getResource();
                    try {
                        rsc.set("server:" + configuration.getServerId() + ":playerCount", String.valueOf(getProxy().getOnlineCount()));
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
                tmpRsc.set("server:" + configuration.getServerId() + ":playerCount", "0"); // reset
                if (tmpRsc.scard("server:" + configuration.getServerId() + ":usersOnline") > 0) {
                    Set<String> smembers = tmpRsc.smembers("server:" + configuration.getServerId() + ":usersOnline");
                    tmpRsc.srem("server:" + configuration.getServerId() + ":usersOnline", smembers.toArray(new String[smembers.size()]));
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

        Yaml yaml = new Yaml();
        Map<?, ?> rawYaml;

        try (InputStream in = new FileInputStream(file)) {
            rawYaml = (Map) yaml.load(in);
        }

        String redisServer = "localhost";
        int redisPort = 6379;
        String redisPassword = null;
        try {
            redisServer = ((String) rawYaml.get("redis-server"));
        } catch (NullPointerException ignored) {
        }
        try {
            redisPort = ((Integer) rawYaml.get("redis-port"));
        } catch (NullPointerException ignored) {
        }
        try {
            redisPassword = ((String) rawYaml.get("redis-password"));
        } catch (NullPointerException ignored) {
        }
        try {
            configuration.setServerId((String) rawYaml.get("server-id"));
        } catch (NullPointerException ignored) {
        }
        try {
            configuration.setCanonicalGlist((Boolean) rawYaml.get("canonical-glist"));
        } catch (NullPointerException ignored) {
        }
        try {
            configuration.setPlayerListInPing(((Boolean) rawYaml.get("player-list-in-ping")));
        } catch (NullPointerException ignored) {
        }
        List<?> tmp = (List<?>) rawYaml.get("linked-servers");

        List<String> servers = new ArrayList<>();
        if (tmp != null)
            for (Object i : tmp) {
                if (i instanceof String) {
                    servers.add((String) i);
                }
            }

        configuration.setLinkedServers(ImmutableList.copyOf(servers));

        if (redisPassword != null && (redisPassword.equals("") || redisPassword.equals("none"))) {
            redisPassword = null;
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
        }
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String server : configuration.getLinkedServers()) {
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
                rsc.sadd("server:" + configuration.getServerId() + ":usersOnline", event.getPlayer().getName());
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
                rsc.srem("server:" + configuration.getServerId() + ":usersOnline", event.getPlayer().getName());
                rsc.hset("player:" + event.getPlayer().getName(), "online", String.valueOf(getUnixTimestamp()));
                rsc.hdel("player:" + event.getPlayer().getName(), "server");
                rsc.hdel("player:" + event.getPlayer().getName(), "ip");
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
        if (configuration.isPlayerListInPing()) {
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
                rsc.subscribe(jpsh, "redisbungee-" + configuration.getServerId(), "redisbungee-allservers");
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
            getProxy().getPluginManager().dispatchCommand(commandSender, cmd);
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
