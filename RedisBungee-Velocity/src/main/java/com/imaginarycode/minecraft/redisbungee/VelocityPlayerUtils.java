package com.imaginarycode.minecraft.redisbungee;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.UnifiedJedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils.playerJoinPayload;

public class VelocityPlayerUtils {
    protected static void createPlayer(Player player, UnifiedJedis unifiedJedis, boolean fireEvent) {
        Optional<ServerConnection> server = player.getCurrentServer();
        server.ifPresent(serverConnection -> unifiedJedis.hset("player:" + player.getUniqueId().toString(), "server", serverConnection.getServerInfo().getName()));

        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", player.getRemoteAddress().getHostName());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getProxyId());

        unifiedJedis.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", player.getUniqueId().toString());
        unifiedJedis.hmset("player:" + player.getUniqueId().toString(), playerData);

        if (fireEvent) {
            playerJoinPayload(player.getUniqueId(), unifiedJedis, player.getRemoteAddress().getAddress());
        }
    }
}
