/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.payloads.proxy;

import com.imaginarycode.minecraft.redisbungee.api.payloads.AbstractPayload;

public class PubSubPayload extends AbstractPayload {

    private final String channel;
    private final String message;


    public PubSubPayload(String proxyId, String channel, String message) {
        super(proxyId);
        this.channel = channel;
        this.message = message;
    }

    public String channel() {
        return channel;
    }

    public String message() {
        return message;
    }
}
