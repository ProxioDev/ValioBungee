package com.imaginarycode.minecraft.redisbungee.util;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import lombok.AllArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.concurrent.Callable;
import java.util.logging.Level;

@AllArgsConstructor
public abstract class RedisCallable<T> implements Callable<T>, Runnable {
    private final RedisBungee plugin;

    @Override
    public T call() {
        return run(false);
    }

    public void run() {
        call();
    }

    private T run(boolean retry) {
        try (Jedis jedis = plugin.getPool().getResource()) {
            return call(jedis);
        } catch (JedisConnectionException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get connection", e);

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