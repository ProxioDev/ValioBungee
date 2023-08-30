/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.util;

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.UnifiedJedis;


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
                        plugin.logInfo("Redis server version: " + version);
                        if (!RedisUtil.isRedisVersionRight(version)) {
                            plugin.logFatal("Your version of Redis (" + version + ") is not at least version 3.0 RedisBungee requires a newer version of Redis.");
                            throw new RuntimeException("Unsupported Redis version detected");
                        }
                        long uuidCacheSize = unifiedJedis.hlen("uuid-cache");
                        if (uuidCacheSize > 750000) {
                            plugin.logInfo("Looks like you have a really big UUID cache! Run https://github.com/ProxioDev/Brains");
                        }
                        break;
                    }
                }
                return null;
            }
        }.execute();
    }


}
