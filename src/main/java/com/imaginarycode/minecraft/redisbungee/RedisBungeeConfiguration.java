package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.util.List;

public class RedisBungeeConfiguration {
    @Getter
    private final JedisPool pool;
    @Getter
    private final String serverId;
    @Getter
    private final boolean registerBungeeCommands;
    @Getter
    private final List<InetAddress> exemptAddresses;

    public RedisBungeeConfiguration(JedisPool pool, Configuration configuration) {
        this.pool = pool;
        this.serverId = configuration.getString("server-id");
        this.registerBungeeCommands = configuration.getBoolean("register-bungee-commands", true);

        List<String> stringified = configuration.getStringList("exempt-ip-addresses");
        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();

        for (String s : stringified) {
            addressBuilder.add(InetAddresses.forString(s));
        }

        this.exemptAddresses = addressBuilder.build();
    }
}
