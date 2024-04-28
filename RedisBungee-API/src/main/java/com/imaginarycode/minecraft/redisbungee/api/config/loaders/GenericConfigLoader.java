/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.config.loaders;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;


public interface GenericConfigLoader {

    // CHANGES on every reboot
    String RANDOM_OLD = "backup-" + Instant.now().getEpochSecond();

    default Path createConfigFile(Path dataFolder, String configFile, @Nullable String defaultResourceID) throws IOException {
        if (Files.notExists(dataFolder)) {
            Files.createDirectory(dataFolder);
        }
        Path file = dataFolder.resolve(configFile);
        if (Files.notExists(file) && defaultResourceID != null) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(defaultResourceID)) {
                Files.createFile(file);
                assert in != null;
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return file;
    }

    default void handleOldConfig(Path dataFolder, String configFile, @Nullable String defaultResourceID) throws IOException {
        Path oldConfigFolder = dataFolder.resolve("old_config");
        if (Files.notExists(oldConfigFolder)) {
            Files.createDirectory(oldConfigFolder);
        }
        Path randomStoreConfigDirectory = oldConfigFolder.resolve(RANDOM_OLD);
        if (Files.notExists(randomStoreConfigDirectory)) {
            Files.createDirectory(randomStoreConfigDirectory);
        }
        Path oldConfigPath = dataFolder.resolve(configFile);

        Files.move(oldConfigPath, randomStoreConfigDirectory.resolve(configFile));
        createConfigFile(dataFolder, configFile, defaultResourceID);
    }

}
