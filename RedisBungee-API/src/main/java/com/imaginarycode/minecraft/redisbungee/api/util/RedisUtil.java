package com.imaginarycode.minecraft.redisbungee.api.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.AbstractDataManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import java.util.UUID;

@VisibleForTesting
public class RedisUtil {
    private static final Gson gson = new Gson();

    public static void cleanUpPlayer(String player, Jedis rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getServerId() + ":usersOnline", player);
        rsc.hdel("player:" + player, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + player, "online", String.valueOf(timestamp));
        rsc.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(player), RedisBungeeAPI.getRedisBungeeApi().getServerId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }

    public static void cleanUpPlayer(String player, Pipeline rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getServerId() + ":usersOnline", player);
        rsc.hdel("player:" + player, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + player, "online", String.valueOf(timestamp));
        rsc.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(player), RedisBungeeAPI.getRedisBungeeApi().getServerId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }
    public static void cleanUpPlayer(String player, JedisCluster rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getServerId() + ":usersOnline", player);
        rsc.hdel("player:" + player, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + player, "online", String.valueOf(timestamp));
        rsc.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(player), RedisBungeeAPI.getRedisBungeeApi().getServerId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }

    public static boolean isRedisVersionRight(String redisVersion) {
        String[] args = redisVersion.split("\\.");
        if (args.length < 2) {
            return false;
        }
        int major = Integer.parseInt(args[0]);
        int minor = Integer.parseInt(args[1]);
        return major >= 3 && minor >= 0;
    }

    // Ham1255: i am keeping this if some plugin uses this *IF*
    @Deprecated
    public static boolean canUseLua(String redisVersion) {
        // Need to use >=3 to use Lua optimizations.
        return isRedisVersionRight(redisVersion);
    }
}
