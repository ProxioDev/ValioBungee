package com.imaginarycode.minecraft.redisbungee.internal.util.uuid;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;

import com.imaginarycode.minecraft.redisbungee.internal.util.RedisTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class UUIDTranslator {
    private static final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    private static final Pattern MOJANGIAN_UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{32}");
    private final RedisBungeePlugin<?> plugin;
    private final Map<String, CachedUUIDEntry> nameToUuidMap = new ConcurrentHashMap<>(128, 0.5f, 4);
    private final Map<UUID, CachedUUIDEntry> uuidToNameMap = new ConcurrentHashMap<>(128, 0.5f, 4);
    private static final Gson gson = new Gson();

    public UUIDTranslator(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

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

    public UUID getTranslatedUuid(@NonNull String player, boolean expensiveLookups) {
        // If the player is online, give them their UUID.
        // Remember, local data > remote data.
        if (plugin.getPlayer(player) != null)
            return plugin.getPlayerUUID(player);

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
        if (!plugin.isOnlineMode()) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + player).getBytes(Charsets.UTF_8));
        }
        RedisTask<UUID> redisTask = new RedisTask<UUID>(plugin.getApi()) {
            @Override
            public UUID singleJedisTask(Jedis jedis) {
                String stored = jedis.hget("uuid-cache", player.toLowerCase());
                if (stored != null) {
                    // Found an entry value. Deserialize it.
                    CachedUUIDEntry entry = gson.fromJson(stored, CachedUUIDEntry.class);

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
                if (!expensiveLookups || !plugin.isOnlineMode())
                    return null;

                Map<String, UUID> uuidMap1;
                try {
                    uuidMap1 = new UUIDFetcher(Collections.singletonList(player)).call();
                } catch (Exception e) {
                    plugin.logFatal("Unable to fetch UUID from Mojang for " + player);
                    return null;
                }
                for (Map.Entry<String, UUID> entry : uuidMap1.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(player)) {
                        persistInfo(entry.getKey(), entry.getValue(), jedis);
                        return entry.getValue();
                    }
                }
                return null;
            }

            @Override
            public UUID clusterJedisTask(JedisCluster jedisCluster) {
                String stored = jedisCluster.hget("uuid-cache", player.toLowerCase());
                if (stored != null) {
                    // Found an entry value. Deserialize it.
                    CachedUUIDEntry entry = gson.fromJson(stored, CachedUUIDEntry.class);

                    // Check for expiry:
                    if (entry.expired()) {
                        jedisCluster.hdel("uuid-cache", player.toLowerCase());
                        // Doesn't hurt to also remove the UUID entry as well.
                        jedisCluster.hdel("uuid-cache", entry.getUuid().toString());
                    } else {
                        nameToUuidMap.put(player.toLowerCase(), entry);
                        uuidToNameMap.put(entry.getUuid(), entry);
                        return entry.getUuid();
                    }
                }

                // That didn't work. Let's ask Mojang.
                if (!expensiveLookups || !plugin.isOnlineMode())
                    return null;

                Map<String, UUID> uuidMap1;
                try {
                    uuidMap1 = new UUIDFetcher(Collections.singletonList(player)).call();
                } catch (Exception e) {
                    plugin.logFatal("Unable to fetch UUID from Mojang for " + player);
                    return null;
                }
                for (Map.Entry<String, UUID> entry : uuidMap1.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(player)) {
                        persistInfo(entry.getKey(), entry.getValue(), jedisCluster);
                        return entry.getValue();
                    }
                }
                return null;
            }
        };
        // Let's try Redis.
        try {
            return redisTask.execute();
        } catch (JedisException e) {
            plugin.logFatal("Unable to fetch UUID for " + player);
        }

        return null; // Nope, game over!
    }

    public String getNameFromUuid(@NonNull UUID player, boolean expensiveLookups) {
        // If the player is online, give them their UUID.
        // Remember, local data > remote data.
        if (plugin.getPlayer(player) != null)
            return plugin.getPlayerName(player);

        // Check if it exists in the map
        CachedUUIDEntry cachedUUIDEntry = uuidToNameMap.get(player);
        if (cachedUUIDEntry != null) {
            if (!cachedUUIDEntry.expired())
                return cachedUUIDEntry.getName();
            else
                uuidToNameMap.remove(player);
        }

        RedisTask<String> redisTask = new RedisTask<String>(plugin.getApi()) {
            @Override
            public String singleJedisTask(Jedis jedis) {
                String stored = jedis.hget("uuid-cache", player.toString());
                if (stored != null) {
                    // Found an entry value. Deserialize it.
                    CachedUUIDEntry entry = gson.fromJson(stored, CachedUUIDEntry.class);

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

                if (!expensiveLookups || !plugin.isOnlineMode())
                    return null;

                // That didn't work. Let's ask Mojang. This call may fail, because Mojang is insane.
                String name;
                try {
                    List<String> nameHist = NameFetcher.nameHistoryFromUuid(player);
                    name = Iterables.getLast(nameHist, null);
                } catch (Exception e) {
                    plugin.logFatal("Unable to fetch name from Mojang for " + player);
                    return null;
                }
                if (name != null) {
                    persistInfo(name, player, jedis);
                    return name;
                }
                return null;
            }

            @Override
            public String clusterJedisTask(JedisCluster jedisCluster) {
                String stored = jedisCluster.hget("uuid-cache", player.toString());
                if (stored != null) {
                    // Found an entry value. Deserialize it.
                    CachedUUIDEntry entry = gson.fromJson(stored, CachedUUIDEntry.class);

                    // Check for expiry:
                    if (entry.expired()) {
                        jedisCluster.hdel("uuid-cache", player.toString());
                        // Doesn't hurt to also remove the named entry as well.
                        // TODO: Since UUIDs are fixed, we could look up the name and see if the UUID matches.
                        jedisCluster.hdel("uuid-cache", entry.getName());
                    } else {
                        nameToUuidMap.put(entry.getName().toLowerCase(), entry);
                        uuidToNameMap.put(player, entry);
                        return entry.getName();
                    }
                }

                if (!expensiveLookups || !plugin.isOnlineMode())
                    return null;

                // That didn't work. Let's ask Mojang. This call may fail, because Mojang is insane.
                String name;
                try {
                    List<String> nameHist = NameFetcher.nameHistoryFromUuid(player);
                    name = Iterables.getLast(nameHist, null);
                } catch (Exception e) {
                    plugin.logFatal("Unable to fetch name from Mojang for " + player);
                    return null;
                }

                if (name != null) {
                    persistInfo(name, player, jedisCluster);
                    return name;
                }

                return null;
            }
        };


        // Okay, it wasn't locally cached. Let's try Redis.
        try {
            return redisTask.execute();
        } catch (JedisException e) {
            plugin.logFatal("Unable to fetch name for " + player);
            return null;
        }
    }

    public void persistInfo(String name, UUID uuid, Jedis jedis) {
        addToMaps(name, uuid);
        String json = gson.toJson(uuidToNameMap.get(uuid));
        jedis.hmset("uuid-cache", ImmutableMap.of(name.toLowerCase(), json, uuid.toString(), json));
    }

    public void persistInfo(String name, UUID uuid, Pipeline pipeline) {
        addToMaps(name, uuid);
        String json = gson.toJson(uuidToNameMap.get(uuid));
        pipeline.hmset("uuid-cache", ImmutableMap.of(name.toLowerCase(), json, uuid.toString(), json));
    }

    public void persistInfo(String name, UUID uuid, JedisCluster jedisCluster) {
        addToMaps(name, uuid);
        String json = gson.toJson(uuidToNameMap.get(uuid));
        jedisCluster.hmset("uuid-cache", ImmutableMap.of(name.toLowerCase(), json, uuid.toString(), json));
    }

    private static class CachedUUIDEntry {
        private final String name;
        private final UUID uuid;
        private final Calendar expiry;

        public CachedUUIDEntry(String name, UUID uuid, Calendar expiry) {
            this.name = name;
            this.uuid = uuid;
            this.expiry = expiry;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Calendar getExpiry() {
            return expiry;
        }

        public boolean expired() {
            return Calendar.getInstance().after(expiry);
        }
    }
}
