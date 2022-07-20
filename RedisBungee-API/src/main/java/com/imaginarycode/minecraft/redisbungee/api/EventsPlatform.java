package com.imaginarycode.minecraft.redisbungee.api;

import java.util.UUID;

/**
 * Since each platform have their own events' implementation for example Bungeecord events extends Event while velocity don't
 *
 * @author Ham1255
 * @since 0.7.0
 *
 */
public interface EventsPlatform {

    Object createPlayerChangedNetworkEvent(UUID uuid, String previousServer, String server);

    Object createPlayerJoinedNetworkEvent(UUID uuid);

    Object createPlayerLeftNetworkEvent(UUID uuid);

    Object createPubSubEvent(String channel, String message);


}
