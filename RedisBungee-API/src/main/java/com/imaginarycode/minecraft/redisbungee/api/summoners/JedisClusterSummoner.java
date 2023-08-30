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

import java.io.IOException;
import java.time.Duration;

public class JedisClusterSummoner implements Summoner<JedisCluster> {
    private final ClusterConnectionProvider clusterConnectionProvider;

    public JedisClusterSummoner(ClusterConnectionProvider clusterConnectionProvider) {
        this.clusterConnectionProvider = clusterConnectionProvider;
        // test the connection
        JedisCluster jedisCluster = obtainResource();
        jedisCluster.set("random_data", "0");
        jedisCluster.del("random_data");
    }


    @Override
    public void close() throws IOException {
        this.clusterConnectionProvider.close();
    }

    @Override
    public JedisCluster obtainResource() {
        return new NotClosableJedisCluster(this.clusterConnectionProvider, 60, Duration.ofSeconds(10));
    }


}
