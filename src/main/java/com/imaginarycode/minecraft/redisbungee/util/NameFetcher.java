/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee.util;

import com.google.gson.reflect.TypeToken;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NameFetcher {
    @Setter
    private static OkHttpClient httpClient;

    public static List<String> nameHistoryFromUuid(UUID uuid) throws IOException {
        String url = "https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names";
        Request request = new Request.Builder().url(url).get().build();
        String response = httpClient.newCall(request).execute().body().string();

        Type listType = new TypeToken<List<Name>>() {}.getType();
        List<Name> names = RedisBungee.getGson().fromJson(response, listType);

        List<String> humanNames = new ArrayList<>();
        for (Name name : names) {
            humanNames.add(name.name);
        }
        return humanNames;
    }

    public static class Name {
        private String name;
        private long changedToAt;
    }
}