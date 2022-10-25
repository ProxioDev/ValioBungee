package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;

import java.time.Duration;


public class NotClosableJedisCluster extends JedisCluster {

    NotClosableJedisCluster(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
        super(provider, maxAttempts, maxTotalRetriesDuration);
    }

    @Override
    public void close() {

    }
}
