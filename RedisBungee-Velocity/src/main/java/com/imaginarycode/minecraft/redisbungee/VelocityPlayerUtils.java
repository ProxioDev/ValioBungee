package com.imaginarycode.minecraft.redisbungee;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils.playerJoinPayload;

public class VelocityPlayerUtils {

    protected static void createPlayer(Player player, Pipeline pipeline, boolean fireEvent) {
        Optional<ServerConnection> server = player.getCurrentServer();
        server.ifPresent(serverConnection -> pipeline.hset("player:" + player.getUniqueId().toString(), "server", serverConnection.getServerInfo().getName()));

        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", player.getRemoteAddress().getHostName());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getProxyId());

        pipeline.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", player.getUniqueId().toString());
        pipeline.hmset("player:" + player.getUniqueId().toString(), playerData);

        if (fireEvent) {
            playerJoinPayload(player.getUniqueId(), pipeline, player.getRemoteAddress().getAddress());
        }
    }

    protected static void createPlayer(Player player, JedisCluster jedisCluster, boolean fireEvent) {
        Optional<ServerConnection> server = player.getCurrentServer();
        server.ifPresent(serverConnection -> jedisCluster.hset("player:" + player.getUniqueId().toString(), "server", serverConnection.getServerInfo().getName()));

        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", player.getRemoteAddress().getHostName());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getProxyId());

        jedisCluster.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", player.getUniqueId().toString());
        jedisCluster.hmset("player:" + player.getUniqueId().toString(), playerData);

        if (fireEvent) {
            playerJoinPayload(player.getUniqueId(), jedisCluster, player.getRemoteAddress().getAddress());
        }
    }
}
