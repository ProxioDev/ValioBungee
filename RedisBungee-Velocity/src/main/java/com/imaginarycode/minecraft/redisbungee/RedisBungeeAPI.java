package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

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
    public final ServerInfo getServerFor(@NonNull UUID player) {
        return ((RedisBungeeVelocityPlugin) this.plugin).getProxy().getServer(this.getServerNameFor(player)).map((RegisteredServer::getServerInfo)).orElse(null);
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
