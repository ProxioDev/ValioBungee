/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This language support implementation is temporarily
 * until I come up with better system but for now we will use Maps instead :/
 */
public class LangConfiguration {

    private interface RegistrableMessages {

        void register(String id, Locale locale, String miniMessage);

        void test(Locale locale);

        default void throwError(Locale locale, String where) {
            throw new IllegalStateException("Language system  in `" + where + "` found missing entries for " + locale.toString());
        }

    }

    public static class Messages implements RegistrableMessages{

        private final Map<Locale, Component> LOGGED_IN_FROM_OTHER_LOCATION;
        private final Map<Locale, Component> ALREADY_LOGGED_IN;
        private final Map<Locale, String> SERVER_CONNECTING;
        private final Map<Locale, String> SERVER_NOT_FOUND;

        private final Locale defaultLocale;

        public Messages(Locale defaultLocale) {
            LOGGED_IN_FROM_OTHER_LOCATION = new HashMap<>();
            ALREADY_LOGGED_IN = new HashMap<>();
            SERVER_CONNECTING = new HashMap<>();
            SERVER_NOT_FOUND = new HashMap<>();
            this.defaultLocale = defaultLocale;
        }

        public void register(String id, Locale locale, String miniMessage) {
            switch (id) {
                case "server-not-found" -> SERVER_NOT_FOUND.put(locale, miniMessage);
                case "server-connecting" -> SERVER_CONNECTING.put(locale, miniMessage);
                case "logged-in-other-location" -> LOGGED_IN_FROM_OTHER_LOCATION.put(locale, MiniMessage.miniMessage().deserialize(miniMessage));
                case "already-logged-in" -> ALREADY_LOGGED_IN.put(locale, MiniMessage.miniMessage().deserialize(miniMessage));
            }
        }

        public Component alreadyLoggedIn(Locale locale) {
            if (ALREADY_LOGGED_IN.containsKey(locale)) return  ALREADY_LOGGED_IN.get(locale);
            return ALREADY_LOGGED_IN.get(defaultLocale);
        }

        // there is no way to know whats client locale during login so just default to use default locale MESSAGES.
        public Component alreadyLoggedIn() {
            return this.alreadyLoggedIn(this.defaultLocale);
        }

        public Component loggedInFromOtherLocation(Locale locale) {
            if (LOGGED_IN_FROM_OTHER_LOCATION.containsKey(locale)) return  LOGGED_IN_FROM_OTHER_LOCATION.get(locale);
            return LOGGED_IN_FROM_OTHER_LOCATION.get(defaultLocale);
        }

        // there is no way to know what's client locale during login so just default to use default locale MESSAGES.
        public Component loggedInFromOtherLocation() {
            return this.loggedInFromOtherLocation(this.defaultLocale);
        }

        public Component serverConnecting(Locale locale, String server) {
            String miniMessage;
            if (SERVER_CONNECTING.containsKey(locale)) {
                miniMessage = SERVER_CONNECTING.get(locale);
            } else {
                miniMessage = SERVER_CONNECTING.get(defaultLocale);
            }
            return MiniMessage.miniMessage().deserialize(miniMessage, Placeholder.parsed("server", server));
        }

        public Component serverConnecting(String server) {
            return this.serverConnecting(this.defaultLocale, server);
        }

        public Component serverNotFound(Locale locale, String server) {
            String miniMessage;
            if (SERVER_NOT_FOUND.containsKey(locale)) {
                miniMessage = SERVER_NOT_FOUND.get(locale);
            } else {
                miniMessage = SERVER_NOT_FOUND.get(defaultLocale);
            }
            return MiniMessage.miniMessage().deserialize(miniMessage, Placeholder.parsed("server", server));
        }

        public Component serverNotFound(String server) {
            return this.serverNotFound(this.defaultLocale, server);
        }


        // tests locale if set CORRECTLY or just throw if not
        public void test(Locale locale) {
            if (!(LOGGED_IN_FROM_OTHER_LOCATION.containsKey(locale) && ALREADY_LOGGED_IN.containsKey(locale) && SERVER_CONNECTING.containsKey(locale) && SERVER_NOT_FOUND.containsKey(locale))) {
                throwError(locale, "messages");
            }
        }

    }

