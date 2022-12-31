/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.util.uuid;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Deprecated
public class NameFetcher {
    private static OkHttpClient httpClient;
    private static final Gson gson = new Gson();

    @Deprecated
    public static void setHttpClient(OkHttpClient httpClient) {
        throw new UnsupportedOperationException("Due mojang disabled the Names API NameFetcher no longer functions and has been disabled");
        // NameFetcher.httpClient = httpClient;
    }

    @Deprecated
    public static List<String> nameHistoryFromUuid(UUID uuid) throws IOException {
        throw new UnsupportedOperationException("Due mojang disabled the Names API NameFetcher no longer functions and has been disabled");
//        String url = "https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names";
//        Request request = new Request.Builder().url(url).get().build();
//        ResponseBody body = httpClient.newCall(request).execute().body();
//        String response = body.string();
//        body.close();
//
//        Type listType = new TypeToken<List<Name>>() {
//        }.getType();
//        List<Name> names = gson.fromJson(response, listType);
//
//        List<String> humanNames = new ArrayList<>();
//        for (Name name : names) {
//            humanNames.add(name.name);
//        }
//        return humanNames;
    }

    @Deprecated
    public static class Name {
        private String name;
        private long changedToAt;
    }
}