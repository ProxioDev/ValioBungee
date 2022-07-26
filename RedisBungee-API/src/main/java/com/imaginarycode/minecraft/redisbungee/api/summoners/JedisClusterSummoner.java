package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.JedisCluster;

import java.io.IOException;

public class JedisClusterSummoner implements Summoner<JedisCluster> {
    public final JedisCluster jedisCluster;

    public JedisClusterSummoner(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        // test the connection
        jedisCluster.set("random_data", "0");
        jedisCluster.del("random_data");
    }


    @Override
    public void close() throws IOException {
        jedisCluster.close();
    }

    @Override
    public JedisCluster obtainResource() {
        return this.jedisCluster;
    }
}
