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
package net.limework.valiobungee.api;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.limework.valiobungee.api.entity.NetworkPlayer;
import net.limework.valiobungee.api.entity.NetworkProxy;

/**
 * The api interface
 *
 * @author Ham1255
 * @since 1.0.0
 */
public interface ValioBungeeAPI {

  /**
   * @param uuid of player
   * @return {@link Optional} if empty player is offline and if present player on the network
   */
  Optional<NetworkPlayer> getNetworkPlayer(UUID uuid);

  /**
   * @param id the proxy id
   * @return {@link Optional} if empty the proxy doesn't exist if present it's an online proxy
   */
  Optional<NetworkProxy> getNetworkProxy(String id);

  /**
   * @return the local proxy
   */
  NetworkProxy getLocalProxy();

  /**
   * @return Returns the network proxies that is currently online including the local
   */
  Set<NetworkProxy> getNetworkProxies();

  /**
   * @return the local players empty if no players
   */
  Set<NetworkPlayer> getLocalProxyPlayers();

  /**
   * @return the network players empty if no players
   */
  Set<NetworkPlayer> getNetworkPlayers();

  /**
   * @return Version based on the build
   */
  String getVersion();

  /**
   * @return GIT COMMIT based on the build
   */
  String getGitCommit();
}
