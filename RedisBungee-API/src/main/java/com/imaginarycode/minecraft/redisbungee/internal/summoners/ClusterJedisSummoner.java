package com.imaginarycode.minecraft.redisbungee.internal.summoners;

import redis.clients.jedis.JedisCluster;

import java.io.IOException;

public class ClusterJedisSummoner implements Summoner<JedisCluster> {
    public final JedisCluster jedisCluster;
    private boolean closed = false;

    public ClusterJedisSummoner(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
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
