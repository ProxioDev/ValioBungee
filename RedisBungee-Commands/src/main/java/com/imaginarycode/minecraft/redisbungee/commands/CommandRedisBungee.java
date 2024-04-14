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
import com.imaginarycode.minecraft.redisbungee.Constants;
import com.imaginarycode.minecraft.redisbungee.commands.utils.AdventureBaseCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Date;

@CommandAlias("rb|redisbungee")
@CommandPermission("redisbungee.use")
public class CommandRedisBungee extends AdventureBaseCommand {


    @Default
    @Subcommand("info|version|git")
    public static void info(CommandIssuer issuer) {
        final String message = """
        <color:aqua>This proxy is running RedisBungee Limework's fork
        <color:yellow>========================================
        <color:aqua>RedisBungee version: <color:green><version>
        <color:aqua>Build date: <color:green><build-date>
        <color:aqua>Commit: <color:green><commit>
        <color:yellow>========================================
        <color:yellow>run /rb help for more commands""";
    sendMessage(
        issuer,
        MiniMessage.miniMessage()
            .deserialize(
                message,
                Placeholder.component("version", Component.text(Constants.VERSION)),
                Placeholder.component("build-date", Component.text( new Date(Constants.BUILD_DATE * 1000).toString() )),
                Placeholder.component(
                    "commit",
                    Component.text(Constants.GIT_COMMIT.substring(0, 8))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, Constants.getGithubCommitLink()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click me to open: " + Constants.getGithubCommitLink())))
                )));
    }

    @HelpCommand
    public static void help(CommandIssuer issuer) {
        final String message = """
        <color:yellow>========================================
        <color:aqua>/rb info: <color:green>shows version, build date, git commit hash.
        <color:aqua>/rb help: shows this page.
        <color:aqua>......: <color:green>......
        <color:yellow>========================================
        <color:yellow>run /rb help for more commands""";
        sendMessage(issuer, MiniMessage.miniMessage().deserialize(message));
    }

}
