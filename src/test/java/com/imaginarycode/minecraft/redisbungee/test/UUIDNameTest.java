package com.imaginarycode.minecraft.redisbungee.test;

import com.imaginarycode.minecraft.redisbungee.util.uuid.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.util.uuid.UUIDFetcher;
import com.squareup.okhttp.OkHttpClient;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UUIDNameTest {
    private String[] uuidsToTest = {"68ec43f7234b41b48764dfb38b9ffe8c", "652a2bc4e8cd405db7b698156ee2dc09"};
    private String[] namesToTest = {"vemacs"};

    @Test
    public void testUuidToName() throws IOException {
        OkHttpClient httpClient = new OkHttpClient();
        NameFetcher.setHttpClient(httpClient);
        for (String uuid : uuidsToTest) {
            List<String> names = NameFetcher.nameHistoryFromUuid(UUIDFetcher.getUUID(uuid));
            String currentName = names.get(names.size() - 1);
            System.out.println("Current name for UUID " + uuid + " is " + currentName);
        }
    }

    @Test
    public void testNameToUuid() throws IOException {
        OkHttpClient httpClient = new OkHttpClient();
        UUIDFetcher.setHttpClient(httpClient);
        for (String name : namesToTest) {
            Map<String, UUID> uuidMap1;
            try {
                uuidMap1 = new UUIDFetcher(Collections.singletonList(name)).call();
                for (Map.Entry<String, UUID> entry : uuidMap1.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(name)) {
                        System.out.println("Current UUID for name " + name + " is " + entry.getValue());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
