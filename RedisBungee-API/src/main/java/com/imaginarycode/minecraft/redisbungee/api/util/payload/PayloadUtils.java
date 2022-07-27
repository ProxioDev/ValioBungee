package com.imaginarycode.minecraft.redisbungee.api.util.payload;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.AbstractRedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.AbstractDataManager;
import redis.clients.jedis.UnifiedJedis;

import java.net.InetAddress;
import java.util.UUID;

public class PayloadUtils {
    private static final Gson gson = new Gson();

    public static void playerJoinPayload(UUID uuid, UnifiedJedis unifiedJedis, InetAddress inetAddress) {
        unifiedJedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId(), AbstractDataManager.DataManagerMessage.Action.JOIN,
                new AbstractDataManager.LoginPayload(inetAddress))));
    }


    public static void playerQuitPayload(String uuid, UnifiedJedis unifiedJedis, long timestamp) {
        unifiedJedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                UUID.fromString(uuid), AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId(), AbstractDataManager.DataManagerMessage.Action.LEAVE,
                new AbstractDataManager.LogoutPayload(timestamp))));
    }



    public static void playerServerChangePayload(UUID uuid, UnifiedJedis unifiedJedis, String newServer, String oldServer) {
        unifiedJedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId(), AbstractDataManager.DataManagerMessage.Action.SERVER_CHANGE,
                new AbstractDataManager.ServerChangePayload(newServer, oldServer))));
    }


    public static void kickPlayerPayload(UUID uuid, String message, UnifiedJedis unifiedJedis) {
        unifiedJedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                uuid, AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId(), AbstractDataManager.DataManagerMessage.Action.KICK,
                new AbstractDataManager.KickPayload(message))));
    }
}
