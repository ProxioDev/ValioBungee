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
        final long timestamp = System.currentTimeMillis();
        final boolean isKickedFromOtherLocation = isKickedOtherLocation(uuid, rsc);
        rsc.srem("proxy:" + AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId() + ":usersOnline", uuid);
        if (!isKickedFromOtherLocation) {
            rsc.hdel("player:" + uuid, "server", "ip", "proxy");
            rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        }
        if (firePayload && !isKickedFromOtherLocation) {
            playerQuitPayload(uuid, rsc, timestamp);
        }
    }

    public static void setKickedOtherLocation(String uuid, UnifiedJedis unifiedJedis) {
        // set anything for sake of exists check. then expire it after 2 seconds. should be great?
        unifiedJedis.set("kicked-other-location::" + uuid, "0");
        unifiedJedis.expire("kicked-other-location::" + uuid, 2);
    }

    public static boolean isKickedOtherLocation(String uuid, UnifiedJedis unifiedJedis) {
        return unifiedJedis.exists("kicked-other-location::" + uuid);
    }


    public static void createPlayer(UUID uuid, UnifiedJedis unifiedJedis, String currentServer, InetAddress hostname, boolean fireEvent) {
        final boolean isKickedFromOtherLocation = isKickedOtherLocation(uuid.toString(), unifiedJedis);
        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", hostname.getHostName());
        playerData.put("proxy", AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId());
        if (currentServer != null) {
            playerData.put("server", currentServer);
        }
        unifiedJedis.sadd("proxy:" + AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId() + ":usersOnline", uuid.toString());
        unifiedJedis.hset("player:" + uuid, playerData);
        if (fireEvent && !isKickedFromOtherLocation) {
            playerJoinPayload(uuid, unifiedJedis, hostname);
        }
    }


}
