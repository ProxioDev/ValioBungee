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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import net.limework.valiobungee.core.proto.messages.Death;
import net.limework.valiobungee.core.proto.messages.DeathReason;
import net.limework.valiobungee.core.proto.messages.Heartbeat;
import net.limework.valiobungee.core.proto.messages.ProxyInfo;

/** this class is used to debug the abstract class */
public class StandaloneProxyNetworkManager extends ProxyNetworkManager {
  public StandaloneProxyNetworkManager(ValioBungeePlatform platform) {
    super(platform);
  }

  @Override
  protected void publishDeathPayload() {}

  @Override
  protected void publishHeartbeatPayload() {}

  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  private void makeFakeProxiesRunning() {
    // fake 12 proxies
    String fakePrefix = "fake-proxy-";
    for (int i = 0; i < 6; i++) {
      String prefix = fakePrefix + i;
      Heartbeat heartbeat =
          Heartbeat.newBuilder()
              .setSender(ProxyInfo.newBuilder().setProxyId(prefix).build())
              .setOnlinePlayersCount(i)
              .build();
      Thread.ofVirtual()
          .start(
              () -> {
                while (!shutdown.get()) {
                  handleProxyHeartBeat(heartbeat);
                  try {
                    Thread.sleep(1000);
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                }
                handleProxyDeath(
                    Death.newBuilder()
                        .setReason(DeathReason.SHUTDOWN)
                        .setSender(heartbeat.getSender())
                        .build());
              });
    }
  }

  private void makeFakeProxiesDisappearing() {
    // fake 12 proxies
    String fakePrefix = "fake-disappear-proxy-";
    for (int i = 0; i < 2; i++) {
      String prefix = fakePrefix + i;
      Heartbeat heartbeat =
          Heartbeat.newBuilder()
              .setSender(ProxyInfo.newBuilder().setProxyId(prefix).build())
              .setOnlinePlayersCount(i)
              .build();
      handleProxyHeartBeat(heartbeat);
    }
  }

  private void makeFakeProxiesShuttingDown() {
    // fake 12 proxies
    String fakePrefix = "fake-shutdown-proxy-";
    for (int i = 0; i < 2; i++) {
      String prefix = fakePrefix + i;
      Heartbeat heartbeat =
          Heartbeat.newBuilder()
              .setSender(ProxyInfo.newBuilder().setProxyId(prefix).build())
              .setOnlinePlayersCount(i)
              .build();
      Thread.ofVirtual()
          .start(
              () -> {
                int li = 0;
                int liMax = 60 + ThreadLocalRandom.current().nextInt(20);
                while (!shutdown.get() && li <= liMax) {
                  handleProxyHeartBeat(heartbeat);
                  try {
                    Thread.sleep(1000);
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                  li++;
                }
                handleProxyDeath(
                    Death.newBuilder()
                        .setReason(DeathReason.SHUTDOWN)
                        .setSender(heartbeat.getSender())
                        .build());
              });
    }
  }

  @Override
  public void init() {
    makeFakeProxiesRunning();
    makeFakeProxiesDisappearing();
    makeFakeProxiesShuttingDown();
  }

  @Override
  public void close() {
    shutdown.set(true);
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {

    }
  }
}
