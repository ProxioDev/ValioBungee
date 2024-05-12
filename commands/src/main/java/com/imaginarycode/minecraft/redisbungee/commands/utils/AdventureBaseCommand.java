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

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import net.kyori.adventure.text.Component;

/**
 *  this just dumb class that wraps the adventure stuff into base command
 */
public abstract class AdventureBaseCommand extends BaseCommand {

    public static void sendMessage(CommandIssuer issuer, Component component) {
        CommandPlatformHelper.getPlatformHelper().sendMessage(issuer, component);
    }

}
