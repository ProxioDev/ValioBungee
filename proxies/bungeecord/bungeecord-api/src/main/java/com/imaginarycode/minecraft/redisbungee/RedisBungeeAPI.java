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

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisPooledSummoner;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import redis.clients.jedis.JedisPool;

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

    // LEGACY API FOR BACKWARD COMPATIBILITY

    /**
     * Warning: this is Legacy API to maintain backward compatibility with RedisBungee Pre fork 0.5
     * <p>
     * Get all the linked proxies in this network.
     *
     * @return the list of all proxies
     * @see #getServerId()
     * @since 0.2.5
     * @deprecated to avoid confusion between A server and A proxy see see {@link #getAllProxies()}
     */
    @Deprecated
    public final List<String> getAllServers() {
        return getAllProxies();
    }

    /**
     * Warning: this is Legacy API to maintain backward compatibility with RedisBungee Pre fork 0.5
     * <p>
     * Get the current BungeeCord / Velocity proxy ID for this server.
     *
     * @return the current server ID
     * @see #getAllServers()
     * @since 0.2.5
     * @deprecated to avoid confusion between A server and A proxy see #getProxyId()
     */
    @Deprecated
    public final String getServerId() {
        return getProxyId();
    }

    /**
     * Warning: this is Legacy API to maintain backward compatibility with RedisBungee Pre fork 0.5
     * <p>
     * Register (a) PubSub channel(s), so that you may handle PubSubMessageEvent for it.
     *
     * @param channels the channels to register
     * @since 0.3
     * @deprecated No longer required
     */
    @Deprecated
    public final void registerPubSubChannels(String... channels) {
    }

    /**
     * Warning: this is Legacy API to maintain backward compatibility with RedisBungee Pre fork 0.5
     * <p>
     *  Unregister (a) PubSub channel(s).
     *
     * @param channels the channels to unregister
     * @since 0.3
     * @deprecated No longer required
     */
    @Deprecated
    public final void unregisterPubSubChannels(String... channels) {
    }


    /**
     * Warning: this is Legacy API to maintain backward compatibility with RedisBungee Pre fork 0.5
     * <p>
     * This gets Redis Bungee {@link JedisPool}
     *
     * @return {@link JedisPool}
     * @throws IllegalStateException if the {@link #getMode()} is not equal to {@link RedisBungeeMode#SINGLE}
     * @throws IllegalStateException if JedisPool compatibility mode is disabled in the config
     * @since 0.6.5
     */
    public JedisPool getJedisPool() {
        if (getMode() == RedisBungeeMode.SINGLE) {
            JedisPool jedisPool = ((JedisPooledSummoner) this.plugin.getSummoner()).getCompatibilityJedisPool();
            if (jedisPool == null) {
                throw new IllegalStateException("JedisPool compatibility mode is disabled, Please enable it in the RedisBungee config.yml");
            }
            return jedisPool;
        } else {
            throw new IllegalStateException("Mode is not " + RedisBungeeMode.SINGLE);
        }
    }
    //

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
