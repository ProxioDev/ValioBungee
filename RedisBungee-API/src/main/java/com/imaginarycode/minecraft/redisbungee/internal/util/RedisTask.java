package com.imaginarycode.minecraft.redisbungee.internal.util;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.internal.summoners.ClusterJedisSummoner;
import com.imaginarycode.minecraft.redisbungee.internal.summoners.JedisSummoner;
import com.imaginarycode.minecraft.redisbungee.internal.summoners.Summoner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.concurrent.Callable;

public abstract class RedisTask<V> implements Runnable, Callable<V> {

    private final Summoner<?> summoner;
    private final RedisBungeeAPI api;

    @Override
    public V call() throws Exception {
        return execute();
    }

    public RedisTask(RedisBungeeAPI api) {
        this.api = api;
        this.summoner = api.getSummoner();
    }

    public abstract V singleJedisTask(Jedis jedis);

    public abstract V clusterJedisTask(JedisCluster jedisCluster);

    @Override
    public void run() {
        this.execute();
    }

    public V execute(){
        if (api.getMode() == RedisBungeeMode.SINGLE) {
            JedisSummoner jedisSummoner = (JedisSummoner) summoner;
            try (Jedis jedis = jedisSummoner.obtainResource()) {
                return this.singleJedisTask(jedis);
            }

        } else if (api.getMode() == RedisBungeeMode.CLUSTER) {
            ClusterJedisSummoner clusterJedisSummoner = (ClusterJedisSummoner) summoner;
            return this.clusterJedisTask(clusterJedisSummoner.obtainResource());
        }
        return null;
    }

}
