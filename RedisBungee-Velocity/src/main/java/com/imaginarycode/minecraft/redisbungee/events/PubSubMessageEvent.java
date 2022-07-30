package com.imaginarycode.minecraft.redisbungee.events;


import com.imaginarycode.minecraft.redisbungee.api.events.IPubSubMessageEvent;

/**
 * This event is posted when a PubSub message is received.
 * <p>
 * <strong>Warning</strong>: This event is fired in a separate thread!
 *
 * @since 0.2.6
 */

public class PubSubMessageEvent implements IPubSubMessageEvent {
    private final String channel;
    private final String message;

    public PubSubMessageEvent(String channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
