package com.imaginarycode.minecraft.redisbungee.api;

/**
 * Since each platform have their own events' implementation for example Bungeecord events extends Event while velocity don't
 *
 * @author Ham1255
 * @since 0.7.0
 *
 */
public interface EventsPlatform {

    Class<?> getPubSubEventClass();

    Class<?> getNetworkJoinEventClass();

    Class<?> getServerChangeEventClass();

    Class<?> getNetworkQuitEventClass();

}
