package com.imaginarycode.minecraft.redisbungee.api.tasks;

import com.imaginarycode.minecraft.redisbungee.api.util.player.PlayerUtils;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class IntegrityCheckTask extends RedisTask<Void> {

    public static int INTERVAL = 30;
    public static TimeUnit TIMEUNIT = TimeUnit.SECONDS;


    public IntegrityCheckTask(RedisBungeePlugin<?> plugin) {
        super(plugin);
    }

    @Override
    public Void jedisTask(Jedis jedis) {
        try {
            Set<String> players = plugin.getLocalPlayersAsUuidStrings();
            Set<String> playersInRedis = jedis.smembers("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline");
            List<String> lagged = plugin.getCurrentProxiesIds(true);

            // Clean up lagged players.
            for (String s : lagged) {
                Set<String> laggedPlayers = jedis.smembers("proxy:" + s + ":usersOnline");
                jedis.del("proxy:" + s + ":usersOnline");
                if (!laggedPlayers.isEmpty()) {
                    plugin.logInfo("Cleaning up lagged proxy " + s + " (" + laggedPlayers.size() + " players)...");
                    for (String laggedPlayer : laggedPlayers) {
                        PlayerUtils.cleanUpPlayer(laggedPlayer, jedis, true);
                    }
                }
            }

            Set<String> absentLocally = new HashSet<>(playersInRedis);
            absentLocally.removeAll(players);
            Set<String> absentInRedis = new HashSet<>(players);
            absentInRedis.removeAll(playersInRedis);

            for (String member : absentLocally) {
                boolean found = false;
                for (String proxyId : plugin.getProxiesIds()) {
                    if (proxyId.equals(plugin.getConfiguration().getProxyId())) continue;
                    if (jedis.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                        // Just clean up the set.
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    PlayerUtils.cleanUpPlayer(member, jedis, false);
                   plugin.logWarn("Player found in set that was not found locally and globally: " + member);
                } else {
                    jedis.srem("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline", member);
                    plugin.logWarn("Player found in set that was not found locally, but is on another proxy: " + member);
                }
            }

            Pipeline pipeline = jedis.pipelined();

            for (String player : absentInRedis) {
                // Player not online according to Redis but not BungeeCord.
                plugin.logWarn("Player " + player + " is on the proxy but not in Redis.");
                handlePlatformPlayer(player, pipeline);
            }

            pipeline.sync();
        } catch (Throwable e) {
            plugin.logFatal("Unable to fix up stored player data");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Void clusterJedisTask(JedisCluster jedisCluster) {
        try {
            Set<String> players = plugin.getLocalPlayersAsUuidStrings();
            Set<String> playersInRedis = jedisCluster.smembers("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline");
            List<String> lagged = plugin.getCurrentProxiesIds(true);

            // Clean up lagged players.
            for (String s : lagged) {
                Set<String> laggedPlayers = jedisCluster.smembers("proxy:" + s + ":usersOnline");
                jedisCluster.del("proxy:" + s + ":usersOnline");
                if (!laggedPlayers.isEmpty()) {
                    plugin.logInfo("Cleaning up lagged proxy " + s + " (" + laggedPlayers.size() + " players)...");
                    for (String laggedPlayer : laggedPlayers) {
                        PlayerUtils.cleanUpPlayer(laggedPlayer, jedisCluster, true);
                    }
                }
            }

            Set<String> absentLocally = new HashSet<>(playersInRedis);
            absentLocally.removeAll(players);
            Set<String> absentInRedis = new HashSet<>(players);
            absentInRedis.removeAll(playersInRedis);

            for (String member : absentLocally) {
                boolean found = false;
                for (String proxyId : plugin.getProxiesIds()) {
                    if (proxyId.equals(plugin.getConfiguration().getProxyId())) continue;
                    if (jedisCluster.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                        // Just clean up the set.
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    PlayerUtils.cleanUpPlayer(member, jedisCluster, false);
                    plugin.logWarn("Player found in set that was not found locally and globally: " + member);
                } else {
                    jedisCluster.srem("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline", member);
                    plugin.logWarn("Player found in set that was not found locally, but is on another proxy: " + member);
                }
            }
            // due JedisCluster does not support pipelined.
            //Pipeline pipeline = jedis.pipelined();

            for (String player : absentInRedis) {
                // Player not online according to Redis but not BungeeCord.
                handlePlatformPlayer(player, jedisCluster);
            }
        } catch (Throwable e) {
            plugin.logFatal("Unable to fix up stored player data");
            e.printStackTrace();
        }
        return null;
    }


    public abstract void handlePlatformPlayer(String player, JedisCluster jedisCluster);

    public abstract void handlePlatformPlayer(String player, Pipeline pipeline);
}
