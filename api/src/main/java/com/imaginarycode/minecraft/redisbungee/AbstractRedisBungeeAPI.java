/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.util.*;

/**
 * This abstract class is extended by platform plugin to provide some platform specific methods.
 * overall its general contains all methods needed by external usage.
 *
 * @author Ham1255
 * @since 0.8.0
 */
@SuppressWarnings("unused")
public abstract class AbstractRedisBungeeAPI {
    protected final RedisBungeePlugin<?> plugin;
    private static AbstractRedisBungeeAPI abstractRedisBungeeAPI;

    public AbstractRedisBungeeAPI(RedisBungeePlugin<?> plugin) {
        // this does make sure that no one can replace first initiated API class.
        if (abstractRedisBungeeAPI == null) {
            abstractRedisBungeeAPI = this;
        }
        this.plugin = plugin;
    }

    /**
     * Get a combined count of all players on this network.
     *
     * @return a count of all players found
     */
    public final int getPlayerCount() {
        return plugin.proxyDataManager().totalNetworkPlayers();
    }

    /**
     * Get the last time a player was on. If the player is currently online, this will return 0. If the player has not been recorded,
     * this will return -1. Otherwise it will return a value in milliseconds.
     *
     * @param player a player name
     * @return the last time a player was on, if online returns a 0
     */
    public final long getLastOnline(@NonNull UUID player) {
        return plugin.playerDataManager().getLastOnline(player);
    }

    /**
     * Get the server where the specified player is playing. This function also deals with the case of local players
     * as well, and will return local information on them.
     *
     * @param player a player uuid
     * @return a String name for the server the player is on. Can be Null if plugins is doing weird stuff to the proxy internals
     */
    @Nullable
    public final String getServerNameFor(@NonNull UUID player) {
        return plugin.playerDataManager().getServerFor(player);
    }

    /**
     * Get a combined list of players on this network.
     * <p>
     * <strong>Note that this function returns an instance of {@link com.google.common.collect.ImmutableSet}.</strong>
     *
     * @return a Set with all players found
     */
    public final Set<UUID> getPlayersOnline() {
        return plugin.proxyDataManager().networkPlayers();
    }

    /**
     * Get a combined list of players on this network, as a collection of usernames.
     *
     * @return a Set with all players found
     * @see #getNameFromUuid(java.util.UUID)
     * @since 0.3
     */
    public final Collection<String> getHumanPlayersOnline() {
        Set<String> names = new HashSet<>();
        for (UUID uuid : getPlayersOnline()) {
            names.add(getNameFromUuid(uuid, false));
        }
        return names;
    }

    /**
     * Get a full list of players on all servers.
     *
     * @return a immutable Multimap with all players found on this network
     * @since 0.2.5
     */
    public final Multimap<String, UUID> getServerToPlayers() {
        return plugin.playerDataManager().serversToPlayers();
    }

    /**
     * Get a list of players on the server with the given name.
     *
     * @param server a server name
     * @return a Set with all players found on this server
     */
    public final Set<UUID> getPlayersOnServer(@NonNull String server) {
        return ImmutableSet.copyOf(getServerToPlayers().get(server));
    }

    /**
     * Get a list of players on the specified proxy.
     *
     * @param proxyID proxy id
     * @return a Set with all UUIDs found on this proxy
     */
    public final Set<UUID> getPlayersOnProxy(@NonNull String proxyID) {
        return plugin.proxyDataManager().getPlayersOn(proxyID);
    }

    /**
     * Convenience method: Checks if the specified player is online.
     *
     * @param player a player name
     * @return if the player is online
     */
    public final boolean isPlayerOnline(@NonNull UUID player) {
        return getLastOnline(player) == 0;
    }

    /**
     * Get the {@link java.net.InetAddress} associated with this player.
     *
     * @param player the player to fetch the IP for
     * @return an {@link java.net.InetAddress} if the player is online, null otherwise
     * @since 0.2.4
     */
    public final InetAddress getPlayerIp(@NonNull UUID player) {
        return plugin.playerDataManager().getIpFor(player);
    }

    /**
     * Get the RedisBungee proxy ID this player is connected to.
     *
     * @param player the player to fetch the IP for
     * @return the proxy the player is connected to, or null if they are offline
     * @since 0.3.3
     */
    public final String getProxy(@NonNull UUID player) {
        return plugin.playerDataManager().getProxyFor(player);
    }

    /**
     * Sends a proxy command to all proxies.
     *
     * @param command the command to send and execute
     * @see #sendProxyCommand(String, String)
     * @since 0.2.5
     */
    public final void sendProxyCommand(@NonNull String command) {
        sendProxyCommand("allservers", command);
    }

    /**
     * Sends a proxy command to the proxy with the given ID. "allservers" means all proxies.
     *
     * @param proxyId a proxy ID
     * @param command the command to send and execute
     * @see #getProxyId()
     * @see #getAllProxies()
     * @since 0.2.5
     */
    public final void sendProxyCommand(@NonNull String proxyId, @NonNull String command) {
        plugin.proxyDataManager().sendCommandTo(proxyId, command);
    }

