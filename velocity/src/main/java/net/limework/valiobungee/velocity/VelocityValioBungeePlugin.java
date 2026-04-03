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

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.limework.valiobungee.api.entity.NetworkPlayer;
import net.limework.valiobungee.api.entity.NetworkProxy;
import net.limework.valiobungee.core.ConstantVariables;
import net.limework.valiobungee.core.ProxyNetworkManager;
import net.limework.valiobungee.core.ValioBungeePlatform;
import net.limework.valiobungee.core.util.logging.LogProviderFactory;
import net.limework.valiobungee.velocity.api.TestProxyNetworkManager;
import net.limework.valiobungee.velocity.api.entities.ImplVelocityNetworkPlayer;
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

  @Inject
  public VelocityValioBungeePlugin(
      ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataFolder = dataDirectory;
    // init logging
    LogProviderFactory.register(logger);
    this.proxyNetworkManager = new TestProxyNetworkManager(this);
  }

  @Subscribe(priority = Short.MAX_VALUE) // this really important so MAKE IT MAX
  public void onProxyInitializeEvent(ProxyInitializeEvent event) {
    logger.info(
        "initializing ValioBungee for {} platform, version {}",
        platformProxyVendor(),
        ConstantVariables.VERSION);
  }

  @Subscribe(priority = Short.MIN_VALUE) // this really import so Make it AT LOWEST
  public void onProxyShutdownEvent(ProxyShutdownEvent event) {}

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
  public NetworkProxy proxyPlatformCreator(String id) {
    return new ImplVelocityNetworkProxy(this, id);
  }

  private final String id = "test-ido-" + ThreadLocalRandom.current().nextInt(10);

  @Override
  public String proxyId() {
    return id;
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
    return this.proxyNetworkManager.getNetworkProxy(id);
  }

  @Override
  public NetworkProxy getLocalProxy() {
    return new ImplVelocityNetworkProxy(this, proxyId());
  }

  @Override
  public Set<NetworkProxy> getNetworkProxies() {
    return proxyNetworkManager.getNetworkProxies();
  }

  @Override
  public Set<NetworkPlayer> getLocalProxyPlayers() {
    NetworkProxy proxy = getLocalProxy();
    return this.server.getAllPlayers().stream()
        .map(p -> new ImplVelocityNetworkPlayer(this, p.getUniqueId(), getLocalProxy(), p))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<NetworkPlayer> getNetworkPlayers() {
    logger.warn("not implemented api call returned as Optional empty");
    return Set.of();
  }
}
