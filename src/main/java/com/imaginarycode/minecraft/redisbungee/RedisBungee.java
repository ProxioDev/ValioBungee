/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
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
import net.md_5.bungee.event.EventHandler;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedisBungee extends Plugin implements Listener {
    private static JedisPool pool;
    private static String serverId;
    private static List<String> servers = Lists.newArrayList();
    private static RedisBungee plugin;
    private boolean canonicalGlist = true;

    protected String getServerId() {
        return serverId;
    }

    protected List<String> getServers() {
        return servers;
    }

    /**
     * Get a combined count of all players on this network.
     *
     * @return a count of all players found
     */
    public static int getCount() {
        Jedis rsc = pool.getResource();
        int c = 0;
        try {
            c = plugin.getProxy().getOnlineCount();
            for (String i : plugin.getServers()) {
                if (i.equals(plugin.getServerId())) continue;
                if (rsc.exists("server:" + i + ":playerCount"))
                    c += Integer.valueOf(rsc.get("server:" + i + ":playerCount"));
            }
        } finally {
            pool.returnResource(rsc);
        }
        return c;
    }

    /**
     * Get a combined list of players on this network.
     * <p/>
     * Note that this function returns an immutable {@link java.util.Set}.
     *
     * @return a Set with all players found
     */
    public static Set<String> getPlayers() {
        List<String> players = Lists.newArrayList();
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

    /**
     * Get the server where the specified player is playing. This function also deals with the case of local players
     * as well, and will return local information on them.
     *
     * @param name a player name
     * @return a {@link net.md_5.bungee.api.config.ServerInfo} for the server the player is on.
     */
    public static ServerInfo getServerFor(String name) {
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

    private static long getUnixTimestamp() {
        return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Get the last time a player was on. If the player is currently online, this will return 0, otherwise it will return
     * a value in seconds.
     *
     * @param name a player name
     * @return the last time a player was on, if online returns a 0
     */
    public static long getLastOnline(String name) {
        long time = 0L;
        if (plugin.getProxy().getPlayer(name) != null) return time;
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
                        rsc.set("server:" + plugin.getServerId() + ":playerCount", String.valueOf(getProxy().getOnlineCount()));
                    } finally {
                        pool.returnResource(rsc);
                    }
                }
            }, 1, 3, TimeUnit.SECONDS);
            getProxy().getPluginManager().registerCommand(this, new Command("glist", "bungeecord.command.glist", "redisbungee") {
                @Override
                public void execute(CommandSender sender, String[] args) {
                    sender.sendMessage(ChatColor.YELLOW + String.valueOf(getCount()) + " player(s) are currently online.");
                    if (args.length > 0 && args[0].equals("showall")) {
                        if (canonicalGlist) {
                            Multimap<String, String> serverToPlayers = HashMultimap.create(getProxy().getServers().size(), getCount());
                            for (String p : getPlayers()) {
                                ServerInfo si = getServerFor(p);
                                if (si != null)
                                    serverToPlayers.put(si.getName(), p);
                            }
                            if (serverToPlayers.size() == 0) return;
                            List<String> sortedServers = new ArrayList<>(serverToPlayers.keySet());
                            Collections.sort(sortedServers);
                            for (String server : sortedServers)
                                sender.sendMessage(ChatColor.GREEN + "[" + server + "] " + ChatColor.YELLOW + "("
                                        + serverToPlayers.get(server).size() + "): " + ChatColor.WHITE
                                        + Joiner.on(", ").join(serverToPlayers.get(server)));
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Players: " + Joiner.on(", ").join(getPlayers()));
                        }
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "To see all players online, use /glist showall.");
                    }
                }
            });
            getProxy().getPluginManager().registerListener(this, this);
        }
    }

    @Override
    public void onDisable() {
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
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }

        Yaml yaml = new Yaml();
        Map rawYaml;

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
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    Jedis rsc = pool.getResource();
                    try {
                        rsc.hset("player:" + event.getPlayer().getName(), "server", event.getPlayer().getServer().getInfo().getName());
                    } finally {
                        pool.returnResource(rsc);
                    }
                }
            }, 1750, TimeUnit.MILLISECONDS);
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
        reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), getCount(), new ServerPing.PlayerInfo[]{}));
        reply.setDescription(old.getDescription());
        reply.setFavicon(old.getFavicon());
        reply.setVersion(old.getVersion());
        event.setResponse(reply);
    }
}
