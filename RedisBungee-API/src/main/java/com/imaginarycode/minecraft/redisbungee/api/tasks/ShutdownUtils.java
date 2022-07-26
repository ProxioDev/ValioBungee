package com.imaginarycode.minecraft.redisbungee.api.tasks;

import com.imaginarycode.minecraft.redisbungee.api.util.player.PlayerUtils;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.Set;

public class ShutdownUtils {

    public static void shutdownCleanup(RedisBungeePlugin<?> plugin) {
        new RedisTask<Void>(plugin) {
            @Override
            public Void jedisTask(Jedis jedis) {
                jedis.hdel("heartbeats", plugin.getConfiguration().getProxyId());
                if (jedis.scard("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline") > 0) {
                    Set<String> players = jedis.smembers("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline");
                    for (String member : players)
                        PlayerUtils.cleanUpPlayer(member, jedis, true);
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                jedisCluster.hdel("heartbeats", plugin.getConfiguration().getProxyId());
                if (jedisCluster.scard("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline") > 0) {
                    Set<String> players = jedisCluster.smembers("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline");
                    for (String member : players)
                        PlayerUtils.cleanUpPlayer(member, jedisCluster, true);
                }
                return null;
            }
        }.execute();
    }


}
