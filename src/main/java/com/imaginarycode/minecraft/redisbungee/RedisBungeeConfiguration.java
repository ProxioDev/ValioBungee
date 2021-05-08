package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

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
        String serverId = configuration.getString("server-id");
        if (serverId == null || serverId.isEmpty()) {
            serverId = UUID.randomUUID().toString();
        }
        this.serverId = serverId;

        this.registerBungeeCommands = configuration.getBoolean("register-bungee-commands", true);

        List<String> stringified = configuration.getStringList("exempt-ip-addresses");
        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();

        for (String s : stringified) {
            addressBuilder.add(InetAddresses.forString(s));
        }

        this.exemptAddresses = addressBuilder.build();
    }
}