    /**
     * Sends a message to a PubSub channel which makes PubSubMessageEvent fire.
     * <p>
     * Note: Since 0.12.0 registering a channel api is no longer required
     *
     * @param channel The PubSub channel
     * @param message the message body to send
     * @since 0.3.3
     */
    public final void sendChannelMessage(@NonNull String channel, @NonNull String message) {
        plugin.proxyDataManager().sendChannelMessage(channel, message);
    }

    /**
     * Get the current BungeeCord / Velocity proxy ID for this server.
     *
     * @return the current server ID
     * @see #getAllProxies()
     * @since 0.8.0
     */
    public final String getProxyId() {
        return plugin.proxyDataManager().proxyId();
    }

    /**
     * Get all the linked proxies in this network.
     *
     * @return the list of all proxies
     * @see #getProxyId()
     * @since 0.8.0
     */
    public final List<String> getAllProxies() {
        return plugin.proxyDataManager().proxiesIds();
    }

    /**
     * Fetch a name from the specified UUID. UUIDs are cached locally and in Redis. This function falls back to Mojang
     * as a last resort, so calls <strong>may</strong> be blocking.
     * <p>
     * For the common use case of translating a list of UUIDs into names, use {@link #getHumanPlayersOnline()} instead.
     * <p>
     * If performance is a concern, use {@link #getNameFromUuid(java.util.UUID, boolean)} as this allows you to disable Mojang lookups.
     *
     * @param uuid the UUID to fetch the name for
     * @return the name for the UUID
     * @since 0.3
     */
    public final String getNameFromUuid(@NonNull UUID uuid) {
        return getNameFromUuid(uuid, true);
    }

    /**
     * Fetch a name from the specified UUID. UUIDs are cached locally and in Redis. This function can fall back to Mojang
     * as a last resort if {@code expensiveLookups} is true, so calls <strong>may</strong> be blocking.
     * <p>
     * For the common use case of translating the list of online players into names, use {@link #getHumanPlayersOnline()}.
     * <p>
     * If performance is a concern, set {@code expensiveLookups} to false as this will disable lookups via Mojang.
     *
     * @param uuid             the UUID to fetch the name for
     * @param expensiveLookups whether or not to perform potentially expensive lookups
     * @return the name for the UUID
     * @since 0.3.2
     */
    public final String getNameFromUuid(@NonNull UUID uuid, boolean expensiveLookups) {
        return plugin.getUuidTranslator().getNameFromUuid(uuid, expensiveLookups);
    }

    /**
     * Fetch a UUID from the specified name. Names are cached locally and in Redis. This function falls back to Mojang
     * as a last resort, so calls <strong>may</strong> be blocking.
     * <p>
     * If performance is a concern, see {@link #getUuidFromName(String, boolean)}, which disables the following functions:
     * <ul>
     * <li>Searching local entries case-insensitively</li>
     * <li>Searching Mojang</li>
     * </ul>
     *
     * @param name the UUID to fetch the name for
     * @return the UUID for the name
     * @since 0.3
     */
    public final UUID getUuidFromName(@NonNull String name) {
        return getUuidFromName(name, true);
    }

    /**
     * Fetch a UUID from the specified name. Names are cached locally and in Redis. This function falls back to Mojang
     * as a last resort if {@code expensiveLookups} is true, so calls <strong>may</strong> be blocking.
     * <p>
     * If performance is a concern, set {@code expensiveLookups} to false to disable searching Mojang and searching for usernames
     * case-insensitively.
     *
     * @param name             the UUID to fetch the name for
     * @param expensiveLookups whether or not to perform potentially expensive lookups
     * @return the {@link UUID} for the name
     * @since 0.3.2
     */
    public final UUID getUuidFromName(@NonNull String name, boolean expensiveLookups) {
        return plugin.getUuidTranslator().getTranslatedUuid(name, expensiveLookups);
    }

    /**
     * Kicks a player from the network
     * calls {@link #getUuidFromName(String)} to get uuid
     *
     * @param playerName player name
     * @param message   kick message that player will see on kick
     * @since 0.12.0
     */

    public void kickPlayer(String playerName, Component message) {
        kickPlayer(getUuidFromName(playerName), message);
    }

    /**
     * Kicks a player from the network
     *
     * @param playerUUID player name
     * @param message    kick message that player will see on kick
     * @since 0.12.0
     */
    public void kickPlayer(UUID playerUUID, Component message) {
        this.plugin.playerDataManager().kickPlayer(playerUUID, message);
    }

    /**
     * returns Summoner class responsible for Single Jedis {@link redis.clients.jedis.JedisPooled} with {@link JedisPool}, Cluster Jedis {@link redis.clients.jedis.JedisCluster} handling
     *
     * @return {@link Summoner}
     * @since 0.8.0
     */
    public Summoner<?> getSummoner() {
        return this.plugin.getSummoner();
    }

    /**
     * shows what mode is RedisBungee is on
     * Basically what every redis mode is used like cluster or single instance.
     *
     * @return {@link RedisBungeeMode}
     * @since 0.8.0
     */
    public RedisBungeeMode getMode() {
        return this.plugin.getRedisBungeeMode();
    }

    public static AbstractRedisBungeeAPI getAbstractRedisBungeeAPI() {
        return abstractRedisBungeeAPI;
    }
}
