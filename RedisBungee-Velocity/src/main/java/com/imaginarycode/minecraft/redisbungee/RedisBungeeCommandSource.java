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


import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.permission.PermissionChecker;

public class RedisBungeeCommandSource implements CommandSource {
    private static final RedisBungeeCommandSource singleton;
    private final PermissionChecker permissionChecker = PermissionChecker.always(net.kyori.adventure.util.TriState.TRUE);

    static {
        singleton = new RedisBungeeCommandSource();
    }

    public static RedisBungeeCommandSource getSingleton() {
        return singleton;
    }


    @Override
    public boolean hasPermission(String permission) {
        return this.permissionChecker.test(permission);
    }

    @Override
    public Tristate getPermissionValue(String s) {
        return Tristate.TRUE;
    }

    @Override
    public PermissionChecker getPermissionChecker() {
        return this.permissionChecker;
    }
}
