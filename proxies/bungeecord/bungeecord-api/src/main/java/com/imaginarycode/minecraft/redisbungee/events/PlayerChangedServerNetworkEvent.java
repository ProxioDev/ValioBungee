/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.events;

import com.imaginarycode.minecraft.redisbungee.api.events.IPlayerChangedServerNetworkEvent;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * This event is sent when a player connects to a new server. RedisBungee sends the event only when
 * the proxy the player has been connected to is different than the local proxy.
 * <p>
 * This event corresponds to {@link net.md_5.bungee.api.event.ServerConnectedEvent}, and is fired
 * asynchronously.
 *
 * @since 0.3.4
 */
public class PlayerChangedServerNetworkEvent extends Event implements IPlayerChangedServerNetworkEvent {
    private final UUID uuid;
    private final String previousServer;
    private final String server;

    public PlayerChangedServerNetworkEvent(UUID uuid, String previousServer, String server) {
        this.uuid = uuid;
        this.previousServer = previousServer;
        this.server = server;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public String getPreviousServer() {
        return previousServer;
    }
}
