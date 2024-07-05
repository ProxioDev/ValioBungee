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
import com.google.common.net.InetAddresses;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.List;

public class RedisBungeeConfiguration {

    private final String proxyId;
    private final List<InetAddress> exemptAddresses;
    private final boolean kickWhenOnline;

    private final boolean handleReconnectToLastServer;
    private final boolean handleMotd;
    private final HandleMotdOrder handleMotdOrder;

    private final CommandsConfiguration commandsConfiguration;
    private final String networkId;


    public RedisBungeeConfiguration(String networkId, String proxyId, List<String> exemptAddresses, boolean kickWhenOnline, boolean handleReconnectToLastServer, boolean handleMotd, HandleMotdOrder handleMotdOrder, CommandsConfiguration commandsConfiguration) {
        this.proxyId = proxyId;
        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();
        for (String s : exemptAddresses) {
            addressBuilder.add(InetAddresses.forString(s));
        }
        this.exemptAddresses = addressBuilder.build();
        this.kickWhenOnline = kickWhenOnline;
        this.handleReconnectToLastServer = handleReconnectToLastServer;
        this.handleMotd = handleMotd;
        this.handleMotdOrder = handleMotdOrder;
        this.commandsConfiguration = commandsConfiguration;
        this.networkId = networkId;
    }

    public String getProxyId() {
        return proxyId;
    }

    public List<InetAddress> getExemptAddresses() {
        return exemptAddresses;
    }

    public boolean kickWhenOnline() {
        return kickWhenOnline;
    }

    public boolean handleMotd() {
        return this.handleMotd;
    }

    public HandleMotdOrder handleMotdOrder() {
        return handleMotdOrder;
    }

    public boolean handleReconnectToLastServer() {
        return this.handleReconnectToLastServer;
    }

    public record CommandsConfiguration(boolean redisbungeeEnabled, boolean redisbungeeLegacyEnabled,
                                        @Nullable LegacySubCommandsConfiguration legacySubCommandsConfiguration) {

    }

    public record LegacySubCommandsConfiguration(boolean findEnabled, boolean glistEnabled, boolean ipEnabled,
                                                 boolean lastseenEnabled, boolean plistEnabled, boolean pproxyEnabled,
                                                 boolean sendtoallEnabled, boolean serveridEnabled,
                                                 boolean serveridsEnabled, boolean installFind, boolean installGlist, boolean installIp,
                                                 boolean installLastseen, boolean installPlist, boolean installPproxy,
                                                 boolean installSendtoall, boolean installServerid,
                                                 boolean installServerids) {
    }

    public CommandsConfiguration commandsConfiguration() {
        return commandsConfiguration;
    }

    public String networkId() {
        return networkId;
    }
}
