package com.imaginarycode.minecraft.redisbungee.api;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.util.List;

public class RedisBungeeConfiguration {
    private final String serverId;
    private final List<InetAddress> exemptAddresses;
    private final boolean overrideBungeeCommands;
    private static RedisBungeeConfiguration config;

    public RedisBungeeConfiguration(String serverId, List<String> exemptAddresses, boolean overrideBungeeCommands) {
        this.serverId = serverId;

        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();
        for (String s : exemptAddresses) {
            addressBuilder.add(InetAddresses.forString(s));
        }
        this.exemptAddresses = addressBuilder.build();
        config = this;
        this.overrideBungeeCommands = overrideBungeeCommands;
    }

    public String getServerId() {
        return serverId;
    }

    public List<InetAddress> getExemptAddresses() {
        return exemptAddresses;
    }

    public boolean doOverrideBungeeCommands() {
        return overrideBungeeCommands;
    }

    public static RedisBungeeConfiguration getConfig() {
        return config;
    }
}
