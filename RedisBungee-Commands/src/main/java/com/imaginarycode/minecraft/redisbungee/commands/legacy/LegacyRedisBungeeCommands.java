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
import co.aikar.commands.CommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.commands.utils.AdventureBaseCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@CommandAlias("rbl|redisbungeelegacy")
@CommandPermission("redisbungee.legacy.use")
public class LegacyRedisBungeeCommands extends AdventureBaseCommand {

    private final RedisBungeePlugin<?> plugin;

    public LegacyRedisBungeeCommands(CommandManager<?, ?, ?, ?, ?, ?> commandManager, RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
        var commands = plugin.configuration().commandsConfiguration().legacySubCommandsConfiguration();
        if (!plugin.configuration().commandsConfiguration().redisbungeeLegacyEnabled()) throw new IllegalStateException("someone tried to init me while disabled!");
        if (commands == null) throw new NullPointerException("commands config is null!!");

        if (commands.installGlist())  commandManager.registerCommand(new CommandGList(this));
        if (commands.installFind())  commandManager.registerCommand(new CommandFind(this));
        if (commands.installIp())  commandManager.registerCommand(new CommandIp(this));
        if (commands.installLastseen())  commandManager.registerCommand(new CommandLastSeen(this));
        if (commands.installPlist())  commandManager.registerCommand(new CommandPlist(this));
        if (commands.installPproxy())  commandManager.registerCommand(new CommandPProxy(this));
        if (commands.installSendtoall())  commandManager.registerCommand(new CommandSendToAll(this));
        if (commands.installServerid())  commandManager.registerCommand(new CommandServerId(this));
        if (commands.installServerids())  commandManager.registerCommand(new CommandServerIds(this));
    }

    private static final Component NO_PLAYER_SPECIFIED =
            Component.text("You must specify a player name.", NamedTextColor.RED);
    private static final Component PLAYER_NOT_FOUND =
            Component.text("No such player found.", NamedTextColor.RED);
    private static final Component NO_COMMAND_SPECIFIED =
            Component.text("You must specify a command to be run.", NamedTextColor.RED);

    private static String playerPlural(int num) {
        return num == 1 ? num + " player is" : num + " players are";
    }

    @Subcommand("glist")
    @CommandPermission("redisbungee.command.glist")
    public void gList(CommandIssuer issuer, String[] args) {
        plugin.executeAsync(() -> {
            int count = plugin.getAbstractRedisBungeeApi().getPlayerCount();
            Component playersOnline = Component.text(playerPlural(count) + " currently online.", NamedTextColor.YELLOW);
            if (args.length > 0 && args[0].equals("showall")) {
                Multimap<String, UUID> serverToPlayers = plugin.getAbstractRedisBungeeApi().getServerToPlayers();
                Multimap<String, String> human = HashMultimap.create();
                serverToPlayers.forEach((key, value) -> {
                    // if for any reason UUID translation fails just return the uuid as name, to make command finish executing.
                    String playerName = plugin.getUuidTranslator().getNameFromUuid(value, false);
                    human.put(key, playerName != null ? playerName : value.toString());
                });
                for (String server : new TreeSet<>(serverToPlayers.keySet())) {
                    Component serverName = Component.text("[" + server + "] ", NamedTextColor.GREEN);
                    Component serverCount = Component.text("(" + serverToPlayers.get(server).size() + "): ", NamedTextColor.YELLOW);
                    Component serverPlayers = Component.text(Joiner.on(", ").join(human.get(server)), NamedTextColor.WHITE);
                    sendMessage(issuer, Component.textOfChildren(serverName, serverCount, serverPlayers));
                }
                sendMessage(issuer, playersOnline);
            } else {
                sendMessage(issuer, playersOnline);
                sendMessage(issuer, Component.text("To see all players online, use /glist showall.", NamedTextColor.YELLOW));
            }

        });
    }

    @Subcommand("find")
    @CommandPermission("redisbungee.command.find")
    public void find(CommandIssuer issuer, String[] args) {
        plugin.executeAsync(() -> {
            if (args.length > 0) {
                UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                if (uuid == null) {
                    sendMessage(issuer, PLAYER_NOT_FOUND);
                    return;
                }
                String proxyId = plugin.playerDataManager().getProxyFor(uuid);
                if (proxyId != null) {
                    String serverId = plugin.playerDataManager().getServerFor(uuid);
                    Component message = Component.text(args[0] + " is on proxy " + proxyId  + " on server " + serverId +".", NamedTextColor.BLUE);
                    sendMessage(issuer, message);
                } else {
                    sendMessage(issuer, PLAYER_NOT_FOUND);
                }
            } else {
                sendMessage(issuer, NO_PLAYER_SPECIFIED);
            }
        });

    }

    @Subcommand("lastseen")
    @CommandPermission("redisbungee.command.lastseen")
    public void lastSeen(CommandIssuer issuer, String[] args) {
        plugin.executeAsync(() -> {
            if (args.length > 0) {
                UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                if (uuid == null) {
                    sendMessage(issuer, PLAYER_NOT_FOUND);
                    return;
                }
                long secs = plugin.getAbstractRedisBungeeApi().getLastOnline(uuid);
                TextComponent.Builder message = Component.text();
                if (secs == 0) {
                    message.color(NamedTextColor.GREEN);
                    message.content(args[0] + " is currently online.");
                } else if (secs != -1) {
                    message.color(NamedTextColor.BLUE);
                    message.content(args[0] + " was last online on " + new SimpleDateFormat().format(secs) + ".");
                } else {
                    message.color(NamedTextColor.RED);
                    message.content(args[0] + " has never been online.");
                }
                sendMessage(issuer, message.build());
            } else {
                sendMessage(issuer, NO_PLAYER_SPECIFIED);
            }


        });
    }

