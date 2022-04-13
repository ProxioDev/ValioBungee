package com.imaginarycode.minecraft.redisbungee;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.internal.DataManager;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;

public class RBUtils {

    private static final Gson gson = new Gson();

    protected static void createPlayer(ProxiedPlayer player, Pipeline pipeline, boolean fireEvent) {
        createPlayer(player.getPendingConnection(), pipeline, fireEvent);
        if (player.getServer() != null)
            pipeline.hset("player:" + player.getUniqueId().toString(), "server", player.getServer().getInfo().getName());
    }

    protected static void createPlayer(PendingConnection connection, Pipeline pipeline, boolean fireEvent) {
        Map<String, String> playerData = new HashMap<>(4);
        playerData.put("online", "0");
        playerData.put("ip", connection.getAddress().getAddress().getHostAddress());
        playerData.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getServerId());

        pipeline.sadd("proxy:" + RedisBungeeAPI.getRedisBungeeApi().getServerId() + ":usersOnline", connection.getUniqueId().toString());
        pipeline.hmset("player:" + connection.getUniqueId().toString(), playerData);

        if (fireEvent) {
            pipeline.publish("redisbungee-data", gson.toJson(new DataManager.DataManagerMessage(
                    connection.getUniqueId(), RedisBungeeAPI.getRedisBungeeApi().getServerId(), DataManager.DataManagerMessage.Action.JOIN,
                    new DataManager.LoginPayload(connection.getAddress().getAddress()))));
        }
    }


}
