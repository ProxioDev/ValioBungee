/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.tasks;

import com.imaginarycode.minecraft.redisbungee.api.util.player.PlayerUtils;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.UnifiedJedis;

import java.util.Set;

public class ShutdownUtils {

    public static void shutdownCleanup(RedisBungeePlugin<?> plugin) {
        new RedisTask<Void>(plugin) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                unifiedJedis.hdel("heartbeats", plugin.getConfiguration().getProxyId());
                if (unifiedJedis.scard("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline") > 0) {
                    Set<String> players = unifiedJedis.smembers("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline");
                    for (String member : players)
                        PlayerUtils.cleanUpPlayer(member, unifiedJedis, true);
                }
                return null;
            }
        }.execute();
    }


}
