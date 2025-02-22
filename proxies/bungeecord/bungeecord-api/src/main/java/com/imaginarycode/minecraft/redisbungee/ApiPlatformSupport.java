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

import net.kyori.adventure.text.Component;

import java.util.UUID;

// this class used to redirect calls to keep the implementation and api separate
public interface ApiPlatformSupport {

    void kickPlayer(UUID player, Component message);

}
