/*
 * Copyright (c) 2026 ValioBungee contributors
 *
 * This file is part of ValioBungee.
 *
 * ValioBungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ValioBungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ValioBungee. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package net.limework.valiobungee.velocity;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.limework.valiobungee.core.ConstantVariables;
import net.limework.valiobungee.core.ValioBungeePlatform;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "valiobungee",
        name = "valiobungee",
        version = ConstantVariables.VERSION,
        url = "https://github.com/ProxioDev/ValioBungee",
        authors = {"limework", "ProxioDev"})
public class VelocityValioBungeePlugin implements ValioBungeePlatform {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;

    public VelocityValioBungeePlugin( ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataFolder = dataDirectory;
    }



}
