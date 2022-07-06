package com.imaginarycode.minecraft.redisbungee;


import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;

import java.util.Collection;
import java.util.Collections;

public class RedisBungeeCommandSource implements CommandSource {
    private static final RedisBungeeCommandSource singleton;

    static {
        singleton = new RedisBungeeCommandSource();
    }

    public static RedisBungeeCommandSource getSingleton() {
        return singleton;
    }


    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public Tristate getPermissionValue(String s) {
        return null;
    }

    @Override
    public PermissionChecker getPermissionChecker() {
        return PermissionChecker.always(TriState.TRUE);
    }
}
