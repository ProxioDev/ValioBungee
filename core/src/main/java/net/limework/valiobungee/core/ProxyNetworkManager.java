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
package net.limework.valiobungee.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import java.time.Duration;
import java.util.UUID;
import net.limework.valiobungee.core.proto.messages.*;
import net.limework.valiobungee.core.util.logging.LogProviderFactory;
import org.slf4j.Logger;

/**
 * This abstract class is responsible for proxy discovery and count online players as its cached
 * locally Why abstract? Because it allows us to develop alternative implementation to use other
 * software than valkey or redis
 */
public abstract class ProxyNetworkManager {

  protected final UUID proxyManagerId = UUID.randomUUID();
  protected final ValioBungeePlatform platform;

  public ProxyNetworkManager(ValioBungeePlatform platform) {
    this.platform = platform;
  }

  protected final Logger log = LogProviderFactory.get();

  // proxy info class here stores the ProxyManager ID hence we use another cache for online players
  // reason we use caffeine cache it allows us to auto remove entries without need for scheduled
  // tasks in the proxies when a proxy disappears without sending death message
  private final Cache<String, Heartbeat> heartbeats =
      Caffeine.newBuilder()
          .expireAfterWrite(Duration.ofSeconds(30))
          .removalListener(
              (RemovalListener<String, Heartbeat>)
                  (key, value, cause) -> {
                    if (cause == RemovalCause.EXPIRED) {
                      assert value != null;
                      log.warn("proxy {} has disconnected but did not send death message", value);
                    }
                  })
          .build();

  protected void handleProxyHeartBeat(Heartbeat payload) {
    if (!this.heartbeats.asMap().containsKey(payload.getSender().getProxyId()))
      log.info("Proxy {} has connected!", payload.getSender().getProxyId());
    this.heartbeats.put(payload.getSender().getProxyId(), payload);
  }

  protected void handleProxyDeath(Death payload) {
    this.heartbeats.invalidate(payload.getSender().getProxyId());
    log.info("Proxy {} has disconnected", payload.getSender().getProxyId());
  }

  public int onlinePlayersCount() {
    // reason we + local online players because our proxy is not inside it own heartbeat cache
    return heartbeats.asMap().values().stream().mapToInt(Heartbeat::getOnlinePlayersCount).sum()
        + platform.localOnlinePlayers();
  }

  public int onlinePlayersCount(String proxyId) {
    if (proxyId.equals(this.platform.proxyId())) {
      return platform.localOnlinePlayers();
    } else {
      if (!heartbeats.asMap().containsKey(proxyId)) return 0; // don't error?
      return heartbeats.asMap().get(proxyId).getOnlinePlayersCount();
    }
  }

  protected ProxyInfo createProxyInfo() {
    return ProxyInfo.newBuilder()
        .setProxyId(platform.proxyId())
        .setProxyManagerId(this.proxyManagerId.toString())
        .build();
  }

  protected Death createDeathPayload() {
    return Death.newBuilder().setSender(createProxyInfo()).setReason(DeathReason.SHUTDOWN).build();
  }

  protected Heartbeat createHeartbeatPayload() {
    return Heartbeat.newBuilder()
        .setSender(createProxyInfo())
        .setOnlinePlayersCount(platform.localOnlinePlayers())
        .build();
  }

  protected abstract void publishDeathPayload();

  protected abstract void publishHeartbeatPayload();

  public abstract void init();

  public abstract void close();
}
