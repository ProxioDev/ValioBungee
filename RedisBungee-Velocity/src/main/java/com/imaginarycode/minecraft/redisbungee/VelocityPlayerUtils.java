/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
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
        playerData.put("proxy", AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId());

        unifiedJedis.sadd("proxy:" + AbstractRedisBungeeAPI.getAbstractRedisBungeeAPI().getProxyId() + ":usersOnline", player.getUniqueId().toString());
        unifiedJedis.hmset("player:" + player.getUniqueId().toString(), playerData);

        if (fireEvent) {
            playerJoinPayload(player.getUniqueId(), unifiedJedis, player.getRemoteAddress().getAddress());
        }
    }
}
