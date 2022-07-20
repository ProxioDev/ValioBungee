package com.imaginarycode.minecraft.redisbungee.api;


import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.InvocationTargetException;


public class JedisPubSubHandler extends JedisPubSub {

    private final RedisBungeePlugin<?> plugin;

    public JedisPubSubHandler(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(final String s, final String s2) {
        if (s2.trim().length() == 0) return;
        plugin.executeAsync(new Runnable() {
            @Override
            public void run() {
                Object event = plugin.createPubSubEvent(s, s2);
                plugin.callEvent(event);
            }
        });
    }
}