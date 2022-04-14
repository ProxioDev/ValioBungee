package com.imaginarycode.minecraft.redisbungee;

/**
 * This used to be old plugin instance of redis-bungee, but now it's used to get the api for old plugins
 *
 * @deprecated its deprecated but won't be removed, so please use {@link RedisBungeeAPI#getRedisBungeeApi()}
 *
 */
@Deprecated
public class RedisBungee {

    private static RedisBungeeAPI api;

    public RedisBungee(RedisBungeeAPI api) {
        RedisBungee.api = api;
    }

    /**
     * This returns an instance of {@link RedisBungeeAPI}
     *
     * @deprecated Please use {@link RedisBungeeAPI#getRedisBungeeApi()} this class intended to for old plugins that no longer updated.
     *
     * @return the {@link RedisBungeeAPI} object instance.
     */
    @Deprecated
    public static RedisBungeeAPI getApi() {
        return api;
    }



}
