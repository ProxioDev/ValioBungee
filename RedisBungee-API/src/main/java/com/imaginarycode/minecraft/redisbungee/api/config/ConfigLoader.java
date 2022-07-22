package com.imaginarycode.minecraft.redisbungee.api.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface ConfigLoader {
    void loadConfig() throws IOException;

    Path createConfigFile() throws IOException;

    void handleOldConfig(Path path) throws IOException;

}
