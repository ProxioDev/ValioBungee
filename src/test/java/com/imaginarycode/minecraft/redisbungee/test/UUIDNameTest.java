/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
package com.imaginarycode.minecraft.redisbungee.test;

import com.imaginarycode.minecraft.redisbungee.util.NameFetcher;
import com.imaginarycode.minecraft.redisbungee.util.UUIDFetcher;
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
