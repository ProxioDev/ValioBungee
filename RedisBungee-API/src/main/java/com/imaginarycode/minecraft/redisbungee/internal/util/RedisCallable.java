package com.imaginarycode.minecraft.redisbungee.internal.util;

import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.concurrent.Callable;



public abstract class RedisCallable<T> implements Callable<T>, Runnable {
    private final RedisBungeePlugin<?> plugin;

    public RedisCallable(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

    @Override
    public T call() {
        return run(false);
    }

    public void run() {
        call();
    }

    private T run(boolean retry) {
        try (Jedis jedis = plugin.requestJedis()) {
            return call(jedis);
        } catch (JedisConnectionException e) {
            plugin.logFatal("Unable to get connection");

            if (!retry) {
                // Wait one second before retrying the task
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    throw new RuntimeException("task failed to run", e1);
                }
                return run(true);
            }
        }

        throw new RuntimeException("task failed to run");
    }

    protected abstract T call(Jedis jedis);
}