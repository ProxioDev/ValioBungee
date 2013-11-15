/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import org.apache.commons.lang3.time.FastDateFormat;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The RedisBungee plugin.
 * <p/>
 * The only function of interest is {@link #getApi()}, which exposes some functions in this class.
 */
public class RedisBungee extends Plugin implements Listener {
    private static final ServerPing.PlayerInfo[] EMPTY_PLAYERINFO = new ServerPing.PlayerInfo[]{};
    private RedisBungeeCommandSender commandSender = new RedisBungeeCommandSender();
    private JedisPool pool;
    private String serverId;
    private List<String> servers = Lists.newArrayList();
    private RedisBungee plugin;
    private static RedisBungeeAPI api;
    private PubSubListener psl;
    private ScheduledTask pubSubTask;
    private boolean canonicalGlist = true;

    /**
     * Fetch the {@link RedisBungeeAPI} object created on plugin start.
     *
     * @return the {@link RedisBungeeAPI} object
     */
    public static RedisBungeeAPI getApi() {
        return api;
    }

    public int getCount() {
        Jedis rsc = pool.getResource();
        int c = 0;
        try {
            c = plugin.getProxy().getOnlineCount();
            for (String i : servers) {
                if (i.equals(serverId)) continue;
                if (rsc.exists("server:" + i + ":playerCount"))
                    c += Integer.valueOf(rsc.get("server:" + i + ":playerCount"));
            }
        } finally {
            pool.returnResource(rsc);
        }
        return c;
    }

    public Set<String> getPlayers() {
        Set<String> players = new HashSet<>();
        for (ProxiedPlayer pp : plugin.getProxy().getPlayers()) {
            players.add(pp.getName());
        }
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                for (String i : servers) {
                    if (i.equals(serverId)) continue;
                    for (String p : rsc.smembers("server:" + i + ":usersOnline")) {
                        if (!players.contains(p)) {
                            players.add(p);
                        }
                    }
                }
            } finally {
                pool.returnResource(rsc);
            }
        }
        return ImmutableSet.copyOf(players);
    }

    public ServerInfo getServerFor(String name) {
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

    public long getLastOnline(String name) {
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

    @Override
    public void onEnable() {
        plugin = this;
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (pool != null) {
            Jedis tmpRsc = pool.getResource();
            try {
                tmpRsc.set("server:" + serverId + ":playerCount", "0"); // reset
                for (String i : tmpRsc.smembers("server:" + serverId + ":usersOnline")) {
                    tmpRsc.srem("server:" + serverId + ":usersOnline", i);
                }
            } finally {
                pool.returnResource(tmpRsc);
            }
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    Jedis rsc = pool.getResource();
                    try {
                        rsc.set("server:" + serverId + ":playerCount", String.valueOf(getProxy().getOnlineCount()));
                    } finally {
                        pool.returnResource(rsc);
                    }
                }
            }, 1, 3, TimeUnit.SECONDS);
            getProxy().getPluginManager().registerCommand(this, new Command("glist", "bungeecord.command.glist", "redisbungee") {
                @Override
                public void execute(CommandSender sender, String[] args) {
                    int count = getCount();
                    if (args.length > 0 && args[0].equals("showall")) {
                        if (canonicalGlist) {
                            Multimap<String, String> serverToPlayers = HashMultimap.create();
                            for (String p : getPlayers()) {
                                ServerInfo si = getServerFor(p);
                                if (si != null)
                                    serverToPlayers.put(si.getName(), p);
                            }
                            if (serverToPlayers.size() == 0) return;
                            Set<String> sortedServers = new TreeSet<>(serverToPlayers.keySet());
                            for (String server : sortedServers)
                                sender.sendMessage(ChatColor.GREEN + "[" + server + "] " + ChatColor.YELLOW + "("
                                        + serverToPlayers.get(server).size() + "): " + ChatColor.WHITE
                                        + Joiner.on(", ").join(serverToPlayers.get(server)));
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Players: " + Joiner.on(", ").join(getPlayers()));
                        }
                        sender.sendMessage(ChatColor.YELLOW + String.valueOf(count) + " player(s) are currently online.");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + String.valueOf(count) + " player(s) are currently online.");
                        sender.sendMessage(ChatColor.YELLOW + "To see all players online, use /glist showall.");
                    }
                }
            });
            getProxy().getPluginManager().registerCommand(this, new Command("find", "bungeecord.command.find") {
                @Override
                public void execute(CommandSender sender, String[] args) {
                    if (args.length > 0) {
                        ServerInfo si = getServerFor(args[0]);
                        if (si != null) {
                            sender.sendMessage(ChatColor.BLUE + args[0] + " is on " + si.getName() + ".");
                        } else {
                            sender.sendMessage(ChatColor.RED + "That user is not online.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You must specify a player name.");
                    }
                }
            });
            getProxy().getPluginManager().registerCommand(this, new Command("lastseen", "redisbungee.command.lastseen") {
                FastDateFormat format = FastDateFormat.getInstance();

                @Override
                public void execute(CommandSender sender, String[] args) {
                    if (args.length > 0) {
                        long secs = getLastOnline(args[0]);
                        if (secs == 0) {
                            sender.sendMessage(ChatColor.GREEN + args[0] + " is currently online.");
                        } else if (secs != -1) {
                            sender.sendMessage(ChatColor.BLUE + args[0] + " was last online on " + format.format(TimeUnit.SECONDS.toMillis(secs)) + ".");
                        } else {
                            sender.sendMessage(ChatColor.RED + args[0] + " has never been online.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You must specify a player name.");
                    }
                }
            });
            getProxy().getPluginManager().registerListener(this, this);
            api = new RedisBungeeAPI(this);
            psl = new PubSubListener();
            pubSubTask = getProxy().getScheduler().runAsync(this, psl);
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
                tmpRsc.set("server:" + serverId + ":playerCount", "0"); // reset
                for (String i : tmpRsc.smembers("server:" + serverId + ":usersOnline")) {
                    tmpRsc.srem("server:" + serverId + ":usersOnline", i);
                }
            } catch (JedisException | ClassCastException ignored) {
            } finally {
                pool.returnResource(tmpRsc);
            }
            pool.destroy();
        }
    }

    private void loadConfig() throws IOException {
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
        try {
            redisServer = ((String) rawYaml.get("redis-server"));
        } catch (NullPointerException ignored) {
        }
        try {
            serverId = ((String) rawYaml.get("server-id"));
        } catch (NullPointerException ignored) {
            serverId = "ImADumbIdi0t";
        }
        try {
            canonicalGlist = ((Boolean) rawYaml.get("canonical-glist"));
        } catch (NullPointerException ignored) {
        }
        List<?> tmp = (List<?>) rawYaml.get("linked-servers");

        if (tmp != null)
            for (Object i : tmp) {
                if (i instanceof String) {
                    servers.add((String) i);
                }
            }

        if (redisServer != null) {
            if (!redisServer.equals("")) {
                pool = new JedisPool(new JedisPoolConfig(), redisServer);
            }
        }
    }

    @EventHandler
    public void onPlayerConnect(final PostLoginEvent event) {
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                rsc.sadd("server:" + serverId + ":usersOnline", event.getPlayer().getName());
                rsc.hset("player:" + event.getPlayer().getName(), "online", "0");
            } finally {
                pool.returnResource(rsc);
            }
            // I used to have a task that eagerly waited for the user to be connected.
            // Well, upon further inspection of BungeeCord's source code, this turned
            // out to not be needed at all, since ServerConnectedEvent is called anyway.
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (pool != null) {
            Jedis rsc = pool.getResource();
            try {
                rsc.srem("server:" + serverId + ":usersOnline", event.getPlayer().getName());
                rsc.hset("player:" + event.getPlayer().getName(), "online", String.valueOf(getUnixTimestamp()));
                rsc.hdel("player:" + event.getPlayer().getName(), "server");
            } finally {
                pool.returnResource(rsc);
            }
        }
    }

    @EventHandler
    public void onServerChange(ServerConnectedEvent event) {
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
        reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), getCount(), EMPTY_PLAYERINFO));
        reply.setDescription(old.getDescription());
        reply.setFavicon(old.getFavicon());
        reply.setVersion(old.getVersion());
        event.setResponse(reply);
    }

    private class PubSubListener implements Runnable {

        private Jedis rsc;
        private JedisPubSubHandler jpsh;

        @Override
        public void run() {
            try {
                rsc = pool.getResource();
                jpsh = new JedisPubSubHandler();
                rsc.subscribe(jpsh, "redisbungee-" + serverId, "redisbungee-allservers");
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
