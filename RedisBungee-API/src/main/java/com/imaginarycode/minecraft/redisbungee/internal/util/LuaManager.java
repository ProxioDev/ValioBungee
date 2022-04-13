package com.imaginarycode.minecraft.redisbungee.internal.util;

import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;

public class LuaManager {
    private final RedisBungeePlugin<?> plugin;

    public LuaManager(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

    public Script createScript(String script) {
        try (Jedis jedis = plugin.requestJedis()) {
            String hash = jedis.scriptLoad(script);
            return new Script(script, hash);
        }
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
            Object data;

            try (Jedis jedis = plugin.requestJedis()) {
                try {
                    data = jedis.evalsha(hashed, keys, args);
                } catch (JedisDataException e) {
                    if (e.getMessage().startsWith("NOSCRIPT")) {
                        data = jedis.eval(script, keys, args);
                    } else {
                        throw e;
                    }
                }
            }

            return data;
        }
    }
}
