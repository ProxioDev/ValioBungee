/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.commands.utils;

import co.aikar.commands.CommandIssuer;
import net.kyori.adventure.text.Component;


public abstract class CommandPlatformHelper {

    private static CommandPlatformHelper SINGLETON;

    public abstract void sendMessage(CommandIssuer issuer, Component component);

    public static void init(CommandPlatformHelper platformHelper) {
        if (SINGLETON != null) {
            throw new IllegalStateException("tried to re init Platform Helper");
        }
        SINGLETON = platformHelper;
    }

    public static CommandPlatformHelper getPlatformHelper() {
        return SINGLETON;
    }

}
