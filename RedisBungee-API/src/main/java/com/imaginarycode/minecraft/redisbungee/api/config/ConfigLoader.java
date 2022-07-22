package com.imaginarycode.minecraft.redisbungee.api.config;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public interface ConfigLoader {
    void loadConfig() throws IOException;

    default Path createConfigFile(Path dataFolder) throws IOException {
        if (Files.notExists(dataFolder)) {
            Files.createDirectory(dataFolder);
        }
        Path file = dataFolder.resolve("config.yml");
        if (Files.notExists(file)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.createFile(file);
                assert in != null;
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return file;
    }

    default void handleOldConfig(Path dataFolder) throws IOException {
        Path oldConfigFolder = dataFolder.resolve("old_config");
        if (Files.notExists(oldConfigFolder)) {
            Files.createDirectory(oldConfigFolder);
        }
        Path oldConfigPath = dataFolder.resolve("config.yml");
        Files.move(oldConfigPath, oldConfigFolder.resolve(UUID.randomUUID() + "_config.yml"));
        createConfigFile(dataFolder);

    }


}
