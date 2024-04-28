/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.commands.legacy;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.imaginarycode.minecraft.redisbungee.commands.utils.AdventureBaseCommand;

@CommandAlias("serverid|rserverid")
@CommandPermission("redisbungee.command.serverid")
public class CommandServerId extends AdventureBaseCommand {


    private final LegacyRedisBungeeCommands rootCommand;

    public CommandServerId(LegacyRedisBungeeCommands rootCommand) {
        this.rootCommand = rootCommand;
    }
    @Default
    public void serverId(CommandIssuer issuer) {
        this.rootCommand.serverId(issuer);
    }
}
