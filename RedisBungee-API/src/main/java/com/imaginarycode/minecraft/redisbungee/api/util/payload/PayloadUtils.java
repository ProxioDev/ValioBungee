package com.imaginarycode.minecraft.redisbungee.api.util.payload;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.AbstractDataManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import java.net.InetAddress;
import java.util.UUID;

public class PayloadUtils {
    private static final Gson gson = new Gson();

    public static void playerJoinPayload(UUID uuid, Pipeline pipeline, InetAddress inetAddress) {
        pipeline.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.JOIN,
                new AbstractDataManager.LoginPayload(inetAddress))));
    }
    public static void playerJoinPayload(UUID uuid, JedisCluster jedisCluster, InetAddress inetAddress) {
        jedisCluster.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.JOIN,
                new AbstractDataManager.LoginPayload(inetAddress))));
    }
    public static void playerJoinPayload(UUID uuid, Jedis jedis, InetAddress inetAddress) {
        jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.JOIN,
                new AbstractDataManager.LoginPayload(inetAddress))));
    }



    public static void playerQuitPayload(String uuid, Jedis jedis, long timestamp) {
        jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(uuid), RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }


    public static void playerQuitPayload(String uuid, JedisCluster jedisCluster, long timestamp) {
        jedisCluster.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(uuid), RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }

    public static void playerQuitPayload(String uuid, Pipeline pipeline, long timestamp) {
        pipeline.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(uuid), RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }


    public static void playerServerChangePayload(UUID uuid, Jedis jedis, String newServer, String oldServer) {
        jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.SERVER_CHANGE,
                new AbstractDataManager.ServerChangePayload(newServer, oldServer))));
    }

    public static void playerServerChangePayload(UUID uuid, Pipeline pipeline, String newServer, String oldServer) {
        pipeline.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.SERVER_CHANGE,
                new AbstractDataManager.ServerChangePayload(newServer, oldServer))));
    }
    public static void playerServerChangePayload(UUID uuid, JedisCluster jedisCluster, String newServer, String oldServer) {
        jedisCluster.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.SERVER_CHANGE,
                new AbstractDataManager.ServerChangePayload(newServer, oldServer))));
    }


    public static void kickPlayerPayload(UUID uuid, String message, Pipeline pipeline) {
        pipeline.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.KICK,
                new AbstractDataManager.KickPayload(message))));
    }

    public static void kickPlayerPayload(UUID uuid, String message, Jedis jedis) {
        jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.KICK,
                new AbstractDataManager.KickPayload(message))));
    }

    public static void kickPlayerPayload(UUID uuid, String message, JedisCluster jedisCluster) {
        jedisCluster.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.KICK,
                new AbstractDataManager.KickPayload(message))));
    }
}
