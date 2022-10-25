package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.providers.PooledConnectionProvider;

import java.io.IOException;

public class JedisPooledSummoner implements Summoner<JedisPooled> {

    private final PooledConnectionProvider connectionProvider;
    private final JedisPool jedisPool;

    public JedisPooledSummoner(PooledConnectionProvider connectionProvider, JedisPool jedisPool) {
        this.connectionProvider = connectionProvider;
        this.jedisPool = jedisPool;
        // test connections
        if (jedisPool != null) {
            try (Jedis jedis = this.jedisPool.getResource()) {
                // Test the connection to make sure configuration is right
                jedis.ping();
            }

        }
        final JedisPooled jedisPooled = this.obtainResource();
        jedisPooled.set("random_data", "0");
        jedisPooled.del("random_data");

    }

    @Override
    public JedisPooled obtainResource() {
        // create UnClosable JedisPool *disposable*
        return new NotClosableJedisPooled(this.connectionProvider);
    }

    public JedisPool getCompatibilityJedisPool() {
        return this.jedisPool;
    }

    @Override
    public void close() throws IOException {
        if (this.jedisPool != null) {
            this.jedisPool.close();
        }
        this.connectionProvider.close();

    }
}
