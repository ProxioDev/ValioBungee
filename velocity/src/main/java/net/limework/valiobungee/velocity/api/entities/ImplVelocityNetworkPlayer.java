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
package net.limework.valiobungee.velocity.api.entities;

import java.util.UUID;
import net.limework.valiobungee.api.entity.NetworkProxy;
import net.limework.valiobungee.core.ValioBungeePlatform;
import net.limework.valiobungee.core.api.entities.AbstractNetworkPlayer;

public class ImplVelocityNetworkPlayer extends AbstractNetworkPlayer {
  public ImplVelocityNetworkPlayer(ValioBungeePlatform platform, UUID uuid, NetworkProxy proxy) {
    super(platform, uuid, proxy);
  }
}
