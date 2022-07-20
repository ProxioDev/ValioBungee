package com.imaginarycode.minecraft.redisbungee.api.tasks;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.summoners.ClusterJedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.concurrent.Callable;

public abstract class RedisTask<V> implements Runnable, Callable<V> {

    protected final Summoner<?> summoner;
    protected final RedisBungeeAPI api;
    protected Jedis jedis;
    protected RedisBungeePlugin<?> plugin;

    @Override
    public V call() throws Exception {
        return execute();
    }

    public RedisTask(RedisBungeeAPI api) {
        this.api = api;
        this.summoner = api.getSummoner();
    }

    public RedisTask(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
        this.api = plugin.getApi();
        this.summoner = api.getSummoner();
    }

    // way to reuse jedis inside another RedisTask object
    public RedisTask(RedisBungeeAPI api, Jedis jedis) {
        this.api = api;
        this.summoner = api.getSummoner();
        this.jedis = jedis;
    }

    // way to reuse jedis inside another RedisTask object
    public RedisTask(RedisBungeePlugin<?> plugin, Jedis jedis) {
        this.plugin = plugin;
        this.api = plugin.getApi();
        this.summoner = api.getSummoner();
        this.jedis = jedis;
    }


    public abstract V jedisTask(Jedis jedis);

    public abstract V clusterJedisTask(JedisCluster jedisCluster);

    @Override
    public void run() {
        this.execute();
    }

    public V execute(){
        if (api.getMode() == RedisBungeeMode.SINGLE) {
            if (this.jedis != null){
                return this.jedisTask(this.jedis);
            }
            JedisSummoner jedisSummoner = (JedisSummoner) summoner;
            try (Jedis newJedis = jedisSummoner.obtainResource()) {
                return this.jedisTask(newJedis);
            }

        } else if (api.getMode() == RedisBungeeMode.CLUSTER) {
            // Jedis cluster does not need new instance since its single instance anyways.
            ClusterJedisSummoner clusterJedisSummoner = (ClusterJedisSummoner) summoner;
            return this.clusterJedisTask(clusterJedisSummoner.obtainResource());
        }
        return null;
    }

    public RedisBungeePlugin<?> getPlugin() {
        if (plugin == null) {
            throw new NullPointerException("Plugin is null in the task");
        }
        return plugin;
    }
}