    public static class CommandMessages implements RegistrableMessages {

        private final Locale defaultLocale;

        // Common
        private final Map<Locale, Component> COMMON_PLAYER_NOT_FOUND;
        private final Map<Locale, Component> COMMON_PLAYER_NOT_SPECIFIED;
        private final Map<Locale, Component> COMMON_COMMAND_NOT_SPECIFIED;

        public CommandMessages(Locale defaultLocale) {
            this.defaultLocale = defaultLocale;
            COMMON_PLAYER_NOT_FOUND = new HashMap<>();
            COMMON_COMMAND_NOT_SPECIFIED = new HashMap<>();
            COMMON_PLAYER_NOT_SPECIFIED = new HashMap<>();
        }

        // probably split using :
        @Override
        public void register(String id, Locale locale, String miniMessage) {
            String[] splitId = id.split(":");
            //System.out.println(Arrays.toString(splitId) + " " + locale + miniMessage);
            switch (splitId[0]) {
                case "commands-common" -> {
                    switch (splitId[1]) {
                        case "player-not-found" -> COMMON_PLAYER_NOT_FOUND.put(locale, MiniMessage.miniMessage().deserialize(miniMessage));
                        case "player-not-specified" -> COMMON_PLAYER_NOT_SPECIFIED.put(locale, MiniMessage.miniMessage().deserialize(miniMessage));
                        case "command-not-specified" -> COMMON_COMMAND_NOT_SPECIFIED.put(locale, MiniMessage.miniMessage().deserialize(miniMessage));
                    }
                }
                case "commands" -> {
                    switch (splitId[1]) {

                    }
                }
            }
        }

        public Component playerNotFound(Locale locale) {
            if (COMMON_PLAYER_NOT_FOUND.containsKey(locale)) return  COMMON_PLAYER_NOT_FOUND.get(locale);
            return COMMON_PLAYER_NOT_FOUND.get(defaultLocale);
        }
        public Component playerNotFound() {
            return playerNotFound(this.defaultLocale);
        }
        public Component commandNotSpecified(Locale locale) {
            if (COMMON_COMMAND_NOT_SPECIFIED.containsKey(locale)) return  COMMON_COMMAND_NOT_SPECIFIED.get(locale);
            return COMMON_COMMAND_NOT_SPECIFIED.get(defaultLocale);
        }
        public Component commandNotSpecified() {
            return commandNotSpecified(this.defaultLocale);
        }
        public Component playerNotSpecified(Locale locale) {
            if (COMMON_PLAYER_NOT_SPECIFIED.containsKey(locale)) return  COMMON_PLAYER_NOT_SPECIFIED.get(locale);
            return COMMON_PLAYER_NOT_SPECIFIED.get(defaultLocale);
        }
        public Component playerNotSpecified() {
            return playerNotSpecified(this.defaultLocale);
        }


        @Override
        public void test(Locale locale) {
            if (!(this.COMMON_PLAYER_NOT_FOUND.containsKey(locale) && this.COMMON_PLAYER_NOT_SPECIFIED.containsKey(locale) && this.COMMON_COMMAND_NOT_SPECIFIED.containsKey(locale))) {
                throwError(locale, "commands messages");
            }
        }
    }

    private final Component redisBungeePrefix;

    private final Locale defaultLanguage;

    private final boolean useClientLanguage;

    private final Messages messages;

    private final CommandMessages commandMessages;


    public LangConfiguration(Component redisBungeePrefix, Locale defaultLanguage, boolean useClientLanguage, Messages messages, CommandMessages commandMessages) {
        this.redisBungeePrefix = redisBungeePrefix;
        this.defaultLanguage = defaultLanguage;
        this.useClientLanguage = useClientLanguage;
        this.messages = messages;
        this.commandMessages = commandMessages;
    }

    public Component redisBungeePrefix() {
        return redisBungeePrefix;
    }

    public Locale defaultLanguage() {
        return defaultLanguage;
    }

    public boolean useClientLanguage() {
        return useClientLanguage;
    }

    public Messages messages() {
        return messages;
    }

    public CommandMessages commandMessages() {
        return commandMessages;
    }
}
