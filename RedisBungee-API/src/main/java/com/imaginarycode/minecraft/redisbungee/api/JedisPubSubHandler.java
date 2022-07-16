package com.imaginarycode.minecraft.redisbungee.api;


import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.InvocationTargetException;


public class JedisPubSubHandler extends JedisPubSub {

    private final RedisBungeePlugin<?> plugin;
    private final Class<?> eventClass;
    public JedisPubSubHandler(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
        this.eventClass = plugin.getPubSubEventClass();
    }

    @Override
    public void onMessage(final String s, final String s2) {
        if (s2.trim().length() == 0) return;
        plugin.executeAsync(new Runnable() {
            @Override
            public void run() {
                Object event;
                try {
                    event = eventClass.getDeclaredConstructor(String.class, String.class).newInstance(s, s2);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                  throw new RuntimeException("unable to dispatch an pubsub event", e);
                }
                plugin.callEvent(event);
            }
        });
    }
}