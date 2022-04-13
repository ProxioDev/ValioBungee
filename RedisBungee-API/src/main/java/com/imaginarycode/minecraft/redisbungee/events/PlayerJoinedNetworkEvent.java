package com.imaginarycode.minecraft.redisbungee.events;

import java.util.UUID;

/**
 * This event is sent when a player joins the network. RedisBungee sends the event only when
 * the proxy the player has been connected to is different than the local proxy.
 * <p>
 * This event corresponds to net.md_5.bungee.api.event.PostLoginEvent on bungee, and is fired
 * asynchronously.
 *
 * @since 0.3.4
 */
public class PlayerJoinedNetworkEvent {
    private final UUID uuid;

    public PlayerJoinedNetworkEvent(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
