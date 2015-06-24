/**
 * This is free and unencumbered software released into the public domain.
 * <p/>
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * <p/>
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * <p/>
 * For more information, please refer to <http://unlicense.org/>
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

@VisibleForTesting
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisUtil {
    // Compatibility restraints prevent me from using using HDEL with multiple keys.
    public static void cleanUpPlayer(String player, Jedis rsc) {
        rsc.srem("proxy:" + RedisBungee.getApi().getServerId() + ":usersOnline", player);
        rsc.hdel("player:" + player, "server");
        rsc.hdel("player:" + player, "ip");
        rsc.hdel("player:" + player, "proxy");
    }

    public static void cleanUpPlayer(String player, Pipeline rsc) {
        rsc.srem("proxy:" + RedisBungee.getApi().getServerId() + ":usersOnline", player);
        rsc.hdel("player:" + player, "server");
        rsc.hdel("player:" + player, "ip");
        rsc.hdel("player:" + player, "proxy");
    }

    public static boolean canUseLua(String redisVersion) {
        // Need to use >=2.6 to use Lua optimizations.
        String[] args = redisVersion.split("\\.");

        if (args.length < 2) {
            return false;
        }

        int major = Integer.parseInt(args[0]);
        int minor = Integer.parseInt(args[1]);

        return major >= 3 || (major == 2 && minor >= 6);
    }
}
