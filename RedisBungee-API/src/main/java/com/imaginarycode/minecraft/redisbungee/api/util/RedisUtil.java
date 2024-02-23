package com.imaginarycode.minecraft.redisbungee.api.util;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public class RedisUtil {
    public final static int PROXY_TIMEOUT = 30;

    public static final int MAJOR_VERSION = 6;
    public static final int MINOR_VERSION = 6;

    public static boolean isRedisVersionRight(String redisVersion) {
        String[] args = redisVersion.split("\\.");
        if (args.length < 2) {
            return false;
        }
        int major = Integer.parseInt(args[0]);
        int minor = Integer.parseInt(args[1]);

        if (major > MAJOR_VERSION) return true;
        return major == MAJOR_VERSION && minor >= MINOR_VERSION;

    }

    // Ham1255: i am keeping this if some plugin uses this *IF*
    @Deprecated
    public static boolean canUseLua(String redisVersion) {
        // Need to use >=3 to use Lua optimizations.
        return isRedisVersionRight(redisVersion);
    }
}
