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
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import redis.clients.jedis.*;

public abstract class RedisPipelineTask<T> extends RedisTask<T> {


    public RedisPipelineTask(AbstractRedisBungeeAPI api) {
        super(api);
    }

    public RedisPipelineTask(RedisBungeePlugin<?> plugin) {
        super(plugin);
    }


    @Override
    public T unifiedJedisTask(UnifiedJedis unifiedJedis) {
        if (unifiedJedis instanceof JedisPooled pooled) {
            try (Pipeline pipeline = pooled.pipelined()) {
                return doPooledPipeline(pipeline);
            }
        } else if (unifiedJedis instanceof JedisCluster jedisCluster) {
            try (ClusterPipeline pipeline = jedisCluster.pipelined()) {
                return clusterPipeline(pipeline);
            }
        }

        return null;
    }

    public abstract T doPooledPipeline(Pipeline pipeline);

    public abstract T clusterPipeline(ClusterPipeline pipeline);


}
