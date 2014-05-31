/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee.util;

import com.google.common.collect.ImmutableList;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;

/* Credits to evilmidget38 for this class. I modified it to use Gson. */
class UUIDFetcher implements Callable<Map<String, UUID>> {
    private static final double PROFILES_PER_REQUEST = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private final List<String> names;
    private final boolean rateLimiting;

    public UUIDFetcher(List<String> names, boolean rateLimiting) {
        this.names = ImmutableList.copyOf(names);
        this.rateLimiting = rateLimiting;
    }

    public UUIDFetcher(List<String> names) {
        this(names, true);
    }

    public Map<String, UUID> call() throws Exception {
        Map<String, UUID> uuidMap = new HashMap<>();
        int requests = (int) Math.ceil(names.size() / PROFILES_PER_REQUEST);
        for (int i = 0; i < requests; i++) {
            HttpURLConnection connection = createConnection();
            String body = RedisBungee.getGson().toJson(names.subList(i * 100, Math.min((i + 1) * 100, names.size())));
            writeBody(connection, body);
            Profile[] array = RedisBungee.getGson().fromJson(new InputStreamReader(connection.getInputStream()), Profile[].class);
            for (Profile profile : array) {
                UUID uuid = UUIDFetcher.getUUID(profile.id);
                uuidMap.put(profile.name, uuid);
            }
            if (rateLimiting && i != requests - 1) {
                Thread.sleep(100L);
            }
        }
        return uuidMap;
    }

    private static void writeBody(HttpURLConnection connection, String body) throws Exception {
        OutputStream stream = connection.getOutputStream();
        stream.write(body.getBytes());
        stream.flush();
        stream.close();
    }

    private static HttpURLConnection createConnection() throws Exception {
        URL url = new URL(PROFILE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    public static UUID getUUID(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
    }

    public static byte[] toBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    public static UUID fromBytes(byte[] array) {
        if (array.length != 16) {
            throw new IllegalArgumentException("Illegal byte array length: " + array.length);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        long mostSignificant = byteBuffer.getLong();
        long leastSignificant = byteBuffer.getLong();
        return new UUID(mostSignificant, leastSignificant);
    }

    public static UUID getUUIDOf(String name) throws Exception {
        return new UUIDFetcher(Collections.singletonList(name)).call().get(name);
    }

    private class Profile {
        String id;
        String name;
    }
}