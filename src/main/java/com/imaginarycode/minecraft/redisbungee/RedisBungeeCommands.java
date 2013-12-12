/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Set;
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
            if (args.length > 0 && args[0].equals("showall")) {
                if (RedisBungee.getConfiguration().isCanonicalGlist()) {
                    Multimap<String, String> serverToPlayers = TreeMultimap.create(Ordering.natural(), Ordering.allEqual());
                    for (String p : RedisBungee.getApi().getPlayersOnline()) {
                        ServerInfo si = RedisBungee.getApi().getServerFor(p);
                        if (si != null)
                            serverToPlayers.put(si.getName(), p);
                    }
                    for (String server : serverToPlayers.keySet())
                        sender.sendMessage(ChatColor.GREEN + "[" + server + "] " + ChatColor.YELLOW + "("
                                + serverToPlayers.get(server).size() + "): " + ChatColor.WHITE
                                + Joiner.on(", ").join(serverToPlayers.get(server)));
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Players: " + Joiner.on(", ").join(RedisBungee.getApi()
                            .getPlayersOnline()));
                }
                sender.sendMessage(ChatColor.YELLOW + String.valueOf(count) + " player(s) are currently online.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + String.valueOf(count) + " player(s) are currently online.");
                sender.sendMessage(ChatColor.YELLOW + "To see all players online, use /glist showall.");
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
                    sender.sendMessage(ChatColor.BLUE + args[0] + " is on " + si.getName() + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "That user is not online.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You must specify a player name.");
            }
        }
    }

    public static class LastSeenCommand extends Command {
        FastDateFormat format = FastDateFormat.getInstance();

        protected LastSeenCommand() {
            super("lastseen", "redisbungee.command.lastseen");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                long secs = RedisBungee.getApi().getLastOnline(args[0]);
                if (secs == 0) {
                    sender.sendMessage(ChatColor.GREEN + args[0] + " is currently online.");
                } else if (secs != -1) {
                    sender.sendMessage(ChatColor.BLUE + args[0] + " was last online on " + format.format(TimeUnit.SECONDS.toMillis(secs)) + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + args[0] + " has never been online.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You must specify a player name.");
            }
        }
    }
}
