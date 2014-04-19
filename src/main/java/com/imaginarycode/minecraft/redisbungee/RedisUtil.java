package com.imaginarycode.minecraft.redisbungee;

import redis.clients.jedis.Jedis;

public class RedisUtil {
    public static void cleanUpPlayer(String player, Jedis rsc) {
        rsc.srem("server:" + RedisBungee.getConfiguration().getString("server-id") + ":usersOnline", player);
        rsc.hdel("player:" + player, "server");
        rsc.hdel("player:" + player, "ip");
    }
}
