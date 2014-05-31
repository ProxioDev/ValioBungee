/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * This class contains subclasses that are used for the commands RedisBungee overrides or includes: /glist, /find and /lastseen.
 * <p/>
 * All classes use the {@link RedisBungeeAPI}.
 *
 * @author tuxed
 * @since 0.2.3
 */
class RedisBungeeCommands {
    private static final BaseComponent[] NO_PLAYER_SPECIFIED =
            new ComponentBuilder("You must specify a player name.").color(ChatColor.RED).create();
    private static final BaseComponent[] PLAYER_NOT_FOUND =
            new ComponentBuilder("No such player found.").color(ChatColor.RED).create();
    private static final BaseComponent[] NO_COMMAND_SPECIFIED =
            new ComponentBuilder("You must specify a command to be run.").color(ChatColor.RED).create();

    public static class GlistCommand extends Command {
        private final RedisBungee plugin;

        GlistCommand(RedisBungee plugin) {
            super("glist", "bungeecord.command.list", "redisbungee", "rglist");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    int count = RedisBungee.getApi().getPlayerCount();
                    BaseComponent[] playersOnline = new ComponentBuilder("").color(ChatColor.YELLOW).append(String.valueOf(count))
                            .append(" player(s) are currently online.").create();
                    if (args.length > 0 && args[0].equals("showall")) {
                        if (RedisBungee.getConfiguration().getBoolean("canonical-glist", true)) {
                            Multimap<String, UUID> serverToPlayers = RedisBungee.getApi().getServerToPlayers();
                            Multimap<String, String> human = HashMultimap.create();
                            for (Map.Entry<String, UUID> entry : serverToPlayers.entries()) {
                                human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                            }
                            for (String server : new TreeSet<>(serverToPlayers.keySet())) {
                                TextComponent serverName = new TextComponent();
                                serverName.setColor(ChatColor.GREEN);
                                serverName.setText("[" + server + "] ");
                                TextComponent serverCount = new TextComponent();
                                serverCount.setColor(ChatColor.YELLOW);
                                serverCount.setText("(" + serverToPlayers.get(server).size() + "): ");
                                TextComponent serverPlayers = new TextComponent();
                                serverPlayers.setColor(ChatColor.WHITE);
                                serverPlayers.setText(Joiner.on(", ").join(human.get(server)));
                                sender.sendMessage(serverName, serverCount, serverPlayers);
                            }
                        } else {
                            sender.sendMessage(new ComponentBuilder("Players: " + Joiner.on(", ").join(RedisBungee.getApi().getHumanPlayersOnline()))
                                    .color(ChatColor.YELLOW).create());
                        }
                        sender.sendMessage(playersOnline);
                    } else {
                        sender.sendMessage(playersOnline);
                        sender.sendMessage(new ComponentBuilder("To see all players online, use /glist showall.").color(ChatColor.YELLOW).create());
                    }
                }
            });
        }
    }

    public static class FindCommand extends Command {
        private final RedisBungee plugin;

        FindCommand(RedisBungee plugin) {
            super("find", "bungeecord.command.find", "rfind");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        ServerInfo si = RedisBungee.getApi().getServerFor(uuid);
                        if (si != null) {
                            TextComponent message = new TextComponent();
                            message.setColor(ChatColor.BLUE);
                            message.setText(args[0] + " is on " + si.getName() + ".");
                            sender.sendMessage(message);
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class LastSeenCommand extends Command {
        private final RedisBungee plugin;

        LastSeenCommand(RedisBungee plugin) {
            super("lastseen", "redisbungee.command.lastseen", "rlastseen");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        long secs = RedisBungee.getApi().getLastOnline(uuid);
                        TextComponent message = new TextComponent();
                        if (secs == 0) {
                            message.setColor(ChatColor.GREEN);
                            message.setText(args[0] + " is currently online.");
                        } else if (secs != -1) {
                            message.setColor(ChatColor.BLUE);
                            message.setText(args[0] + " was last online on " + new SimpleDateFormat().format(secs) + ".");
                        } else {
                            message.setColor(ChatColor.RED);
                            message.setText(args[0] + " has never been online.");
                        }
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class IpCommand extends Command {
        private final RedisBungee plugin;

        IpCommand(RedisBungee plugin) {
            super("ip", "redisbungee.command.ip", "playerip", "rip", "rplayerip");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        InetAddress ia = RedisBungee.getApi().getPlayerIp(uuid);
                        if (ia != null) {
                            TextComponent message = new TextComponent();
                            message.setColor(ChatColor.GREEN);
                            message.setText(args[0] + " is connected from " + ia.toString() + ".");
                            sender.sendMessage(message);
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class SendToAll extends Command {
        private final RedisBungee plugin;

        SendToAll(RedisBungee plugin) {
            super("sendtoall", "redisbungee.command.sendtoall", "rsendtoall");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                String command = Joiner.on(" ").skipNulls().join(args);
                RedisBungee.getApi().sendProxyCommand(command);
                TextComponent message = new TextComponent();
                message.setColor(ChatColor.GREEN);
                message.setText("Sent the command /" + command + " to all proxies.");
                sender.sendMessage(message);
            } else {
                sender.sendMessage(NO_COMMAND_SPECIFIED);
            }
        }
    }

    public static class ServerId extends Command {
        private final RedisBungee plugin;

        ServerId(RedisBungee plugin) {
            super("serverid", "redisbungee.command.serverid", "rserverid");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            TextComponent textComponent = new TextComponent();
            textComponent.setText("You are on " + RedisBungee.getApi().getServerId() + ".");
            textComponent.setColor(ChatColor.YELLOW);
            sender.sendMessage(textComponent);
        }
    }

    public static class ServerIds extends Command {
        public ServerIds() {
            super("serverids", "redisbungee.command.serverids");
        }

        @Override
        public void execute(CommandSender sender, String[] strings) {
            TextComponent textComponent = new TextComponent();
            textComponent.setText("All server IDs: " + Joiner.on(", ").join(RedisBungee.getApi().getAllServers()));
            textComponent.setColor(ChatColor.YELLOW);
            sender.sendMessage(textComponent);
        }
    }
}
