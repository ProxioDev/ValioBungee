/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.events;

import java.util.UUID;

/**
 * Since each platform have their own events' implementation for example Bungeecord events extends Event while velocity don't
 *
 * @author Ham1255
 * @since 0.7.0
 *
 */
public interface EventsPlatform {

    IPlayerChangedServerNetworkEvent createPlayerChangedServerNetworkEvent(UUID uuid, String previousServer, String server);

    IPlayerJoinedNetworkEvent createPlayerJoinedNetworkEvent(UUID uuid);

    IPlayerLeftNetworkEvent createPlayerLeftNetworkEvent(UUID uuid);

    IPubSubMessageEvent createPubSubEvent(String channel, String message);

    void fireEvent(Object event);

}
