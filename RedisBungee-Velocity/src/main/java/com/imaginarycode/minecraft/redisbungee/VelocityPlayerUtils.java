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

import com.imaginarycode.minecraft.redisbungee.api.util.player.PlayerUtils;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import redis.clients.jedis.UnifiedJedis;

import java.util.Optional;

public class VelocityPlayerUtils {
    protected static void createVelocityPlayer(Player player, UnifiedJedis unifiedJedis, boolean fireEvent) {
        Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        String serverName = null;
        if (optionalServerConnection.isPresent()) {
            serverName = optionalServerConnection.get().getServerInfo().getName();
        }
        PlayerUtils.createPlayer(player.getUniqueId(), unifiedJedis, serverName, player.getRemoteAddress().getAddress(), fireEvent);
    }


}
