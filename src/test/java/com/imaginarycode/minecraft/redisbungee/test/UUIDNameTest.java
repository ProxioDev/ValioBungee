/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee.test;

import com.imaginarycode.minecraft.redisbungee.util.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.util.UUIDFetcher;
import com.squareup.okhttp.OkHttpClient;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class UUIDNameTest {
    @Test
    public void testUuidToName() throws IOException {
        OkHttpClient httpClient = new OkHttpClient();
        String uuid = "68ec43f7234b41b48764dfb38b9ffe8c";
        NameFetcher.setHttpClient(httpClient);
        List<String> names = NameFetcher.nameHistoryFromUuid(UUIDFetcher.getUUID(uuid));
        String currentName = names.get(names.size() - 1);
        System.out.println("Current name for UUID " + uuid + " is " + currentName);
    }
}
