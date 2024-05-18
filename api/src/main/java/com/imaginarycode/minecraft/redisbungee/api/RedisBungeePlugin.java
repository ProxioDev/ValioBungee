/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api;

import com.imaginarycode.minecraft.redisbungee.AbstractRedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.api.config.LangConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
import com.imaginarycode.minecraft.redisbungee.api.events.EventsPlatform;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import com.imaginarycode.minecraft.redisbungee.api.util.uuid.UUIDTranslator;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * This Class has all internal methods needed by every redis bungee plugin, and it can be used to implement another platforms than bungeecord or another forks of RedisBungee
 * <p>
 * Reason this is interface because some proxies implementations require the user to extend class for plugins for example bungeecord.
 *
 * @author Ham1255
 * @since 0.7.0
 */
public interface RedisBungeePlugin<P> extends EventsPlatform {

    default void initialize() {

    }

    default void stop() {

    }

    void logInfo(String msg);

    void logInfo(String format, Object... object);

    void logWarn(String msg);

    void logWarn(String format, Object... object);

    void logFatal(String msg);

    void logFatal(String format, Throwable throwable);

    RedisBungeeConfiguration configuration();

    LangConfiguration langConfiguration();

    Summoner<?> getSummoner();

    RedisBungeeMode getRedisBungeeMode();

    AbstractRedisBungeeAPI getAbstractRedisBungeeApi();

    ProxyDataManager proxyDataManager();

    PlayerDataManager<P, ?, ?, ?, ?, ?, ?, ?> playerDataManager();

    UUIDTranslator getUuidTranslator();

    boolean isOnlineMode();

    P getPlayer(UUID uuid);

    P getPlayer(String name);

    UUID getPlayerUUID(String player);


    String getPlayerName(UUID player);

    boolean handlePlatformKick(UUID uuid, Component message);

    String getPlayerServerName(P player);

    boolean isPlayerOnAServer(P player);

    InetAddress getPlayerIp(P player);

    void executeAsync(Runnable runnable);

    void executeAsyncAfter(Runnable runnable, TimeUnit timeUnit, int time);


}
