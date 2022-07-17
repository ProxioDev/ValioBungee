package com.imaginarycode.minecraft.redisbungee.api;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.util.List;

public class RedisBungeeConfiguration {
    private final String proxyId;
    private final List<InetAddress> exemptAddresses;
    private final boolean overrideBungeeCommands;
    private static RedisBungeeConfiguration config;

    public RedisBungeeConfiguration(String proxyId, List<String> exemptAddresses, boolean overrideBungeeCommands) {
        this.proxyId = proxyId;

        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();
        for (String s : exemptAddresses) {
            addressBuilder.add(InetAddresses.forString(s));
        }
        this.exemptAddresses = addressBuilder.build();
        config = this;
        this.overrideBungeeCommands = overrideBungeeCommands;
    }

    public String getProxyId() {
        return proxyId;
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
