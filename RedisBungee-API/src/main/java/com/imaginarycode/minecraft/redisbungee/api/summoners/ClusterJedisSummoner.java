package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.JedisCluster;

import java.io.IOException;

public class ClusterJedisSummoner implements Summoner<JedisCluster> {
    public final JedisCluster jedisCluster;
    private boolean closed = false;

    public ClusterJedisSummoner(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        // test the connection
        jedisCluster.set("random_data", "0");
        jedisCluster.del("random_data");
    }

    @Override
    public JedisCluster obtainResource() {
        return jedisCluster;
    }

    @Override
    public boolean isAvailable() {
        return !closed;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        jedisCluster.close();
    }
}
