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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

public class RedisBungeeConfiguration {

    public enum MessageType {
        LOGGED_IN_OTHER_LOCATION
    }

    private final ImmutableMap<MessageType, String> messages;
    public static final int CONFIG_VERSION = 1;
    private final String proxyId;
    private final List<InetAddress> exemptAddresses;

    private final boolean registerLegacyCommands;

    private final boolean overrideBungeeCommands;

    public RedisBungeeConfiguration(String proxyId, List<String> exemptAddresses, boolean registerLegacyCommands, boolean overrideBungeeCommands, ImmutableMap<MessageType, String> messages) {
        this.proxyId = proxyId;
        this.messages = messages;
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

    public ImmutableMap<MessageType, String> getMessages() {
        return messages;
    }
}
