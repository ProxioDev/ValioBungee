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

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.commands.utils.AdventureBaseCommand;
import net.kyori.adventure.text.Component;

import java.util.UUID;

@CommandAlias("rbd|redisbungeedebug")
@CommandPermission("redisbungee.command.debug.use")
@Description("debug commands")
public class CommandRedisBungeeDebug extends AdventureBaseCommand {

    private final RedisBungeePlugin<?> plugin;

    public CommandRedisBungeeDebug(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

    @Subcommand("kickByName")
    @Description("kicks a player from the network by name")
    @Private
    public void kick(CommandIssuer issuer, String playerName) {

        plugin.getAbstractRedisBungeeApi().kickPlayer(playerName, Component.text("debug kick"));
    }

    @Subcommand("kickByUUID")
    @Description("kicks a player from the network by UUID")
    @Private
    public void kick(CommandIssuer issuer, UUID uuid) {

        plugin.getAbstractRedisBungeeApi().kickPlayer(uuid, Component.text("debug kick"));
    }


}
