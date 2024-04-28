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

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.VelocityCommandIssuer;
import com.imaginarycode.minecraft.redisbungee.commands.utils.CommandPlatformHelper;
import net.kyori.adventure.text.Component;

public class VelocityCommandPlatformHelper extends CommandPlatformHelper {

   @Override
    public void sendMessage(CommandIssuer issuer, Component component) {
        VelocityCommandIssuer vIssuer = (VelocityCommandIssuer) issuer;
        vIssuer.getIssuer().sendMessage(component);
    }

}
