package com.imaginarycode.minecraft.redisbungee;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.UnifiedJedis;

import java.util.HashMap;
import java.util.Map;

import static com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils.playerJoinPayload;

public class BungeePlayerUtils {

    public static void createPlayer(ProxiedPlayer player, UnifiedJedis unifiedJedis, boolean fireEvent) {
        createPlayer(player.getPendingConnection(), unifiedJedis, fireEvent);
        if (player.getServer() != null)
            unifiedJedis.hset("player:" + player.getUniqueId().toString(), "server", player.getServer().getInfo().getName());
    }

    public static void createPlayer(PendingConnection connection, UnifiedJedis unifiedJedis, boolean fireEvent) {
        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", connection.getAddress().getAddress().getHostAddress());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getProxyId());

        unifiedJedis.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", connection.getUniqueId().toString());
        unifiedJedis.hmset("player:" + connection.getUniqueId().toString(), playerData);

        if (fireEvent) {
            playerJoinPayload(connection.getUniqueId(), unifiedJedis, connection.getAddress().getAddress());
        }
    }



}
