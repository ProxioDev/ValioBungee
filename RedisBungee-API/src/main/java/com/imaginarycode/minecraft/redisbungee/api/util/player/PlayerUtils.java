package com.imaginarycode.minecraft.redisbungee.api.util.player;

import com.imaginarycode.minecraft.redisbungee.AbstractRedisBungeeAPI;
import redis.clients.jedis.UnifiedJedis;

import static com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils.playerQuitPayload;

public class PlayerUtils {

    public static void cleanUpPlayer(String uuid, UnifiedJedis rsc, boolean firePayload) {
        rsc.srem("proxy:" + AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId() + ":usersOnline", uuid);
        rsc.hdel("player:" + uuid, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        if (firePayload) {
            playerQuitPayload(uuid, rsc, timestamp);
        }
    }


}
