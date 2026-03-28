/*
* Copyright (c) 2026-present ValioBungee contributors
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License Version 2.0
* which accompanies this distribution, and is available at
* https://www.apache.org/licenses/LICENSE-2.0.txt
*/
package net.limework.valiobungee.api;

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
  int onlinePlayers();

  /**
   * @return returns true if this object is proxy on it
   */
  boolean isMe();
}
