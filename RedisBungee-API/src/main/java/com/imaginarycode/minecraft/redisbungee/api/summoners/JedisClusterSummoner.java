package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.io.IOException;
import java.time.Duration;

public class JedisClusterSummoner implements Summoner<JedisCluster> {
    public final ClusterConnectionProvider clusterConnectionProvider;

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
        return new NotClosableJedisCluster(this.clusterConnectionProvider, 60, Duration.ofSeconds(30000));
    }
}
