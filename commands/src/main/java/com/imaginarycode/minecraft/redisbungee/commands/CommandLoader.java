/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.commands;

import co.aikar.commands.CommandManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;

import com.imaginarycode.minecraft.redisbungee.commands.legacy.LegacyRedisBungeeCommands;

public class CommandLoader {

    public static void initCommands(CommandManager<?, ?, ?, ?, ?, ?> commandManager, RedisBungeePlugin<?> plugin) {
        var commandsConfiguration = plugin.configuration().commandsConfiguration();
        if (commandsConfiguration.redisbungeeEnabled()) {
            commandManager.registerCommand(new CommandRedisBungee(plugin));
        }
        if (commandsConfiguration.redisbungeeLegacyEnabled()) {
            commandManager.registerCommand(new LegacyRedisBungeeCommands(commandManager,plugin));
        }

    }

}
