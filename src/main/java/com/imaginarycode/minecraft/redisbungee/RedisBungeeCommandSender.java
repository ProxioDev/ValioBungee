/**
 * This is free and unencumbered software released into the public domain.
 * <p/>
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * <p/>
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * <p/>
 * For more information, please refer to <http://unlicense.org/>
 */
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
