package com.imaginarycode.minecraft.redisbungee.api;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import static com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils.playerQuitPayload;

public class GenericPlayerUtils {


    public static void cleanUpPlayer(String uuid, Jedis rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", uuid);
        rsc.hdel("player:" + uuid, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        playerQuitPayload(uuid, rsc, timestamp);
    }

    public static void cleanUpPlayer(String uuid, Pipeline rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", uuid);
        rsc.hdel("player:" + uuid, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        playerQuitPayload(uuid, rsc, timestamp);

    }

    public static void cleanUpPlayer(String uuid, JedisCluster rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", uuid);
        rsc.hdel("player:" + uuid, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        playerQuitPayload(uuid, rsc, timestamp);

    }


}
