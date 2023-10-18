package com.imaginarycode.minecraft.redisbungee.api.util;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public class RedisUtil {
    public final static int PROXY_TIMEOUT = 30;

    public static boolean isRedisVersionRight(String redisVersion) {
        String[] args = redisVersion.split("\\.");
        if (args.length < 2) {
            return false;
        }
        int major = Integer.parseInt(args[0]);
        int minor = Integer.parseInt(args[1]);
        return major >= 6 && minor >= 2;
    }

    // Ham1255: i am keeping this if some plugin uses this *IF*
    @Deprecated
    public static boolean canUseLua(String redisVersion) {
        // Need to use >=3 to use Lua optimizations.
        return isRedisVersionRight(redisVersion);
    }
}
