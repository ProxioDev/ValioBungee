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

import java.util.Set;

/**
 * Proxy is an object for online proxy in a network
 *
 * @author Ham1255
 * @since 1.0.0
 */
public interface NetworkProxy {
  /**
   * @return return the proxy id of this proxy
   */
  String proxyId();

  /**
   * @return online players number in this proxy
   */
  int onlinePlayerCount();

  /**
   * @return Returns set of players in this proxy
   */
  Set<NetworkPlayer> getProxyPlayers();

  /**
   * @return returns true if this NetworkProxy is the same proxy
   */
  boolean isMe();
}
