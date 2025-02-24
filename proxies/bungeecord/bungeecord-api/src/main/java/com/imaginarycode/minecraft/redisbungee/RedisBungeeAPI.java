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

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * This platform class exposes some internal RedisBungee functions. You obtain an instance of this object by invoking {@link RedisBungeeAPI#getRedisBungeeApi()}
 * or somehow you got the Plugin instance by you can call the api using {@link RedisBungeePlugin#getAbstractRedisBungeeApi()}.
 *
 * @author tuxed
 * @since 0.2.3 | updated 0.8.0
 */
public class RedisBungeeAPI extends AbstractRedisBungeeAPI {

    private static RedisBungeeAPI redisBungeeApi;

    private static final BungeeComponentSerializer BUNGEE_COMPONENT_SERIALIZER = BungeeComponentSerializer.get();

    public RedisBungeeAPI(RedisBungeePlugin<?> plugin) {
        super(plugin);
        if (redisBungeeApi == null) {
            redisBungeeApi = this;
        }
    }

    /**
     * Get the server where the specified player is playing. This function also deals with the case of local players
     * as well, and will return local information on them.
     *
     * @param player a player uuid
     * @return {@link ServerInfo} Can be null if proxy can't find it.
     * @see #getServerNameFor(UUID)
     */
    @Nullable
    public final ServerInfo getServerFor(@NonNull UUID player) {
        String serverName = this.getServerNameFor(player);
        if (serverName == null) return null;
        return ((Plugin) this.plugin).getProxy().getServerInfo(serverName);
    }

    /**
     * Kicks a player from the network
     * calls {@link #getUuidFromName(String)} to get uuid
     *
     * @param playerName player name
     * @param message   kick message that player will see on kick
     * @since 0.13.0
     */
    public void kickPlayer(String playerName, BaseComponent[] message) {
        kickPlayer(getUuidFromName(playerName), message);
    }

    /**
     * Kicks a player from the network
     *
     * @param player player uuid
     * @param message    kick message that player will see on kick
     * @since 0.13.0
     */
    public void kickPlayer(UUID player, BaseComponent[] message) {
        kickPlayer(player, BUNGEE_COMPONENT_SERIALIZER.deserialize(message));
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
     * @param player player uuid
     * @param message    kick message that player will see on kick
     * @since 0.12.0
     */
    public void kickPlayer(UUID player, Component message) {
        ((ApiPlatformSupport) this.plugin).kickPlayer(player, message);
    }

    /**
     * Get the current BungeeCord / Velocity proxy ID for this server.
     *
     * @return the current server ID
     * @see #getAllServers()
     * @since 0.2.5
     * @deprecated to avoid confusion between A server and A proxy see #getProxyId()
     */
    @Deprecated(forRemoval = true)
    public final String getServerId() {
        return getProxyId();
    }

    /**
     * Get all the linked proxies in this network.
     *
     * @return the list of all proxies
     * @see #getServerId()
     * @since 0.2.5
     * @deprecated to avoid confusion between A server and A proxy see see {@link #getAllProxies()}
     */
    @Deprecated(forRemoval = true)
    public final List<String> getAllServers() {
        return getAllProxies();
    }

    /**
     * Register (a) PubSub channel(s), so that you may handle PubSubMessageEvent for it.
     *
     * @param channels the channels to register
     * @since 0.3
     * @deprecated No longer required
     */
    @Deprecated(forRemoval = true)
    public final void registerPubSubChannels(String... channels) {
    }

    /**
     * Unregister (a) PubSub channel(s).
     *
     * @param channels the channels to unregister
     * @since 0.3
     * @deprecated No longer required
     */
    @Deprecated(forRemoval = true)
    public final void unregisterPubSubChannels(String... channels) {
    }

    /**
     * Api instance
     *
     * @return the API instance.
     * @since 0.6.5
     */
    public static RedisBungeeAPI getRedisBungeeApi() {
        return redisBungeeApi;
    }
}
