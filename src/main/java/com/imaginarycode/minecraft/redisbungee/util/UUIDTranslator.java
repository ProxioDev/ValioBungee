/**
 * This is free and unencumbered software released into the public domain.
 * <p/>
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * <p/>
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * <p/>
 * For more information, please refer to <http://unlicense.org/>
 */
package com.imaginarycode.minecraft.redisbungee.util;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public final class UUIDTranslator {
    private static final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    private static final Pattern MOJANGIAN_UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{32}");
    private final RedisBungee plugin;
    private final Map<String, CachedUUIDEntry> nameToUuidMap = new ConcurrentHashMap<>(128, 0.5f, 4);
    private final Map<UUID, CachedUUIDEntry> uuidToNameMap = new ConcurrentHashMap<>(128, 0.5f, 4);

    private void addToMaps(String name, UUID uuid) {
        // This is why I like LocalDate...

        // Cache the entry for three days.
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 3);

        // Create the entry and populate the local maps
        CachedUUIDEntry entry = new CachedUUIDEntry(name, uuid, calendar);
        nameToUuidMap.put(name.toLowerCase(), entry);
        uuidToNameMap.put(uuid, entry);
    }

    public final UUID getTranslatedUuid(@NonNull String player, boolean expensiveLookups) {
        // If the player is online, give them their UUID.
        // Remember, local data > remote data.
        if (ProxyServer.getInstance().getPlayer(player) != null)
            return ProxyServer.getInstance().getPlayer(player).getUniqueId();

        // Check if it exists in the map
        CachedUUIDEntry cachedUUIDEntry = nameToUuidMap.get(player.toLowerCase());
        if (cachedUUIDEntry != null) {
            if (!cachedUUIDEntry.expired())
                return cachedUUIDEntry.getUuid();
            else
                nameToUuidMap.remove(player);
        }

        // Check if we can exit early
        if (UUID_PATTERN.matcher(player).find()) {
            return UUID.fromString(player);
        }

        if (MOJANGIAN_UUID_PATTERN.matcher(player).find()) {
            // Reconstruct the UUID
            return UUIDFetcher.getUUID(player);
        }

        // If we are in offline mode, UUID generation is simple.
        // We don't even have to cache the UUID, since this is easy to recalculate.
        if (!plugin.getProxy().getConfig().isOnlineMode()) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + player).getBytes(Charsets.UTF_8));
        }

        // Let's try Redis.
        try (Jedis jedis = plugin.getPool().getResource()) {
            String stored = jedis.hget("uuid-cache", player.toLowerCase());
            if (stored != null) {
                // Found an entry value. Deserialize it.
                CachedUUIDEntry entry = RedisBungee.getGson().fromJson(stored, CachedUUIDEntry.class);

                // Check for expiry:
                if (entry.expired()) {
                    jedis.hdel("uuid-cache", player.toLowerCase());
                    // Doesn't hurt to also remove the UUID entry as well.
                    jedis.hdel("uuid-cache", entry.getUuid().toString());
                } else {
                    nameToUuidMap.put(player.toLowerCase(), entry);
                    uuidToNameMap.put(entry.getUuid(), entry);
                    return entry.getUuid();
                }
            }

            // That didn't work. Let's ask Mojang.
            if (!expensiveLookups || !ProxyServer.getInstance().getConfig().isOnlineMode())
                return null;

            Map<String, UUID> uuidMap1;
            try {
                uuidMap1 = new UUIDFetcher(Collections.singletonList(player)).call();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Unable to fetch UUID from Mojang for " + player, e);
                return null;
            }
            for (Map.Entry<String, UUID> entry : uuidMap1.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(player)) {
                    persistInfo(entry.getKey(), entry.getValue(), jedis);
                    return entry.getValue();
                }
            }
        } catch (JedisException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to fetch UUID for " + player, e);
        }

        return null; // Nope, game over!
    }

    public final String getNameFromUuid(@NonNull UUID player, boolean expensiveLookups) {
        // If the player is online, give them their UUID.
        // Remember, local data > remote data.
        if (ProxyServer.getInstance().getPlayer(player) != null)
            return ProxyServer.getInstance().getPlayer(player).getName();

        // Check if it exists in the map
        CachedUUIDEntry cachedUUIDEntry = uuidToNameMap.get(player);
        if (cachedUUIDEntry != null) {
            if (!cachedUUIDEntry.expired())
                return cachedUUIDEntry.getName();
            else
                uuidToNameMap.remove(player);
        }

        // Okay, it wasn't locally cached. Let's try Redis.
        try (Jedis jedis = plugin.getPool().getResource()) {
            String stored = jedis.hget("uuid-cache", player.toString());
            if (stored != null) {
                // Found an entry value. Deserialize it.
                CachedUUIDEntry entry = RedisBungee.getGson().fromJson(stored, CachedUUIDEntry.class);

                // Check for expiry:
                if (entry.expired()) {
                    jedis.hdel("uuid-cache", player.toString());
                    // Doesn't hurt to also remove the named entry as well.
                    // TODO: Since UUIDs are fixed, we could look up the name and see if the UUID matches.
                    jedis.hdel("uuid-cache", entry.getName());
                } else {
                    nameToUuidMap.put(entry.getName().toLowerCase(), entry);
                    uuidToNameMap.put(player, entry);
                    return entry.getName();
                }
            }

            if (!expensiveLookups || !ProxyServer.getInstance().getConfig().isOnlineMode())
                return null;

            // That didn't work. Let's ask Mojang. This call may fail, because Mojang is insane.
            String name;
            try {
                List<String> nameHist = NameFetcher.nameHistoryFromUuid(player);
                name = Iterables.getLast(nameHist, null);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Unable to fetch name from Mojang for " + player, e);
                return null;
            }

            if (name != null) {
                persistInfo(name, player, jedis);
                return name;
            }

            return null;
        } catch (JedisException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to fetch name for " + player, e);
            return null;
        }
    }

    public final void persistInfo(String name, UUID uuid, Jedis jedis) {
        addToMaps(name, uuid);
        String json = RedisBungee.getGson().toJson(uuidToNameMap.get(uuid));
        jedis.hmset("uuid-cache", ImmutableMap.of(name, json, uuid.toString(), json));
    }

    public final void persistInfo(String name, UUID uuid, Pipeline jedis) {
        addToMaps(name, uuid);
        String json = RedisBungee.getGson().toJson(uuidToNameMap.get(uuid));
        jedis.hmset("uuid-cache", ImmutableMap.of(name, json, uuid.toString(), json));
    }

    @RequiredArgsConstructor
    @Getter
    private class CachedUUIDEntry {
        private final String name;
        private final UUID uuid;
        private final Calendar expiry;

        public boolean expired() {
            return Calendar.getInstance().after(expiry);
        }
    }
}
