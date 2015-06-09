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