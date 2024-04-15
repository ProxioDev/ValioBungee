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
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.commands.utils.AdventureBaseCommand;
import com.imaginarycode.minecraft.redisbungee.commands.utils.StopperUUIDCleanupTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Date;

@CommandAlias("rb|redisbungee")
@CommandPermission("redisbungee.use")
public class CommandRedisBungee extends AdventureBaseCommand {

    private final RedisBungeePlugin<?> plugin;

    public CommandRedisBungee(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
    }

    @Default
    @Subcommand("info|version|git")
    public void info(CommandIssuer issuer) {
        final String message = """
        <color:aqua>This proxy is running RedisBungee Limework's fork
        <color:gold>========================================
        <color:aqua>RedisBungee version: <color:green><version>
        <color:aqua>Build date: <color:green><build-date>
        <color:aqua>Commit: <color:green><commit>
        <color:gold>========================================
        <color:gold>run /rb help for more commands""";
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
    // <color:aqua>......: <color:green>......
    @HelpCommand
    public void help(CommandIssuer issuer) {
        final String message = """
        <color:gold>========================================
        <color:aqua>/rb info: <color:green>shows info of this version.
        <color:aqua>/rb help: <color:green>shows this page.
        <color:aqua>/rb clean: <color:green>cleans up the uuid cache
        <color:red><bold>WARNING...</bold> <color:white>command above could cause performance issues
        <color:aqua>/rb show: <color:green>shows list of proxies with player count
        <color:gold>========================================
        <color:gold>run /rb help for more commands""";
        sendMessage(issuer, MiniMessage.miniMessage().deserialize(message));
    }
    @Subcommand("clean")
    @Private
    public void cleanUp(CommandIssuer issuer) {
        if (StopperUUIDCleanupTask.isRunning) {
            sendMessage(issuer,
                    Component.text("cleanup is currently running!").color(NamedTextColor.RED));
            return;
        }
        sendMessage(issuer,
                Component.text("cleanup is Starting, you should see the output status in the proxy console").color(NamedTextColor.GOLD));
        plugin.executeAsync(new StopperUUIDCleanupTask(plugin));
    }


    @Subcommand("show")
    public void showProxies(CommandIssuer issuer) {
        final String message = """
        <color:gold>========================================
        <data><color:gold>========================================""";

        final String proxyPlayersMessage = "<color:yellow><proxy><here> : <color:green><players> online";


        TextComponent.Builder builder = Component.text();

        plugin.proxyDataManager().eachProxyCount().forEach((proxy, players)
                -> builder.append(
                MiniMessage.miniMessage()
                        .deserialize(proxyPlayersMessage,
                                Placeholder.component("here", Component.text(plugin.proxyDataManager().proxyId().equals(proxy) ? " (#) " : "")),
                                Placeholder.component("proxy", Component.text(proxy)),
                                Placeholder.component("players", Component.text(players))
                        )
                        .appendNewline()));

        sendMessage(issuer, MiniMessage.miniMessage().deserialize(message, Placeholder.component("data", builder)));


    }
}
