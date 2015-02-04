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
