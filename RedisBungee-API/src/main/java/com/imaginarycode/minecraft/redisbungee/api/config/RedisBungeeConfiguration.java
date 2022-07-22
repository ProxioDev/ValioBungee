package com.imaginarycode.minecraft.redisbungee.api.config;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.util.List;

public class RedisBungeeConfiguration {
    public static final int CONFIG_VERSION = 1;
    private final String proxyId;
    private final List<InetAddress> exemptAddresses;

    private final boolean registerLegacyCommands;

    private final boolean overrideBungeeCommands;

    public RedisBungeeConfiguration(String proxyId, List<String> exemptAddresses, boolean registerLegacyCommands, boolean overrideBungeeCommands) {
        this.proxyId = proxyId;

        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();
        for (String s : exemptAddresses) {
            addressBuilder.add(InetAddresses.forString(s));
        }
        this.exemptAddresses = addressBuilder.build();
        this.registerLegacyCommands = registerLegacyCommands;
        this.overrideBungeeCommands = overrideBungeeCommands;
    }

    public String getProxyId() {
        return proxyId;
    }

    public List<InetAddress> getExemptAddresses() {
        return exemptAddresses;
    }

    public boolean doRegisterLegacyCommands() {
        return registerLegacyCommands;
    }

    public boolean doOverrideBungeeCommands() {
        return overrideBungeeCommands;
    }
}
