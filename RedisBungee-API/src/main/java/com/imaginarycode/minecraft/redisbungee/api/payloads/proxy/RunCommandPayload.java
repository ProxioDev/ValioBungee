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

public class RunCommandPayload extends AbstractPayload {


    private final String proxyToRun;

    private final String command;


    public RunCommandPayload(String proxyId, String proxyToRun, String command) {
        super(proxyId);
        this.proxyToRun = proxyToRun;
        this.command = command;
    }

    public String proxyToRun() {
        return proxyToRun;
    }

    public String command() {
        return command;
    }
}
