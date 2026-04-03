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
package net.limework.valiobungee.core.api.entities;

import java.util.Objects;
import java.util.UUID;
import net.limework.valiobungee.api.entity.NetworkPlayer;
import net.limework.valiobungee.api.entity.NetworkProxy;
import net.limework.valiobungee.api.entity.UUIDPlayer;
import net.limework.valiobungee.core.ValioBungeePlatform;

public abstract class AbstractUUIDNetworkPlayer implements NetworkPlayer, UUIDPlayer {

  private final ValioBungeePlatform platform;
  private final UUID uuid;
  private final NetworkProxy proxy;

  public AbstractUUIDNetworkPlayer(ValioBungeePlatform platform, UUID uuid, NetworkProxy proxy) {
    this.platform = platform;
    this.uuid = uuid;
    this.proxy = proxy;
  }

  @Override
  public UUID getUniqueId() {
    return this.uuid;
  }

  @Override
  public NetworkProxy getProxy() {
    return this.proxy;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AbstractUUIDNetworkPlayer that)) return false;
    return Objects.equals(uuid, that.uuid) && Objects.equals(proxy, that.proxy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, proxy);
  }
}
