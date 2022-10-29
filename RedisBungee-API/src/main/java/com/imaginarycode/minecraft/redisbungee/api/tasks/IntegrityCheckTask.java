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
import redis.clients.jedis.UnifiedJedis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class IntegrityCheckTask extends RedisTask<Void> {

    public static int INTERVAL = 30;
    public static TimeUnit TIMEUNIT = TimeUnit.SECONDS;


    public IntegrityCheckTask(RedisBungeePlugin<?> plugin) {
        super(plugin);
    }

    @Override
    public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
        try {
            Set<String> players = plugin.getLocalPlayersAsUuidStrings();
            Set<String> playersInRedis = unifiedJedis.smembers("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline");
            List<String> lagged = plugin.getCurrentProxiesIds(true);

            // Clean up lagged players.
            for (String s : lagged) {
                Set<String> laggedPlayers = unifiedJedis.smembers("proxy:" + s + ":usersOnline");
                unifiedJedis.del("proxy:" + s + ":usersOnline");
                if (!laggedPlayers.isEmpty()) {
                    plugin.logInfo("Cleaning up lagged proxy " + s + " (" + laggedPlayers.size() + " players)...");
                    for (String laggedPlayer : laggedPlayers) {
                        PlayerUtils.cleanUpPlayer(laggedPlayer, unifiedJedis, true);
                    }
                }
            }

            Set<String> absentLocally = new HashSet<>(playersInRedis);
            absentLocally.removeAll(players);
            Set<String> absentInRedis = new HashSet<>(players);
            absentInRedis.removeAll(playersInRedis);

            for (String member : absentLocally) {
                boolean found = false;
                for (String proxyId : plugin.getProxiesIds()) {
                    if (proxyId.equals(plugin.getConfiguration().getProxyId())) continue;
                    if (unifiedJedis.sismember("proxy:" + proxyId + ":usersOnline", member)) {
                        // Just clean up the set.
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    PlayerUtils.cleanUpPlayer(member, unifiedJedis, false);
                    plugin.logWarn("Player found in set that was not found locally and globally: " + member);
                } else {
                    unifiedJedis.srem("proxy:" + plugin.getConfiguration().getProxyId() + ":usersOnline", member);
                    plugin.logWarn("Player found in set that was not found locally, but is on another proxy: " + member);
                }
            }
            // due unifiedJedis does not support pipelined.
            //Pipeline pipeline = jedis.pipelined();

            for (String player : absentInRedis) {
                // Player not online according to Redis but not BungeeCord.
                handlePlatformPlayer(player, unifiedJedis);
            }
        } catch (Throwable e) {
            plugin.logFatal("Unable to fix up stored player data");
            e.printStackTrace();
        }
        return null;
    }


    public abstract void handlePlatformPlayer(String player, UnifiedJedis unifiedJedis);

}
