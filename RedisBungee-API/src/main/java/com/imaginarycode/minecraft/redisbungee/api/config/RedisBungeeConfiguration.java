/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;
import java.util.List;

public class RedisBungeeConfiguration {

    public static final int CONFIG_VERSION = 2;
    private final String proxyId;
    private final List<InetAddress> exemptAddresses;
    private final boolean registerLegacyCommands;
    private final boolean overrideBungeeCommands;
    private final boolean kickWhenOnline;

    private final boolean handleReconnectToLastServer;
    private final boolean handleMotd;


    public RedisBungeeConfiguration(String proxyId, List<String> exemptAddresses, boolean registerLegacyCommands, boolean overrideBungeeCommands, boolean kickWhenOnline, boolean handleReconnectToLastServer, boolean handleMotd) {
        this.proxyId = proxyId;
        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();
        for (String s : exemptAddresses) {
            addressBuilder.add(InetAddresses.forString(s));
        }
        this.exemptAddresses = addressBuilder.build();
        this.registerLegacyCommands = registerLegacyCommands;
        this.overrideBungeeCommands = overrideBungeeCommands;
        this.kickWhenOnline = kickWhenOnline;
        this.handleReconnectToLastServer = handleReconnectToLastServer;
        this.handleMotd = handleMotd;
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

    public boolean kickWhenOnline() {
        return kickWhenOnline;
    }

    public boolean handleMotd() {
        return this.handleMotd;
    }

    public boolean handleReconnectToLastServer() {
        return this.handleReconnectToLastServer;
    }


}
