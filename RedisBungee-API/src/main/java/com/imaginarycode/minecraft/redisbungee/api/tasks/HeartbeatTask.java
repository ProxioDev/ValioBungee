package com.imaginarycode.minecraft.redisbungee.api.tasks;

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatTask extends RedisTask<Void>{

    public final static TimeUnit REPEAT_INTERVAL_TIME_UNIT = TimeUnit.SECONDS;
    public final static int INTERVAL = 1;
    private final AtomicInteger globalPlayerCount;

    public HeartbeatTask(RedisBungeePlugin<?> plugin, AtomicInteger globalPlayerCount) {
        super(plugin);
        this.globalPlayerCount = globalPlayerCount;
    }

    @Override
    public Void jedisTask(Jedis jedis) {
        try {
            long redisTime = plugin.getRedisTime(jedis.time());
            jedis.hset("heartbeats", plugin.getConfiguration().getProxyId(), String.valueOf(redisTime));
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
            plugin.logFatal("Unable to update heartbeat - did your Redis server go away?");
            e.printStackTrace();
            return null;
        }
        try {
            plugin.updateProxiesIds();
            globalPlayerCount.set(plugin.getCurrentCount());
        } catch (Throwable e) {
            plugin.logFatal("Unable to update data - did your Redis server go away?");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Void clusterJedisTask(JedisCluster jedisCluster) {
        try {
            long redisTime = plugin.getRedisTime(jedisCluster);
            jedisCluster.hset("heartbeats", plugin.getConfiguration().getProxyId(), String.valueOf(redisTime));
        } catch (JedisConnectionException e) {
            // Redis server has disappeared!
           plugin.logFatal("Unable to update heartbeat - did your Redis server go away?");
           e.printStackTrace();
            return null;
        }
        try {
            plugin.updateProxiesIds();
            globalPlayerCount.set(plugin.getCurrentCount());
        } catch (Throwable e) {
            plugin.logFatal("Unable to update data - did your Redis server go away?");
            e.printStackTrace();
        }
        return null;
    }



}
