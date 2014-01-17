/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.Multimap;
import lombok.NonNull;
import net.md_5.bungee.api.config.ServerInfo;

import java.net.InetAddress;
import java.util.Set;

/**
 * This class exposes some internal RedisBungee functions. You obtain an instance of this object by invoking {@link RedisBungee#getApi()}.
 *
 * @author tuxed
 * @since 0.2.3
 */
public class RedisBungeeAPI {
    private final RedisBungee plugin;

    RedisBungeeAPI(RedisBungee plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a combined count of all players on this network.
     *
     * @return a count of all players found
     */
    public final int getPlayerCount() {
        return plugin.getCount();
    }

    /**
     * Get the last time a player was on. If the player is currently online, this will return 0. If the player has not been recorded,
     * this will return -1. Otherwise it will return a value in milliseconds.
     *
     * @param player a player name
     * @return the last time a player was on, if online returns a 0
     */
    public final long getLastOnline(@NonNull String player) {
        return plugin.getLastOnline(player);
    }

    /**
     * Get the server where the specified player is playing. This function also deals with the case of local players
     * as well, and will return local information on them.
     *
     * @param player a player name
     * @return a {@link net.md_5.bungee.api.config.ServerInfo} for the server the player is on.
     */
    public final ServerInfo getServerFor(@NonNull String player) {
        return plugin.getServerFor(player);
    }

    /**
     * Get a combined list of players on this network.
     * <p/>
     * <strong>Note that this function returns an immutable {@link java.util.Set}.</strong>
     *
     * @return a Set with all players found
     */
    public final Set<String> getPlayersOnline() {
        return plugin.getPlayers();
    }

    /**
     * Get a full list of players on all servers.
     * @return a immutable Multimap with all players found on this server
     */
    public final Multimap<String, String> getServerToPlayers() {
        return plugin.serversToPlayers();
    }

    /**
     * Get a list of players on the server with the given name.
     * @param server a server name
     * @return a Set with all players found on this server
     */
    public final Set<String> getPlayersOnServer(@NonNull String server) {
        return plugin.getPlayersOnServer(server);
    }

    /**
     * Convenience method: Checks if the specified player is online.
     *
     * @param player a player name
     * @return if the server is online
     */
    public final boolean isPlayerOnline(@NonNull String player) {
        return getLastOnline(player) == 0;
    }

    /**
     * Get the {@link java.net.InetAddress} associated with this player.
     *
     * @return an {@link java.net.InetAddress} if the player is online, null otherwise
     */
    public final InetAddress getPlayerIp(@NonNull String player) {
        return plugin.getIpAddress(player);
    }

    /**
     * Sends a proxy command to all proxies.
     * @param command the command to send and execute
     */
    public final void sendProxyCommand(@NonNull String command) {
        plugin.sendProxyCommand("allservers", command);
    }

    /**
     * Sends a proxy command to the proxy with the given ID.
     * @param proxyId a proxy ID
     * @param command the command to send and execute
     */
    public final void sendProxyCommand(@NonNull String proxyId, @NonNull String command) {
        plugin.sendProxyCommand(proxyId, command);
    }

    /**
     * Get the current BungeeCord server ID for this server.
     * @return the current server ID
     */
    public final String getServerId() {
        return RedisBungee.getConfiguration().getString("server-id");
    }
}
