package com.imaginarycode.minecraft.redisbungee.api;

import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PubSubListener implements Runnable {
    private JedisPubSubHandler jpsh;
    private final Set<String> addedChannels = new HashSet<String>();

    private final RedisBungeePlugin<?> plugin;

    public PubSubListener(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        RedisTask<Void> subTask = new RedisTask<Void>(plugin.getApi()) {
            @Override
            public Void jedisTask(Jedis jedis) {
                try {
                    jpsh = new JedisPubSubHandler(plugin);
                    addedChannels.add("redisbungee-" + plugin.getConfiguration().getProxyId());
                    addedChannels.add("redisbungee-allservers");
                    addedChannels.add("redisbungee-data");
                    jedis.subscribe(jpsh, addedChannels.toArray(new String[0]));
                } catch (Exception e) {
                    // FIXME: Extremely ugly hack
                    // Attempt to unsubscribe this instance and try again.
                    plugin.logWarn("PubSub error, attempting to recover.");
                    try {
                        jpsh.unsubscribe();
                    } catch (Exception e1) {
                        /* This may fail with
                        - java.net.SocketException: Broken pipe
                        - redis.clients.jedis.exceptions.JedisConnectionException: JedisPubSub was not subscribed to a Jedis instance
                        */
                    }
                }
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                try {
                    jpsh = new JedisPubSubHandler(plugin);
                    addedChannels.add("redisbungee-" + plugin.getConfiguration().getProxyId());
                    addedChannels.add("redisbungee-allservers");
                    addedChannels.add("redisbungee-data");
                    jedisCluster.subscribe(jpsh, addedChannels.toArray(new String[0]));
                } catch (Exception e) {
                    // FIXME: Extremely ugly hack
                    // Attempt to unsubscribe this instance and try again.
                    plugin.logWarn("PubSub error, attempting to recover.");
                    try {
                        jpsh.unsubscribe();
                    } catch (Exception e1) {
                        /* This may fail with
                        - java.net.SocketException: Broken pipe
                        - redis.clients.jedis.exceptions.JedisConnectionException: JedisPubSub was not subscribed to a Jedis instance
                        */
                    }
                }
                return null;
            }
        };

        try  {
            subTask.execute();
        } catch (JedisConnectionException e) {
            plugin.logWarn("PubSub error, attempting to recover in 5 secs.");
            plugin.executeAsyncAfter(this, TimeUnit.SECONDS, 5);
        }
    }

    public void addChannel(String... channel) {
        addedChannels.addAll(Arrays.asList(channel));
        jpsh.subscribe(channel);
    }

    public void removeChannel(String... channel) {
        Arrays.asList(channel).forEach(addedChannels::remove);
        jpsh.unsubscribe(channel);
    }

    public void poison() {
        addedChannels.clear();
        jpsh.unsubscribe();
    }
}

