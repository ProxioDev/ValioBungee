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
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.limework.valiobungee.api.entity.NetworkPlayer;
import net.limework.valiobungee.api.entity.NetworkProxy;
import net.limework.valiobungee.core.ConstantVariables;
import net.limework.valiobungee.core.ProxyNetworkManager;
import net.limework.valiobungee.core.ValioBungeePlatform;
import net.limework.valiobungee.core.util.logging.LogProviderFactory;
import net.limework.valiobungee.velocity.api.entities.ImplVelocityNetworkProxy;
import org.slf4j.Logger;

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
  private final ProxyNetworkManager proxyNetworkManager;

  public VelocityValioBungeePlugin(
      ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataFolder = dataDirectory;
    // init logging
    LogProviderFactory.register(logger);
    this.proxyNetworkManager = null;
  }

  @Override
  public int localOnlinePlayers() {
    return this.server.getPlayerCount();
  }

  @Override
  public String platformProxyVendor() {
    return "velocity";
  }

  @Override
  public ProxyNetworkManager proxyNetworkManager() {
    return this.proxyNetworkManager;
  }

  @Override
  public String proxyId() {
    return "test-ido";
  }

  @Override
  public String networkId() {
    return "development";
  }

  @Override
  public Optional<NetworkPlayer> getNetworkPlayer(UUID uuid) {
    logger.warn("not implemented api call returned as Optional empty");
    return Optional.empty();
  }

  @Override
  public Optional<NetworkProxy> getNetworkProxy(String id) {
    if (this.proxyId().equals(id)) return Optional.of(getLocalProxy());
    logger.warn("not implemented api call returned as Optional empty");
    return Optional.empty();
  }

  @Override
  public NetworkProxy getLocalProxy() {
    return new ImplVelocityNetworkProxy(this, proxyId());
  }

  @Override
  public Set<NetworkProxy> getNetworkProxies() {
    logger.warn("not implemented api call returned as Optional empty");
    return Set.of();
  }

  @Override
  public Set<NetworkPlayer> getLocalProxyPlayers() {
    logger.warn("not implemented api call returned as Optional empty");
    return Set.of();
  }

  @Override
  public Set<NetworkPlayer> getNetworkPlayers() {
    logger.warn("not implemented api call returned as Optional empty");
    return Set.of();
  }
}
