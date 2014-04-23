/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee.util;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class UUIDTranslator {
    private final RedisBungee plugin;
    private BiMap<String, UUID> uuidMap = Maps.synchronizedBiMap(HashBiMap.<String, UUID>create());
    public static final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

    public UUID getTranslatedUuid(String player) {
        if (ProxyServer.getInstance().getPlayer(player) != null)
            return ProxyServer.getInstance().getPlayer(player).getUniqueId();

        UUID uuid = uuidMap.get(player);
        if (uuid != null)
            return uuid;

        if (!plugin.getProxy().getConfig().isOnlineMode()) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + player).getBytes(Charsets.UTF_8));
            uuidMap.put(player, uuid);
            return uuid;
        }

        // Okay, it wasn't locally cached. Let's try Redis.
        Jedis jedis = plugin.getPool().getResource();
        try {
            String stored = jedis.hget("uuids", player);
            if (stored != null && UUID_PATTERN.matcher(stored).find()) {
                // This is it!
                uuid = UUID.fromString(stored);
                storeInfo(player, uuid, jedis);
                uuidMap.put(player, uuid);
                return uuid;
            }

            // That didn't work. Let's ask Mojang.
            uuid = UUIDFetcher.getUUIDOf(player);

            if (uuid != null) {
                uuidMap.put(player, uuid);
                storeInfo(player, uuid, jedis);
            }

            return uuid;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to fetch UUID for " + player, e);
            return null;
        } finally {
            plugin.getPool().returnResource(jedis);
        }
    }

    public String getNameFromUuid(UUID player) {
        if (ProxyServer.getInstance().getPlayer(player) != null)
            return ProxyServer.getInstance().getPlayer(player).getName();

        String name = uuidMap.inverse().get(player);

        if (name != null)
            return name;

        // Okay, it wasn't locally cached. Let's try Redis.
        Jedis jedis = plugin.getPool().getResource();
        try {
            String stored = jedis.hget("player:" + player, "name");
            if (stored != null) {
                name = stored;
                uuidMap.put(name, player);
                return name;
            }

            // That didn't work. Let's ask Mojang.
            name = new NameFetcher(Collections.singletonList(player)).call().get(player);

            if (name != null) {
                storeInfo(name, player, jedis);
                return name;
            }

            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to fetch name for " + player, e);
            return null;
        } finally {
            plugin.getPool().returnResource(jedis);
        }
    }

    private static void storeInfo(String name, UUID uuid, Jedis jedis) {
        jedis.hset("uuids", name, uuid.toString());
        jedis.hset("player:" + uuid, "name", name);
    }
}
