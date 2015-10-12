package com.imaginarycode.minecraft.redisbungee;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.Collection;
import java.util.Collections;

/**
 * This class is the CommandSender that RedisBungee uses to dispatch commands to BungeeCord.
 * <p>
 * It inherits all permissions of the console command sender. Sending messages and modifying permissions are no-ops.
 *
 * @author tuxed
 * @since 0.2.3
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisBungeeCommandSender implements CommandSender {
    static final RedisBungeeCommandSender instance = new RedisBungeeCommandSender();

    @Override
    public String getName() {
        return "RedisBungee";
    }

    @Override
    public void sendMessage(String s) {
        // no-op
    }

    @Override
    public void sendMessages(String... strings) {
        // no-op
    }

    @Override
    public void sendMessage(BaseComponent... baseComponents) {
        // no-op
    }

    @Override
    public void sendMessage(BaseComponent baseComponent) {
        // no-op
    }

    @Override
    public Collection<String> getGroups() {
        return Collections.emptySet();
    }

    @Override
    public void addGroups(String... strings) {
        // no-op
    }

    @Override
    public void removeGroups(String... strings) {
        // no-op
    }

    @Override
    public boolean hasPermission(String s) {
        return true;
    }

    @Override
    public void setPermission(String s, boolean b) {
        // no-op
    }

    @Override
    public Collection<String> getPermissions() {
        return Collections.emptySet();
    }
}