    @Subcommand("ip")
    @CommandPermission("redisbungee.command.ip")
    public void ip(CommandIssuer issuer, String[] args) {
        plugin.executeAsync(() -> {
            if (args.length > 0) {
                UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                if (uuid == null) {
                    sendMessage(issuer, PLAYER_NOT_FOUND);
                    return;
                }
                InetAddress ia = plugin.getAbstractRedisBungeeApi().getPlayerIp(uuid);
                if (ia != null) {
                    TextComponent message = Component.text(args[0] + " is connected from " + ia.toString() + ".", NamedTextColor.GREEN);
                    sendMessage(issuer, message);
                } else {
                    sendMessage(issuer, PLAYER_NOT_FOUND);
                }
            } else {
                sendMessage(issuer, NO_PLAYER_SPECIFIED);
            }
        });
    }

    @Subcommand("pproxy")
    @CommandPermission("redisbungee.command.pproxy")
    public void playerProxy(CommandIssuer issuer, String[] args) {
        plugin.executeAsync(() -> {
            if (args.length > 0) {
                UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                if (uuid == null) {
                    sendMessage(issuer, PLAYER_NOT_FOUND);
                    return;
                }
                String proxy = plugin.getAbstractRedisBungeeApi().getProxy(uuid);
                if (proxy != null) {
                    TextComponent message = Component.text(args[0] + " is connected to " + proxy + ".", NamedTextColor.GREEN);
                    sendMessage(issuer, message);
                } else {
                    sendMessage(issuer, PLAYER_NOT_FOUND);
                }
            } else {
                sendMessage(issuer, NO_PLAYER_SPECIFIED);
            }
        });

    }

    @Subcommand("sendtoall")
    @CommandPermission("redisbungee.command.sendtoall")
    public void sendToAll(CommandIssuer issuer, String[] args) {
        if (args.length > 0) {
            String command = Joiner.on(" ").skipNulls().join(args);
            plugin.getAbstractRedisBungeeApi().sendProxyCommand(command);
            TextComponent message = Component.text("Sent the command /" + command + " to all proxies.", NamedTextColor.GREEN);
            sendMessage(issuer, message);
        } else {
            sendMessage(issuer, NO_COMMAND_SPECIFIED);
        }

    }

    @Subcommand("serverid")
    @CommandPermission("redisbungee.command.serverid")
    public void serverId(CommandIssuer issuer) {
        sendMessage(issuer, Component.text("You are on " + plugin.getAbstractRedisBungeeApi().getProxyId() + ".", NamedTextColor.YELLOW));
    }

    @Subcommand("serverids")
    @CommandPermission("redisbungee.command.serverids")
    public void serverIds(CommandIssuer issuer) {
        sendMessage(issuer, Component.text("All Proxies IDs: " + Joiner.on(", ").join(plugin.getAbstractRedisBungeeApi().getAllProxies()), NamedTextColor.YELLOW));
    }


    @Subcommand("plist")
    @CommandPermission("redisbungee.command.plist")
    public void playerList(CommandIssuer issuer, String[] args) {
        plugin.executeAsync(() -> {
            String proxy = args.length >= 1 ? args[0] : plugin.configuration().getProxyId();
            if (!plugin.proxyDataManager().proxiesIds().contains(proxy)) {
                sendMessage(issuer, Component.text(proxy + " is not a valid proxy. See /serverids for valid proxies.", NamedTextColor.RED));
                return;
            }
            Set<UUID> players = plugin.getAbstractRedisBungeeApi().getPlayersOnProxy(proxy);
            Component playersOnline = Component.text(playerPlural(players.size()) + " currently on proxy " + proxy + ".", NamedTextColor.YELLOW);
            if (args.length >= 2 && args[1].equals("showall")) {
                Multimap<String, UUID> serverToPlayers = plugin.getAbstractRedisBungeeApi().getServerToPlayers();
                Multimap<String, String> human = HashMultimap.create();
                serverToPlayers.forEach((key, value) -> {
                    if (players.contains(value)) {
                        human.put(key, plugin.getUuidTranslator().getNameFromUuid(value, false));
                    }
                });
                for (String server : new TreeSet<>(human.keySet())) {
                    TextComponent serverName = Component.text("[" + server + "] ", NamedTextColor.RED);
                    TextComponent serverCount = Component.text("(" + human.get(server).size() + "): ", NamedTextColor.YELLOW);
                    TextComponent serverPlayers = Component.text(Joiner.on(", ").join(human.get(server)), NamedTextColor.WHITE);
                    sendMessage(issuer, Component.textOfChildren(serverName, serverCount, serverPlayers));
                }
                sendMessage(issuer, playersOnline);
            } else {
                sendMessage(issuer, playersOnline);
                sendMessage(issuer, Component.text("To see all players online, use /plist " + proxy + " showall.", NamedTextColor.YELLOW));
            }
        });

    }

}