package com.imaginarycode.minecraft.redisbungee.api.summoners;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.providers.PooledConnectionProvider;


public class NotClosableJedisPooled extends JedisPooled {
    NotClosableJedisPooled(PooledConnectionProvider provider) {
        super(provider);
    }

    @Override
    public void close() {

    }
}
