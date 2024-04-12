/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imaginarycode.minecraft.redisbungee.api.payloads.AbstractPayload;
import com.imaginarycode.minecraft.redisbungee.api.payloads.gson.AbstractPayloadSerializer;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.DeathPayload;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.HeartbeatPayload;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.PubSubPayload;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.RunCommandPayload;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.gson.DeathPayloadSerializer;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.gson.HeartbeatPayloadSerializer;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.gson.PubSubPayloadSerializer;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.gson.RunCommandPayloadSerializer;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisPipelineTask;
import redis.clients.jedis.*;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class ProxyDataManager implements Runnable, AutoCloseable {

    private static final String STREAM_ID = "redisbungee-stream";
    private static final int MAX_ENTRIES = 10000;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final UnifiedJedis unifiedJedis;

    // data:
    // Proxy id, heartbeat (unix epoch from instant), players as int
    private final ConcurrentHashMap<String, HeartbeatPayload.HeartbeatData> heartbeats = new ConcurrentHashMap<>();

    private final String proxyId;

    // This different from proxy id, just to detect if there is duplicate proxy using same proxy id
    private final UUID dataManagerUUID = UUID.randomUUID();

    protected final RedisBungeePlugin<?> plugin;

    private final Gson gson = new GsonBuilder().registerTypeAdapter(AbstractPayload.class, new AbstractPayloadSerializer()).registerTypeAdapter(HeartbeatPayload.class, new HeartbeatPayloadSerializer()).registerTypeAdapter(DeathPayload.class, new DeathPayloadSerializer()).registerTypeAdapter(PubSubPayload.class, new PubSubPayloadSerializer()).registerTypeAdapter(RunCommandPayload.class, new RunCommandPayloadSerializer()).create();

    public ProxyDataManager(RedisBungeePlugin<?> plugin) {
        this.plugin = plugin;
        this.proxyId = this.plugin.configuration().getProxyId();
        this.unifiedJedis = plugin.getSummoner().obtainResource();
        this.destroyProxyMembers();
    }

    public abstract Set<UUID> getLocalOnlineUUIDs();

    public Set<UUID> getPlayersOn(String proxyId) {
        checkArgument(proxiesIds().contains(proxyId), proxyId + " is not a valid proxy ID");
        if (proxyId.equals(this.proxyId)) return this.getLocalOnlineUUIDs();
        if (!this.heartbeats.containsKey(proxyId)) {
            return new HashSet<>();  // return empty hashset or null?
        }
        return getProxyMembers(proxyId);
    }

    public List<String> proxiesIds() {
        return Collections.list(this.heartbeats.keys());
    }

    public synchronized void sendCommandTo(String proxyToRun, String command) {
        if (isClosed()) return;
        publishPayload(new RunCommandPayload(this.proxyId, proxyToRun, command));
    }

    public synchronized void sendChannelMessage(String channel, String message) {
        if (isClosed()) return;
        this.plugin.fireEvent(this.plugin.createPubSubEvent(channel, message));
        publishPayload(new PubSubPayload(this.proxyId, channel, message));
    }

    // call every 1 second
    public synchronized void publishHeartbeat() {
        if (isClosed()) return;
        HeartbeatPayload.HeartbeatData heartbeatData = new HeartbeatPayload.HeartbeatData(Instant.now().getEpochSecond(), this.getLocalOnlineUUIDs().size());
        this.heartbeats.put(this.proxyId(), heartbeatData);
        publishPayload(new HeartbeatPayload(this.proxyId, heartbeatData));
    }

    public Set<UUID> networkPlayers() {
        try {
            return new RedisPipelineTask<Set<UUID>>(this.plugin) {
                @Override
                public Set<UUID> doPooledPipeline(Pipeline pipeline) {
                    HashSet<Response<Set<String>>> responses = new HashSet<>();
                    for (String proxyId : proxiesIds()) {
                        responses.add(pipeline.smembers("redisbungee::proxies::" + proxyId + "::online-players"));
                    }
                    pipeline.sync();
                    HashSet<UUID> uuids = new HashSet<>();
                    for (Response<Set<String>> response : responses) {
                        for (String stringUUID : response.get()) {
                            uuids.add(UUID.fromString(stringUUID));
                        }
                    }
                    return uuids;
                }

                @Override
                public Set<UUID> clusterPipeline(ClusterPipeline pipeline) {
                    HashSet<Response<Set<String>>> responses = new HashSet<>();
                    for (String proxyId : proxiesIds()) {
                        responses.add(pipeline.smembers("redisbungee::proxies::" + proxyId + "::online-players"));
                    }
                    pipeline.sync();
                    HashSet<UUID> uuids = new HashSet<>();
                    for (Response<Set<String>> response : responses) {
                        for (String stringUUID : response.get()) {
                            uuids.add(UUID.fromString(stringUUID));
                        }
                    }
                    return uuids;
                }
            }.call();
        } catch (Exception e) {
            throw new RuntimeException("unable to get network players", e);
        }

    }

    public int totalNetworkPlayers() {
        int players = 0;
        for (HeartbeatPayload.HeartbeatData value : this.heartbeats.values()) {
            players += value.players();
        }
        return players;
    }

    // Call on close
    private synchronized void publishDeath() {
        publishPayload(new DeathPayload(this.proxyId));
    }

    private void publishPayload(AbstractPayload payload) {
        Map<String, String> data = new HashMap<>();
        data.put("payload", gson.toJson(payload));
        data.put("data-manager-uuid", this.dataManagerUUID.toString());
        data.put("class", payload.getClassName());
        this.unifiedJedis.xadd(STREAM_ID, XAddParams.xAddParams().maxLen(MAX_ENTRIES).id(StreamEntryID.NEW_ENTRY), data);
    }


    private void handleHeartBeat(HeartbeatPayload payload) {
        String id = payload.senderProxy();
        if (!heartbeats.containsKey(id)) {
            plugin.logInfo("Proxy {} has connected", id);
        }
        heartbeats.put(id, payload.data());
    }


    // call every 1 minutes
    public void correctionTask() {
        // let's check this proxy players
        Set<UUID> localOnlineUUIDs = getLocalOnlineUUIDs();
        Set<UUID> storedRedisUuids = getProxyMembers(this.proxyId);

        if (!localOnlineUUIDs.equals(storedRedisUuids)) {
            plugin.logWarn("De-synced playerS set detected correcting....");
            Set<UUID> add = new HashSet<>(localOnlineUUIDs);
            Set<UUID> remove = new HashSet<>(storedRedisUuids);
            add.removeAll(storedRedisUuids);
            remove.removeAll(localOnlineUUIDs);
            for (UUID uuid : add) {
                plugin.logWarn("found {} that isn't in the set, adding it to the Corrected set", uuid);
            }
            for (UUID uuid : remove) {
                plugin.logWarn("found {} that does not belong to this proxy removing it from the corrected set", uuid);
            }
            try {
                new RedisPipelineTask<Void>(plugin) {
                    @Override
                    public Void doPooledPipeline(Pipeline pipeline) {
                        Set<String> removeString = new HashSet<>();
                        for (UUID uuid : remove) {
                            removeString.add(uuid.toString());
                        }
                        Set<String> addString = new HashSet<>();
                        for (UUID uuid : add) {
                            addString.add(uuid.toString());
                        }
                        pipeline.srem("redisbungee::proxies::" + proxyId() + "::online-players", removeString.toArray(new String[]{}));
                        pipeline.sadd("redisbungee::proxies::" + proxyId() + "::online-players", addString.toArray(new String[]{}));
                        pipeline.sync();
                        return null;
                    }

                    @Override
                    public Void clusterPipeline(ClusterPipeline pipeline) {
                        Set<String> removeString = new HashSet<>();
                        for (UUID uuid : remove) {
                            removeString.add(uuid.toString());
                        }
                        Set<String> addString = new HashSet<>();
                        for (UUID uuid : add) {
                            addString.add(uuid.toString());
                        }
                        pipeline.srem("redisbungee::proxies::" + proxyId() + "::online-players", removeString.toArray(new String[]{}));
                        pipeline.sadd("redisbungee::proxies::" + proxyId() + "::online-players", addString.toArray(new String[]{}));
                        pipeline.sync();
                        return null;
                    }
                }.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            plugin.logInfo("Player set has been corrected!");
        }


        // handle dead proxies "THAT" Didn't send death payload but considered dead due TIMEOUT ~10 seconds
        final Set<String> deadProxies = new HashSet<>();
        for (Map.Entry<String, HeartbeatPayload.HeartbeatData> stringHeartbeatDataEntry : this.heartbeats.entrySet()) {
            String id = stringHeartbeatDataEntry.getKey();
            long heartbeat = stringHeartbeatDataEntry.getValue().heartbeat();
            if (Instant.now().getEpochSecond() - heartbeat > 10) {
                deadProxies.add(id);
                cleanProxy(id);
            }
        }
        try {
            new RedisPipelineTask<Void>(plugin) {
                @Override
                public Void doPooledPipeline(Pipeline pipeline) {
                    for (String deadProxy : deadProxies) {
                        pipeline.del("redisbungee::proxies::" + deadProxy + "::online-players");
                    }
                    pipeline.sync();
                    return null;
                }

                @Override
                public Void clusterPipeline(ClusterPipeline pipeline) {
                    for (String deadProxy : deadProxies) {
                        pipeline.del("redisbungee::proxies::" + deadProxy + "::online-players");
                    }
                    pipeline.sync();
                    return null;
                }
            }.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleProxyDeath(DeathPayload payload) {
        cleanProxy(payload.senderProxy());
    }

    private void cleanProxy(String id) {
        if (id.equals(this.proxyId())) {
            return;
        }
        for (UUID uuid : getProxyMembers(id)) plugin.fireEvent(plugin.createPlayerLeftNetworkEvent(uuid));
        plugin.logInfo("Proxy {} has disconnected", id);
    }

    private void handleChannelMessage(PubSubPayload payload) {
        String channel = payload.channel();
        String message = payload.message();
        this.plugin.fireEvent(this.plugin.createPubSubEvent(channel, message));
    }

    protected abstract void handlePlatformCommandExecution(String command);

    private void handleCommand(RunCommandPayload payload) {
        String proxyToRun = payload.proxyToRun();
        String command = payload.command();
        if (proxyToRun.equals("allservers") || proxyToRun.equals(this.proxyId())) {
            handlePlatformCommandExecution(command);
        }
    }


    public void addPlayer(UUID uuid) {
        this.unifiedJedis.sadd("redisbungee::proxies::" + this.proxyId + "::online-players", uuid.toString());
    }

    public void removePlayer(UUID uuid) {
        this.unifiedJedis.srem("redisbungee::proxies::" + this.proxyId + "::online-players", uuid.toString());
    }

    private void destroyProxyMembers() {
        unifiedJedis.del("redisbungee::proxies::" + this.proxyId + "::online-players");
    }

    private Set<UUID> getProxyMembers(String proxyId) {
        Set<String> uuidsStrings = unifiedJedis.smembers("redisbungee::proxies::" + proxyId + "::online-players");
        HashSet<UUID> uuids = new HashSet<>();
        for (String proxyMember : uuidsStrings) {
            uuids.add(UUID.fromString(proxyMember));
        }
        return uuids;
    }

    private StreamEntryID lastStreamEntryID;

    // polling from stream
    @Override
    public void run() {
        while (!isClosed()) {
            try {
                List<java.util.Map.Entry<String, List<StreamEntry>>> data = unifiedJedis.xread(XReadParams.xReadParams().block(0), Collections.singletonMap(STREAM_ID, lastStreamEntryID != null ? lastStreamEntryID : StreamEntryID.LAST_ENTRY));
                for (Map.Entry<String, List<StreamEntry>> datum : data) {
                    for (StreamEntry streamEntry : datum.getValue()) {
                        this.lastStreamEntryID = streamEntry.getID();
                        String payloadData = streamEntry.getFields().get("payload");
                        String clazz = streamEntry.getFields().get("class");
                        UUID payloadDataManagerUUID = UUID.fromString(streamEntry.getFields().get("data-manager-uuid"));

                        AbstractPayload unknownPayload = (AbstractPayload) gson.fromJson(payloadData, Class.forName(clazz));

                        if (unknownPayload.senderProxy().equals(this.proxyId)) {
                            if (!payloadDataManagerUUID.equals(this.dataManagerUUID)) {
                                plugin.logWarn("detected other proxy is using same ID! {} this can cause issues, please shutdown this proxy and change the id!", this.proxyId);
                            }
                            break;
                        }
                        if (unknownPayload instanceof HeartbeatPayload payload) {
                            handleHeartBeat(payload);
                        } else if (unknownPayload instanceof DeathPayload payload) {
                            handleProxyDeath(payload);
                        } else if (unknownPayload instanceof RunCommandPayload payload) {
                            handleCommand(payload);
                        } else if (unknownPayload instanceof PubSubPayload payload) {
                            handleChannelMessage(payload);
                        } else {
                            plugin.logWarn("got unknown data manager payload: {}", unknownPayload.getClassName());
                        }
                    }
                }
            } catch (Exception e) {
                this.plugin.logFatal("an error has occurred in the stream", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        closed.set(true);
        this.publishDeath();
        this.heartbeats.clear();
        this.destroyProxyMembers();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public String proxyId() {
        return proxyId;
    }

    public UnifiedJedis unifiedJedis() {
        return unifiedJedis;
    }

}
