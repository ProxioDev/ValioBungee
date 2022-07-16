package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;

public class JedisSummoner implements Summoner<Jedis> {

    private final JedisPool jedisPool;

    public JedisSummoner(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Jedis obtainResource() {
        return jedisPool.getResource();
    }

    public JedisPool getJedisPool() {
        return this.jedisPool;
    }

    @Override
    public boolean isAvailable() {
        return !jedisPool.isClosed();
    }

    @Override
    public void close() throws IOException {
        this.jedisPool.close();

    }
}
