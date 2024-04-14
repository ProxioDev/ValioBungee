package com.imaginarycode.minecraft.redisbungee;

import co.aikar.commands.BungeeCommandIssuer;
import co.aikar.commands.CommandIssuer;
import com.imaginarycode.minecraft.redisbungee.commands.utils.CommandPlatformHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;

public class BungeeCommandPlatformHelper extends CommandPlatformHelper {

    @Override
    public void sendMessage(CommandIssuer issuer, Component component) {
        BungeeCommandIssuer bIssuer = (BungeeCommandIssuer) issuer;
        bIssuer.getIssuer().sendMessage(BungeeComponentSerializer.get().serialize(component));
    }

}
