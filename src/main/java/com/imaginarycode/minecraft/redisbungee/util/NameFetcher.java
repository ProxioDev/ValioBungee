/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee.util;

import com.google.common.io.ByteStreams;
import com.google.gson.reflect.TypeToken;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class NameFetcher {
    public static List<String> nameHistoryFromUuid(UUID uuid) throws IOException {
        URLConnection connection = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "").toLowerCase() + "/names").openConnection();
        String text = new String(ByteStreams.toByteArray(connection.getInputStream()));
        Type listType = new TypeToken<List<String>>() {}.getType();
        return RedisBungee.getGson().fromJson(text, listType);
    }
}