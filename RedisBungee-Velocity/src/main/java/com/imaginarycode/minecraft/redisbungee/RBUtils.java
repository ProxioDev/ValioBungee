package com.imaginarycode.minecraft.redisbungee;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.internal.DataManager;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RBUtils {

    private static final Gson gson = new Gson();

    protected static void createPlayer(Player player, Pipeline pipeline, boolean fireEvent) {
        Optional<ServerConnection> server = player.getCurrentServer();
        server.ifPresent(serverConnection -> pipeline.hset("player:" + player.getUniqueId().toString(), "server", serverConnection.getServerInfo().getName()));

        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", player.getRemoteAddress().getHostName());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getServerId());

        pipeline.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getServerId() + ":usersOnline", player.getUniqueId().toString());
        pipeline.hmset("player:" + player.getUniqueId().toString(), playerData);

        if (fireEvent) {
            pipeline.publish("redisbungee-data", gson.toJson(new DataManager.DataManagerMessage<>(
                    player.getUniqueId(), RedisBungeeAPI.getRedisBungeeApi().getServerId(), DataManager.DataManagerMessage.Action.JOIN,
                    new DataManager.LoginPayload(player.getRemoteAddress().getAddress()))));
        }
    }


}
