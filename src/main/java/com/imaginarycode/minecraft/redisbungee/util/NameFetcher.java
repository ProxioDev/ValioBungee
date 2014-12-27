/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee.util;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class NameFetcher {
    private static Map<UUID, List<String>> cache = new HashMap<>();

    public static List<String> nameHistoryFromUuid(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        URLConnection connection;
        try {
            connection = new URL("https://api.mojang.com/user/profiles/"
                    + uuid.toString().replace("-", "").toLowerCase() + "/names"
            ).openConnection();
            String text = new Scanner(connection.getInputStream()).useDelimiter("\\Z").next();
            Type listType = new TypeToken<List<String>>() {
            }.getType();
            List<String> list = new Gson().fromJson(text, listType);
            cache.put(uuid, list);
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}