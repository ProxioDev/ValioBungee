package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
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

    RedisBungeeAPI(RedisBungeePlugin<?> plugin) {
        super(plugin);
        redisBungeeApi = this;
    }

    /**
     * Get the server where the specified player is playing. This function also deals with the case of local players
     * as well, and will return local information on them.
     *
     * @param player a player uuid
     * @return {@link ServerInfo}
     * @deprecated This does return null even if player is on a server if the server is not on the proxy
     * @see #getServerNameFor(UUID) 
     */
    @Deprecated
    public final ServerInfo getServerFor(@NonNull UUID player) {
        return ((Plugin) this.plugin).getProxy().getServerInfo(this.getServerNameFor(player));
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
