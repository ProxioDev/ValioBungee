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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LangConfiguration {


    public static class Messages {

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
                throw new IllegalStateException("Language system  in `messages` found missing entries for " + locale.toString());
            }
        }

    }


    private final Component redisBungeePrefix;

    private final Locale defaultLanguage;

    private final boolean useClientLanguage;

    private final Messages messages;


    public LangConfiguration(Component redisBungeePrefix, Locale defaultLanguage, boolean useClientLanguage, Messages messages) {
        this.redisBungeePrefix = redisBungeePrefix;
        this.defaultLanguage = defaultLanguage;
        this.useClientLanguage = useClientLanguage;
        this.messages = messages;
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


}
