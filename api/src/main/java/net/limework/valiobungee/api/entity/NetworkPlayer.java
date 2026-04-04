/*
 * Copyright (c) 2026 ValioBungee contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.limework.valiobungee.api.entity;

import java.util.UUID;

/**
 * Network player.
 *
 * @author Ham1255
 * @since 1.0.0
 */
public interface NetworkPlayer {
  /**
   * @return proxy player on
   */
  NetworkProxy getProxy();

  /**
   * @return true when player is on the same proxy
   */
  boolean isLocal();

  /**
   * This used when we already had the object but player went offline since getting a player as
   * {@link java.util.Optional} we check if its present if not means offline see {@link
   * net.limework.valiobungee.api.ValioBungeeAPI#getNetworkPlayer(UUID)}
   *
   * @return true when a player is online on the network
   */
  boolean isOnline();

  /**
   * @return player unique id
   */
  UUID getUniqueId();
}
