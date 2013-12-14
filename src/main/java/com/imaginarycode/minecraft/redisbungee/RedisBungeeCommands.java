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
                    for (String server : new TreeSet<>(serverToPlayers.keySet()))
                        sender.sendMessage(new ComponentBuilder("").color(ChatColor.GREEN).append("[").append(server)
                                .append("]").color(ChatColor.YELLOW).append("(").append(String.valueOf(serverToPlayers.get(server).size()))
                                .append("): ").color(ChatColor.WHITE).append(Joiner.on(", ").join(serverToPlayers.get(server))).create());
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
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.BLUE).append(args[0]).append(" is on ")
                            .append(si.getName()).append(".").create());
                } else {
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.RED).append("That user is not online.").create());
                }
            } else {
                sender.sendMessage(new ComponentBuilder("").color(ChatColor.RED).append("You must specify a player name.").create());
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
                if (secs == 0) {
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.GREEN).append(args[0]).append(" is currently online.").create());
                } else if (secs != -1) {
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.BLUE).append(args[0]).append(" was last online on ").
                            append(new SimpleDateFormat().format(TimeUnit.SECONDS.toMillis(secs))).append(".").create());
                } else {
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.RED).append(args[0]).append(" has never been online.").create());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You must specify a player name.");
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
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.GREEN).append(args[0]).append(" is connected from ").append(ia.toString()).append(".").create());
                } else {
                    sender.sendMessage(new ComponentBuilder("").color(ChatColor.RED).append("No such player found.").create());
                }
            } else {
                sender.sendMessage(new ComponentBuilder("").color(ChatColor.RED).append("You must specify a player name.").create());
            }
        }
    }
}
