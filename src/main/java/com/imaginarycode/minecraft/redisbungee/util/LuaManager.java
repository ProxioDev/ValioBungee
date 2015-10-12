package com.imaginarycode.minecraft.redisbungee.util;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;

@RequiredArgsConstructor
public class LuaManager {
    private final RedisBungee plugin;

    public Script createScript(String script) {
        try (Jedis jedis = plugin.getPool().getResource()) {
            String hash = jedis.scriptLoad(script);
            return new Script(script, hash);
        }
    }

    @RequiredArgsConstructor
    public class Script {
        private final String script;
        private final String hashed;

        public Object eval(List<String> keys, List<String> args) {
            Object data;

            try (Jedis jedis = plugin.getPool().getResource()) {
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
