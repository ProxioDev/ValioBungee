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
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * This class contains subclasses that are used for the commands RedisBungee overrides or includes: /glist, /find and /lastseen.
 * <p/>
 * All classes use the {@link RedisBungeeAPI}.
 *
 * @author tuxed
 * @since 0.2.3
 */
public class RedisBungeeCommands {
    private static final BaseComponent[] NO_PLAYER_SPECIFIED =
            new ComponentBuilder("").color(ChatColor.RED).append("You must specify a player name.").create();
    private static final BaseComponent[] PLAYER_NOT_FOUND =
            new ComponentBuilder("").color(ChatColor.RED).append("No such player found.").create();

    public static class GlistCommand extends Command {
        protected GlistCommand() {
            super("glist", "bungeecord.command.list", "redisbungee");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            int count = RedisBungee.getApi().getPlayerCount();
            BaseComponent[] playersOnline = new ComponentBuilder("").color(ChatColor.YELLOW).append(String.valueOf(count))
                    .append(" player(s) are currently online.").create();
            if (args.length > 0 && args[0].equals("showall")) {
                if (RedisBungee.getConfiguration().isCanonicalGlist()) {
                    Multimap<String, String> serverToPlayers = HashMultimap.create();
                    for (String p : RedisBungee.getApi().getPlayersOnline()) {
                        ServerInfo si = RedisBungee.getApi().getServerFor(p);
                        if (si != null)
                            serverToPlayers.put(si.getName(), p);
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
                        serverPlayers.setText(Joiner.on(", ").join(serverToPlayers.get(server)));
                        sender.sendMessage(serverName, serverCount, serverPlayers);
                    }
                } else {
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.YELLOW).append("Players: ")
                            .append(Joiner.on(", ").join(RedisBungee.getApi().getPlayersOnline())).create());
                }
                sender.sendMessage(playersOnline);
            } else {
                sender.sendMessage(playersOnline);
                sender.sendMessage(new ComponentBuilder("").color(ChatColor.YELLOW).append("To see all players online, use /glist showall.").create());
            }
        }
    }

    public static class FindCommand extends Command {
        protected FindCommand() {
            super("find", "bungeecord.command.find");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                ServerInfo si = RedisBungee.getApi().getServerFor(args[0]);
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
    }

    public static class LastSeenCommand extends Command {
        protected LastSeenCommand() {
            super("lastseen", "redisbungee.command.lastseen");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                long secs = RedisBungee.getApi().getLastOnline(args[0]);
                TextComponent message = new TextComponent();
                if (secs == 0) {
                    message.setColor(ChatColor.GREEN);
                    message.setText(args[0] + " is currently online.");
                    sender.sendMessage(message);
                } else if (secs != -1) {
                    message.setColor(ChatColor.BLUE);
                    message.setText(args[0] + " was last online on " + new SimpleDateFormat().format(TimeUnit.SECONDS.toMillis(secs)) + ".");
                    sender.sendMessage(message);
                } else {
                    message.setColor(ChatColor.RED);
                    message.setText(args[0] + " has never been online.");
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(NO_PLAYER_SPECIFIED);
            }
        }
    }

    public static class IpCommand extends Command {
        protected IpCommand() {
            super("ip", "redisbungee.command.ip", "playerip");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                InetAddress ia = RedisBungee.getApi().getPlayerIp(args[0]);
                if (ia != null) {
                    TextComponent message = new TextComponent();
                    message.setColor(ChatColor.GREEN);
                    message.setText(args[0] + " is connected from " + ia.toString() + ".");
                } else {
                    sender.sendMessage(PLAYER_NOT_FOUND);
                }
            } else {
                sender.sendMessage(NO_PLAYER_SPECIFIED);
            }
        }
    }
}
