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

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.Collection;
import java.util.Collections;

public class RedisBungeeCommandSender implements CommandSender {
    private static final RedisBungeeCommandSender singleton;

    static {
        singleton = new RedisBungeeCommandSender();
    }

    public static RedisBungeeCommandSender getSingleton() {
        return singleton;
    }

    @Override
    public String getName() {
        return "RedisBungee";
    }

    @Override
    public void sendMessage(String s) {

    }

    @Override
    public void sendMessages(String... strings) {

    }

    @Override
    public void sendMessage(BaseComponent... baseComponents) {

    }

    @Override
    public void sendMessage(BaseComponent baseComponent) {

    }

    @Override
    public Collection<String> getGroups() {
        return null;
    }

    @Override
    public void addGroups(String... strings) {

    }

    @Override
    public void removeGroups(String... strings) {

    }

    @Override
    public boolean hasPermission(String s) {
        return true;
    }

    @Override
    public void setPermission(String s, boolean b) {

    }

    @Override
    public Collection<String> getPermissions() {
        return Collections.emptySet();
    }
}
