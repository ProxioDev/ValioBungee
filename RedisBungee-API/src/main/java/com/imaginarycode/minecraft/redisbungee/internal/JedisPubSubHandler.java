package com.imaginarycode.minecraft.redisbungee.internal;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.InvocationTargetException;

public class JedisPubSubHandler extends JedisPubSub {

    private final RedisBungeePlugin<?> plugin;

    public JedisPubSubHandler(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

    private Class<?> bungeeEvent;

    @Override
    public void onMessage(final String s, final String s2) {
        if (s2.trim().length() == 0) return;
        plugin.executeAsync(new Runnable() {
            @Override
            public void run() {
                if (isBungeeEvent()) {
                    try {
                        Object object = bungeeEvent.getConstructor(String.class, String.class).newInstance(s, s2);
                        plugin.callEvent(object);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                        throw new RuntimeException("unable to fire pubsub event.");
                    }
                    return;
                }
                PubSubMessageEvent event = new PubSubMessageEvent(s, s2);
                plugin.callEvent(event);
            }
        });
    }

    public boolean isBungeeEvent() {
        return bungeeEvent != null;
    }

    public void setBungeeEvent(Class<?> clazz) {
        bungeeEvent = clazz;
    }
}