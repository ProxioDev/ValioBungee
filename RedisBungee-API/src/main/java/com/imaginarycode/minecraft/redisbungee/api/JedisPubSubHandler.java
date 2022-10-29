/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api;


import redis.clients.jedis.JedisPubSub;


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
                plugin.fireEvent(event);
            }
        });
    }
}