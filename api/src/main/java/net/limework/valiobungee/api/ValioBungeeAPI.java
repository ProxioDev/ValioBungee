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

public interface ValioBungeeAPI {

  Optional<NetworkPlayer> getNetworkPlayer(UUID uuid);

  Optional<NetworkProxy> getNetworkProxy(String id);

  NetworkProxy getLocalProxy();

  Set<NetworkProxy> getNetworkProxies();

  Set<NetworkPlayer> getLocalProxyPlayers();

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
