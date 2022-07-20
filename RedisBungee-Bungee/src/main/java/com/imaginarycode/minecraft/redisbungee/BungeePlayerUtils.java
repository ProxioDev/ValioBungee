package com.imaginarycode.minecraft.redisbungee;

import com.google.gson.Gson;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;

import static com.imaginarycode.minecraft.redisbungee.api.util.payload.PayloadUtils.playerJoinPayload;

public class BungeePlayerUtils {

    public static void createPlayer(ProxiedPlayer player, Pipeline pipeline, boolean fireEvent) {
        createPlayer(player.getPendingConnection(), pipeline, fireEvent);
        if (player.getServer() != null)
            pipeline.hset("player:" + player.getUniqueId().toString(), "server", player.getServer().getInfo().getName());
    }

    public static void createPlayer(PendingConnection connection, Pipeline pipeline, boolean fireEvent) {
        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", connection.getAddress().getAddress().getHostAddress());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getProxyId());

        pipeline.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", connection.getUniqueId().toString());
        pipeline.hmset("player:" + connection.getUniqueId().toString(), playerData);

        if (fireEvent) {
            playerJoinPayload(connection.getUniqueId(), pipeline, connection.getAddress().getAddress());
        }
    }

    public static void createPlayer(ProxiedPlayer player, JedisCluster jedisCluster, boolean fireEvent) {
        createPlayer(player.getPendingConnection(), jedisCluster, fireEvent);
        if (player.getServer() != null)
            jedisCluster.hset("player:" + player.getUniqueId().toString(), "server", player.getServer().getInfo().getName());
    }

    public static void createPlayer(PendingConnection connection, JedisCluster jedisCluster, boolean fireEvent) {
        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", connection.getAddress().getAddress().getHostAddress());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getProxyId());

        jedisCluster.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", connection.getUniqueId().toString());
        jedisCluster.hmset("player:" + connection.getUniqueId().toString(), playerData);

        if (fireEvent) {
            playerJoinPayload(connection.getUniqueId(), jedisCluster, connection.getAddress().getAddress());
        }
    }



}
