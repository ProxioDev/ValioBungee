/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.time.Duration;


public class NotClosableJedisCluster extends JedisCluster {

    NotClosableJedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
        super(provider, maxAttempts, maxTotalRetriesDuration);
    }

    @Override
    public void close() {

    }
}
