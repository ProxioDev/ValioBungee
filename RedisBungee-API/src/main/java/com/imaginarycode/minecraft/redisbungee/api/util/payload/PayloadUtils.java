package com.imaginarycode.minecraft.redisbungee.api.util.payload;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.AbstractDataManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import java.util.UUID;

public class PayloadUtils {
    private static final Gson gson = new Gson();

    public static void cleanUpPlayer(String uuid, Jedis rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", uuid);
        rsc.hdel("player:" + uuid, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        rsc.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(uuid), RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }

    public static void cleanUpPlayer(String uuid, Pipeline rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", uuid);
        rsc.hdel("player:" + uuid, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        rsc.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(uuid), RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }

    public static void cleanUpPlayer(String uuid, JedisCluster rsc) {
        rsc.srem("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getProxyId() + ":usersOnline", uuid);
        rsc.hdel("player:" + uuid, "server", "ip", "proxy");
        long timestamp = System.currentTimeMillis();
        rsc.hset("player:" + uuid, "online", String.valueOf(timestamp));
        rsc.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(uuid), RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }

    public static void kickPlayer(UUID uuid, String message, Pipeline pipeline) {
        System.out.println(uuid);
        pipeline.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.KICK,
                new AbstractDataManager.KickPayload(message))));
    }

    public static void kickPlayer(UUID uuid, String message, Jedis jedis) {
        System.out.println(uuid);
        jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.KICK,
                new AbstractDataManager.KickPayload(message))));
    }

    public static void kickPlayer(UUID uuid, String message, JedisCluster jedisCluster) {
        System.out.println(uuid);
        jedisCluster.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, RedisBungeeAPI.getRedisBungeeApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.KICK,
                new AbstractDataManager.KickPayload(message))));
    }
}
