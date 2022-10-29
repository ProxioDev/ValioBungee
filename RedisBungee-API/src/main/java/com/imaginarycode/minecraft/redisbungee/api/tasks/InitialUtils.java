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

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.util.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.UnifiedJedis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InitialUtils {

    public static void checkRedisVersion(RedisBungeePlugin<?> plugin) {
        new RedisTask<Void>(plugin) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                // This is more portable than INFO <section>

                String info = new String((byte[]) unifiedJedis.sendCommand(Protocol.Command.INFO));
                for (String s : info.split("\r\n")) {
                    if (s.startsWith("redis_version:")) {
                        String version = s.split(":")[1];
                        plugin.logInfo(version + " <- redis version");
                        if (!RedisUtil.isRedisVersionRight(version)) {
                            plugin.logFatal("Your version of Redis (" + version + ") is not at least version 3.0 RedisBungee requires a newer version of Redis.");
                            throw new RuntimeException("Unsupported Redis version detected");
                        }
                        long uuidCacheSize = unifiedJedis.hlen("uuid-cache");
                        if (uuidCacheSize > 750000) {
                            plugin.logInfo("Looks like you have a really big UUID cache! Run https://www.spigotmc.org/resources/redisbungeecleaner.8505/ as soon as possible.");
                        }
                        break;
                    }
                }
                return null;
            }
        }.execute();
    }


    public static void checkIfRecovering(RedisBungeePlugin<?> plugin, Path dataFolder) {
        new RedisTask<Void>(plugin) {
            @Override
            public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
                Path crashFile = dataFolder.resolve("restarted_from_crash.txt");
                if (Files.exists(crashFile)) {
                    try {
                        Files.delete(crashFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    plugin.logInfo("crash file was deleted");
                } else if (unifiedJedis.hexists("heartbeats", plugin.getConfiguration().getProxyId())) {
                    try {
                        long value = Long.parseLong(unifiedJedis.hget("heartbeats", plugin.getConfiguration().getProxyId()));
                        long redisTime = plugin.getRedisTime(unifiedJedis);

                        if (redisTime < value + RedisUtil.PROXY_TIMEOUT) {
                            logImposter(plugin);
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return null;
            }
        }.execute();
    }

    private static void logImposter(RedisBungeePlugin<?> plugin) {
       plugin.logFatal("You have launched a possible impostor Velocity / Bungeecord instance. Another instance is already running.");
        plugin.logFatal("For data consistency reasons, RedisBungee will now disable itself.");
        plugin.logFatal("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
    }

}
