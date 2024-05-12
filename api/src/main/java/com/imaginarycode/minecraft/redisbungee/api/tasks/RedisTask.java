/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.tasks;

import com.imaginarycode.minecraft.redisbungee.AbstractRedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisClusterSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisPooledSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import redis.clients.jedis.UnifiedJedis;

import java.util.concurrent.Callable;

/**
 * Since Jedis now have UnifiedJedis which basically extended by cluster / single connections classes
 * can help us to have shared code.
 */
public abstract class RedisTask<V> implements Runnable, Callable<V> {

    protected final Summoner<?> summoner;

    protected final RedisBungeeMode mode;

    @Override
    public V call() throws Exception {
        return this.execute();
    }

    public RedisTask(AbstractRedisBungeeAPI api) {
        this.summoner = api.getSummoner();
        this.mode = api.getMode();
    }

    public RedisTask(RedisBungeePlugin<?> plugin) {
        this.summoner = plugin.getSummoner();
        this.mode = plugin.getRedisBungeeMode();
    }

    public abstract V unifiedJedisTask(UnifiedJedis unifiedJedis);

    @Override
    public void run() {
        this.execute();
    }

    public V execute() {
        // JedisCluster, JedisPooled in fact is just UnifiedJedis does not need new instance since its single instance anyway.
        if (mode == RedisBungeeMode.SINGLE) {
            JedisPooledSummoner jedisSummoner = (JedisPooledSummoner) summoner;
            return this.unifiedJedisTask(jedisSummoner.obtainResource());
        } else if (mode == RedisBungeeMode.CLUSTER) {
            JedisClusterSummoner jedisClusterSummoner = (JedisClusterSummoner) summoner;
            return this.unifiedJedisTask(jedisClusterSummoner.obtainResource());
        }
        return null;
    }

}
