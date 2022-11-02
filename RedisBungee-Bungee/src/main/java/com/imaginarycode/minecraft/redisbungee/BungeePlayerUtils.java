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
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.UnifiedJedis;
public class BungeePlayerUtils {

    public static void createBungeePlayer(ProxiedPlayer player, UnifiedJedis unifiedJedis, boolean fireEvent) {
        String serverName = null;
        if (player.getServer() != null) {
           serverName = player.getServer().getInfo().getName();
        }
        PendingConnection pendingConnection = player.getPendingConnection();
        PlayerUtils.createPlayer(player.getUniqueId(), unifiedJedis, serverName, pendingConnection.getAddress().getAddress(), fireEvent);
    }

}
