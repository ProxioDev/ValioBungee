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
import com.google.common.primitives.Ints;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@CommandAlias("rb|redisbungee")
@CommandPermission("redisbungee.command.use")
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



    private List<Map.Entry<String, Integer>> subListProxies(List<Map.Entry<String, Integer>> data, int currentPage, int pageSize) {
        return data.subList(((currentPage * pageSize) - pageSize), Ints.constrainToRange(currentPage * pageSize, 0, data.size()));

    }
    @Subcommand("show")
    public void showProxies(CommandIssuer issuer, String[] args) {
        final String closer = "<color:gold>========================================";
        final String pageTop = "<color:yellow>Page: <color:green><current>/<max> <color:yellow>Network ID: <color:green><network> Proxies online: <proxies>";
        final String proxy = "<color:yellow><proxy><here> : <color:green><players> online";
        final String proxyHere = " (#) ";
        final String nextPage = ">>>>>";
        final String previousPage = "<<<<< ";
        final String pageInvalid = "<color:red>invalid page";
        final String noProxies = "<color:red>No proxies were found :(";

        final int pageSize = 16;

        int currentPage;
        if (args.length > 0) {
            try {
                currentPage = Integer.parseInt(args[0]);
                if (currentPage < 1) currentPage = 1;
            } catch (NumberFormatException e) {
                sendMessage(issuer, MiniMessage.miniMessage().deserialize(pageInvalid));
                return;
            }
        } else currentPage = 1;

        var data = new ArrayList<>(plugin.proxyDataManager().eachProxyCount().entrySet());
        for (int i = 0; i < 2; i++) {
            data.addAll(data);
        }

        // there is no way this runs because there is always an heartbeat.
        // if not could be some shenanigans done by devs :P
        if (data.isEmpty()) {
            sendMessage(issuer, MiniMessage.miniMessage().deserialize(noProxies));
            return;
        }
        // compute the total pages
        final int maxPages = (data.size() / pageSize);
        if (currentPage > maxPages) currentPage = maxPages;

        System.out.println((currentPage * pageSize) - pageSize);
        var subList = subListProxies(data, currentPage, pageSize);
        TextComponent.Builder builder = Component.text();
        builder.append(MiniMessage.miniMessage().deserialize(closer)).appendNewline();
        builder.append(MiniMessage.miniMessage().deserialize(pageTop,
                Placeholder.component("current", Component.text(currentPage)),
                Placeholder.component("max", Component.text(maxPages)),
                Placeholder.component("network", Component.text(plugin.proxyDataManager().networkId())),
                Placeholder.component("proxies", Component.text(data.size()))


                )).appendNewline();
        int left = pageSize;
        for (Map.Entry<String, Integer> entrySet : subList) {
            builder.append(MiniMessage.miniMessage().deserialize(proxy,

                    Placeholder.component("proxy", Component.text(entrySet.getKey())),
                    Placeholder.component("here", Component.text(plugin.proxyDataManager().proxyId().equals(entrySet.getKey()) ? proxyHere : "")),
                    Placeholder.component("players", Component.text(entrySet.getValue()))

            )).appendNewline();
            left--;
        }
        while(left > 0) {
            builder.appendNewline();
            left--;
        }
        if (currentPage > 1) {
            builder.append(MiniMessage.miniMessage().deserialize(previousPage)
                    .clickEvent(ClickEvent.runCommand("/rb show " + (currentPage - 1))));
        }
        if (subList.size() == pageSize && !subListProxies(data, currentPage + 1, pageSize).isEmpty()) {
            builder.append(MiniMessage.miniMessage().deserialize(nextPage)
                    .clickEvent(ClickEvent.runCommand("/rb show " + (currentPage + 1))));
        }
        builder.appendNewline();
        builder.append(MiniMessage.miniMessage().deserialize(closer));
        sendMessage(issuer, builder.build());



    }
}
