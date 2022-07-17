package com.imaginarycode.minecraft.redisbungee.api.util.lua;

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;

public class LuaManager {
    private final RedisBungeePlugin<?> plugin;

    public LuaManager(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }



    public Script createScript(String script) {
        RedisTask<Script> scriptRedisTask = new RedisTask<Script>(plugin.getApi()) {
            @Override
            public Script jedisTask(Jedis jedis) {
                String hash = jedis.scriptLoad(script);
                return new Script(script, hash);
            }

            @Override
            public Script clusterJedisTask(JedisCluster jedisCluster) {
                String hash = jedisCluster.scriptLoad(script,  "0");
                return new Script(script, hash);
            }
        };
        return scriptRedisTask.execute();
    }

    public class Script {
        private final String script;
        private final String hashed;

        public Script(String script, String hashed) {
            this.script = script;
            this.hashed = hashed;
        }

        public String getScript() {
            return script;
        }

        public String getHashed() {
            return hashed;
        }

        public Object eval(List<String> keys, List<String> args) {
            RedisTask<Object> objectRedisTask = new RedisTask<Object>(plugin.getApi()) {
                @Override
                public Object jedisTask(Jedis jedis) {
                    Object data;
                    try {
                        data = jedis.evalsha(hashed, keys, args);
                    } catch (JedisDataException e) {
                        if (e.getMessage().startsWith("NOSCRIPT")) {
                            data = jedis.eval(script, keys, args);
                        } else {
                            throw e;
                        }
                    }
                    return data;
                }

                @Override
                public Object clusterJedisTask(JedisCluster jedisCluster) {
                    Object data;
                    try {
                        data = jedisCluster.evalsha(hashed, keys, args);
                    } catch (JedisDataException e) {
                        if (e.getMessage().startsWith("NOSCRIPT")) {
                            data = jedisCluster.eval(script, keys, args);
                        } else {
                            throw e;
                        }
                    }
                    return data;
                }
            };


            return objectRedisTask.execute();
        }
    }
}
