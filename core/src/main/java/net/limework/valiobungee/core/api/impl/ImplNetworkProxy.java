/*
* Copyright (c) 2026-present ValioBungee contributors
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU GENERAL PUBLIC LICENSE Version 3
* which accompanies this distribution, and is available at
* https://www.gnu.org/licenses/gpl-3.0.txt
*/
package net.limework.valiobungee.core.api.impl;

import net.limework.valiobungee.api.NetworkProxy;
import net.limework.valiobungee.core.ValioBungeePlatform;

public class ImplNetworkProxy implements NetworkProxy {

  private final ValioBungeePlatform platform;

  private final String proxyId;

  public ImplNetworkProxy(ValioBungeePlatform platform, String proxyId) {
    this.platform = platform;
    this.proxyId = proxyId;
  }

  @Override
  public String proxyId() {
    return this.proxyId;
  }

  @Override
  public int onlinePlayers() {
    return this.platform.proxyManager().onlinePlayersCount(this.proxyId);
  }

  @Override
  public boolean isMe() {
    return this.platform.proxyId().equals(proxyId);
  }
}
