package com.imaginarycode.minecraft.redisbungee.util.uuid;

import com.google.common.collect.ImmutableList;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.squareup.okhttp.*;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/* Credits to evilmidget38 for this class. I modified it to use Gson. */
public class UUIDFetcher implements Callable<Map<String, UUID>> {
    private static final double PROFILES_PER_REQUEST = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private static final MediaType JSON = MediaType.parse("application/json");
    private final List<String> names;
    private final boolean rateLimiting;

    @Setter
    private static OkHttpClient httpClient;

    private UUIDFetcher(List<String> names, boolean rateLimiting) {
        this.names = ImmutableList.copyOf(names);
        this.rateLimiting = rateLimiting;
    }

    public UUIDFetcher(List<String> names) {
        this(names, true);
    }

    public static UUID getUUID(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
    }

    public Map<String, UUID> call() throws Exception {
        Map<String, UUID> uuidMap = new HashMap<>();
        int requests = (int) Math.ceil(names.size() / PROFILES_PER_REQUEST);
        for (int i = 0; i < requests; i++) {
            String body = RedisBungee.getGson().toJson(names.subList(i * 100, Math.min((i + 1) * 100, names.size())));
            Request request = new Request.Builder().url(PROFILE_URL).post(RequestBody.create(JSON, body)).build();
            ResponseBody responseBody = httpClient.newCall(request).execute().body();
            String response = responseBody.string();
            responseBody.close();
            Profile[] array = RedisBungee.getGson().fromJson(response, Profile[].class);
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

    private static class Profile {
        String id;
        String name;
    }
}