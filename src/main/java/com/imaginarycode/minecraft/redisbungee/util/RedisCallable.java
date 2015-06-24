/**
 * This is free and unencumbered software released into the public domain.
 * <p/>
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * <p/>
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * <p/>
 * For more information, please refer to <http://unlicense.org/>
 */
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
                run(true);
            }
        }

        throw new RuntimeException("task failed to run");
    }

    protected abstract T call(Jedis jedis);
}