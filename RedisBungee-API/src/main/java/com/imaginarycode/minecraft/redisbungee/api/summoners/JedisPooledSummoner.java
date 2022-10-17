package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;

public class JedisPooledSummoner implements Summoner<JedisPooled> {

    private final JedisPooled jedisPooled;
    private final JedisPool jedisPool;

    public JedisPooledSummoner(JedisPooled jedisPooled, JedisPool jedisPool) {
        this.jedisPooled = jedisPooled;
        this.jedisPool = jedisPool;
        // test connections
        if (jedisPool != null) {
            try (Jedis jedis = this.jedisPool.getResource()) {
                // Test the connection to make sure configuration is right
                jedis.ping();
            }
        }
        jedisPooled.set("random_data", "0");
        jedisPooled.del("random_data");

    }

    @Override
    public JedisPooled obtainResource() {
        return this.jedisPooled;
    }

    public JedisPool getCompatibilityJedisPool() {
        return this.jedisPool;
    }

    @Override
    public void close() throws IOException {
        if (this.jedisPool != null) {
            this.jedisPool.close();
        }
        this.jedisPooled.close();

    }
}
