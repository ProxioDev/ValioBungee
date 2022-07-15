package com.imaginarycode.minecraft.redisbungee.internal.summoners;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;

public class SinglePoolJedisSummoner implements JedisSummoner {
    final JedisPool jedisPool;

    public SinglePoolJedisSummoner(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Jedis requestJedis() {
        return this.jedisPool.getResource();
    }

    @Override
    public boolean isJedisAvailable() {
        return !this.jedisPool.isClosed();
    }

    @Override
    public JedisPool getJedisPool() {
        return this.jedisPool;
    }

    @Override
    public void close() throws IOException {
        jedisPool.close();
    }
}
