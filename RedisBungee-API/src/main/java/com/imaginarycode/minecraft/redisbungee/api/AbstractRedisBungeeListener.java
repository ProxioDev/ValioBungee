/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api;


import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.ByteArrayDataOutput;
import com.google.gson.Gson;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractRedisBungeeListener<LE, PLE, PD, SC, PP, PM, PS> {

    protected final RedisBungeePlugin<?> plugin;
    protected final List<InetAddress> exemptAddresses;
    protected final Gson gson = new Gson();

    public AbstractRedisBungeeListener(RedisBungeePlugin<?> plugin, List<InetAddress> exemptAddresses) {
        this.plugin = plugin;
        this.exemptAddresses = exemptAddresses;
    }

    public void onLogin(LE event) {}

    public abstract void onPostLogin(PLE event);

    public abstract void onPlayerDisconnect(PD event);

    public abstract void onServerChange(SC event);

    public abstract void onPing(PP event);

    public abstract void onPluginMessage(PM event);

    public abstract void onPubSubMessage(PS event);


}
