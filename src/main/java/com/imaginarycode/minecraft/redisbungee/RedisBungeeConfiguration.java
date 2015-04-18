/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
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
    private final boolean canonicalGlist;
    @Getter
    private final List<InetAddress> exemptAddresses;

    public RedisBungeeConfiguration(JedisPool pool, Configuration configuration) {
        this.pool = pool;
        this.serverId = configuration.getString("server-id");
        this.registerBungeeCommands = configuration.getBoolean("register-bungee-commands", true);
        this.canonicalGlist = configuration.getBoolean("canonical-glist", true);

        List<String> stringified = configuration.getStringList("exempt-ip-addresses");
        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();

        for (String s : stringified) {
            addressBuilder.add(InetAddresses.forString(s));
        }

        this.exemptAddresses = addressBuilder.build();
    }
}
