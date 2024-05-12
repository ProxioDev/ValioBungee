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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

/**
 * This platform class exposes some internal RedisBungee functions. You obtain an instance of this object by invoking {@link RedisBungeeAPI#getRedisBungeeApi()}
 * or somehow you got the Plugin instance by you can call the api using {@link RedisBungeePlugin#getAbstractRedisBungeeApi()}.
 *
 * @author tuxed
 * @since 0.2.3
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
        return ((ServerObjectFetcher) this.plugin).getProxy().getServer(serverName).map((RegisteredServer::getServerInfo)).orElse(null);
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
