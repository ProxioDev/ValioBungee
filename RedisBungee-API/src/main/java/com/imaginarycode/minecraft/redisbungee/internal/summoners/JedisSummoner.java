package com.imaginarycode.minecraft.redisbungee.internal.summoners;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.io.Closeable;


/**
 * This class intended for future release to support redis sentinel or redis clusters
 *
 * @author Ham1255
 * @since 0.7.0
 *
 */
public interface JedisSummoner extends Closeable {

    Jedis requestJedis();

    boolean isJedisAvailable();

}
