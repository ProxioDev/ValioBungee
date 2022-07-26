package com.imaginarycode.minecraft.redisbungee.commands;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeVelocityPlugin;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;


/**
 * This class contains subclasses that are used for the commands RedisBungee overrides or includes: /glist, /find and /lastseen.
 * <p>
 * All classes use the {@link RedisBungeeAPI}.
 *
 * @author tuxed
 * @since 0.2.3
 */
public class RedisBungeeCommands {

    private static final Component NO_PLAYER_SPECIFIED =
            Component.text("You must specify a player name.", NamedTextColor.RED);
    private static final Component PLAYER_NOT_FOUND =
            Component.text("No such player found.", NamedTextColor.RED);
    private static final Component NO_COMMAND_SPECIFIED =
            Component.text("You must specify a command to be run.", NamedTextColor.RED);

    private static String playerPlural(int num) {
        return num == 1 ? num + " player is" : num + " players are";
    }

    public static class GlistCommand implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public GlistCommand(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(final Invocation invocation) {
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                int count = plugin.getRedisBungeeApi().getPlayerCount();
                Component playersOnline = Component.text(playerPlural(count) + " currently online.", NamedTextColor.YELLOW);
                CommandSource sender = invocation.source();
                if (invocation.arguments().length > 0 && invocation.arguments()[0].equals("showall")) {
                    Multimap<String, UUID> serverToPlayers = plugin.getRedisBungeeApi().getServerToPlayers();
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
                        sender.sendMessage(Component.textOfChildren(serverName, serverCount, serverPlayers));
                    }
                    sender.sendMessage(playersOnline);
                } else {
                    sender.sendMessage(playersOnline);
                    sender.sendMessage(Component.text("To see all players online, use /glist showall.", NamedTextColor.YELLOW));
                }
            }).schedule();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("velocity.command.server");
        }
    }

    public static class FindCommand implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public FindCommand(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(final Invocation invocation) {
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                String[] args = invocation.arguments();
                CommandSource sender = invocation.source();
                if (args.length > 0) {
                    UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                    if (uuid == null) {
                        sender.sendMessage(PLAYER_NOT_FOUND);
                        return;
                    }
                    ServerInfo si = plugin.getProxy().getServer(plugin.getRedisBungeeApi().getServerFor(uuid)).map(RegisteredServer::getServerInfo).orElse(null);
                    if (si != null) {
                        Component message = Component.text(args[0] + " is on " + si.getName() + ".", NamedTextColor.BLUE);
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(PLAYER_NOT_FOUND);
                    }
                } else {
                    sender.sendMessage(NO_PLAYER_SPECIFIED);
                }
            }).schedule();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.find");
        }
    }

    public static class LastSeenCommand implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public LastSeenCommand(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(final Invocation invocation) {
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                String[] args = invocation.arguments();
                CommandSource sender = invocation.source();
                if (args.length > 0) {
                    UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                    if (uuid == null) {
                        sender.sendMessage(PLAYER_NOT_FOUND);
                        return;
                    }
                    long secs = plugin.getRedisBungeeApi().getLastOnline(uuid);
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
                    sender.sendMessage(message.build());
                } else {
                    sender.sendMessage(NO_PLAYER_SPECIFIED);
                }
            }).schedule();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.lastseen");
        }
    }

    public static class IpCommand implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public IpCommand(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(final Invocation invocation) {
            CommandSource sender = invocation.source();
            String[] args = invocation.arguments();
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                if (args.length > 0) {
                    UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                    if (uuid == null) {
                        sender.sendMessage(PLAYER_NOT_FOUND);
                        return;
                    }
                    InetAddress ia = plugin.getRedisBungeeApi().getPlayerIp(uuid);
                    if (ia != null) {
                        TextComponent message = Component.text(args[0] + " is connected from " + ia.toString() + ".", NamedTextColor.GREEN);
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(PLAYER_NOT_FOUND);
                    }
                } else {
                    sender.sendMessage(NO_PLAYER_SPECIFIED);
                }
            }).schedule();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.ip");
        }
    }

    public static class PlayerProxyCommand implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public PlayerProxyCommand(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(final Invocation invocation) {
            CommandSource sender = invocation.source();
            String[] args = invocation.arguments();
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                if (args.length > 0) {
                    UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                    if (uuid == null) {
                        sender.sendMessage(PLAYER_NOT_FOUND);
                        return;
                    }
                    String proxy = plugin.getRedisBungeeApi().getProxy(uuid);
                    if (proxy != null) {
                        TextComponent message = Component.text(args[0] + " is connected to " + proxy + ".", NamedTextColor.GREEN);
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(PLAYER_NOT_FOUND);
                    }
                } else {
                    sender.sendMessage(NO_PLAYER_SPECIFIED);
                }
            }).schedule();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.pproxy");
        }
    }

    public static class SendToAll implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public SendToAll(RedisBungeeVelocityPlugin plugin) {
            //super("sendtoall", "redisbungee.command.sendtoall", "rsendtoall");
            this.plugin = plugin;
        }

        @Override
        public void execute(final Invocation invocation) {
            String[] args = invocation.arguments();
            CommandSource sender = invocation.source();
            if (args.length > 0) {
                String command = Joiner.on(" ").skipNulls().join(args);
                plugin.getRedisBungeeApi().sendProxyCommand(command);
                TextComponent message = Component.text("Sent the command /" + command + " to all proxies.", NamedTextColor.GREEN);
                sender.sendMessage(message);
            } else {
                sender.sendMessage(NO_COMMAND_SPECIFIED);
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.sendtoall");
        }
    }

    public static class ServerId implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public ServerId(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(Invocation invocation) {
            invocation.source().sendMessage(Component.text("You are on " + plugin.getRedisBungeeApi().getProxyId() + ".", NamedTextColor.YELLOW));
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.serverid");
        }
    }

    public static class ServerIds implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public ServerIds(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(Invocation invocation) {
            invocation.source().sendMessage(
                    Component.text("All server IDs: " + Joiner.on(", ").join(plugin.getRedisBungeeApi().getAllProxies()), NamedTextColor.YELLOW));
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.serverids");
        }
    }

    public static class PlistCommand implements SimpleCommand {
        private final RedisBungeeVelocityPlugin plugin;

        public PlistCommand(RedisBungeeVelocityPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(Invocation invocation) {
            CommandSource sender = invocation.source();
            String[] args = invocation.arguments();
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                String proxy = args.length >= 1 ? args[0] : plugin.getConfiguration().getProxyId();
                if (!plugin.getProxiesIds().contains(proxy)) {
                    sender.sendMessage(Component.text(proxy + " is not a valid proxy. See /serverids for valid proxies.", NamedTextColor.RED));
                    return;
                }
                Set<UUID> players = plugin.getRedisBungeeApi().getPlayersOnProxy(proxy);
                Component playersOnline = Component.text(playerPlural(players.size()) + " currently on proxy " + proxy + ".", NamedTextColor.YELLOW);
                if (args.length >= 2 && args[1].equals("showall")) {
                    Multimap<String, UUID> serverToPlayers = plugin.getRedisBungeeApi().getServerToPlayers();
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
                        sender.sendMessage(Component.textOfChildren(serverName, serverCount, serverPlayers));
                    }
                    sender.sendMessage(playersOnline);
                } else {
                    sender.sendMessage(playersOnline);
                    sender.sendMessage(Component.text("To see all players online, use /plist " + proxy + " showall.", NamedTextColor.YELLOW));
                }
            }).schedule();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("redisbungee.command.plist");
        }
    }

}