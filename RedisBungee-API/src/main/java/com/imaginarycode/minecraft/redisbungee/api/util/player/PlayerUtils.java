package com.imaginarycode.minecraft.redisbungee.api.util.player;

import com.imaginarycode.minecraft.redisbungee.AbstractRedisBungeeAPI;
import redis.clients.jedis.UnifiedJedis;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils.playerJoinPayload;
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

    public static void createPlayer(UUID uuid, UnifiedJedis unifiedJedis, String currentServer, InetAddress hostname, boolean fireEvent) {
        if (currentServer != null) {
            unifiedJedis.hset("player:" + uuid, "server", currentServer);
        }
        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", hostname.getHostName());
        playerData.put("proxy", AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId());

        unifiedJedis.sadd("proxy:" + AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId() + ":usersOnline", uuid.toString());
        unifiedJedis.hmset("player:" + uuid, playerData);

        if (fireEvent) {
            playerJoinPayload(uuid, unifiedJedis, hostname);
        }
    }


}
