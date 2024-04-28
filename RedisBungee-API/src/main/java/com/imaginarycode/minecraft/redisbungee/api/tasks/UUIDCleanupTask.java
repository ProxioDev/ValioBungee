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

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.CachedUUIDEntry;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;


public class UUIDCleanupTask extends RedisTask<Void>{

    private final Gson gson = new Gson();
    private final RedisBungeePlugin<?> plugin;

    public UUIDCleanupTask(RedisBungeePlugin<?> plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    // this code is inspired from https://github.com/minecrafter/redisbungeeclean
    @Override
    public Void unifiedJedisTask(UnifiedJedis unifiedJedis) {
        try {
            final long number = unifiedJedis.hlen("uuid-cache");
            plugin.logInfo("Found {} entries", number);
            ArrayList<String> fieldsToRemove = new ArrayList<>();
            unifiedJedis.hgetAll("uuid-cache").forEach((field, data) -> {
                CachedUUIDEntry cachedUUIDEntry = gson.fromJson(data, CachedUUIDEntry.class);
                if (cachedUUIDEntry.expired()) {
                    fieldsToRemove.add(field);
                }
            });
            if (!fieldsToRemove.isEmpty()) {
                unifiedJedis.hdel("uuid-cache", fieldsToRemove.toArray(new String[0]));
            }
          plugin.logInfo("deleted {} entries", fieldsToRemove.size());
        } catch (JedisException e) {
            plugin.logFatal("There was an error fetching information", e);
        }
        return null;
    }


}