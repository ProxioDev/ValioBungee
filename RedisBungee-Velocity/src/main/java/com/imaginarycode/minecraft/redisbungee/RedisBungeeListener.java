package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.internal.AbstractRedisBungeeListener;
import com.imaginarycode.minecraft.redisbungee.internal.AbstractDataManager;
import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.RedisUtil;
import com.imaginarycode.minecraft.redisbungee.internal.util.RedisCallable;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

public class RedisBungeeListener extends AbstractRedisBungeeListener<LoginEvent, PostLoginEvent, DisconnectEvent, ServerConnectedEvent, ProxyPingEvent, PluginMessageEvent, PubSubMessageEvent> {
    // Some messages are using legacy characters
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public RedisBungeeListener(RedisBungeePlugin<?> plugin, List<InetAddress> exemptAddresses) {
        super(plugin, exemptAddresses);
    }

    @Subscribe
    public void onLogin(LoginEvent event, Continuation continuation) {
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                try {
                    if (!event.getResult().isAllowed()) {
                        return null;
                    }

                    // We make sure they aren't trying to use an existing player's name.
                    // This is problematic for online-mode servers as they always disconnect old clients.
                    if (plugin.isOnlineMode()) {
                        Player player = (Player) plugin.getPlayer(event.getPlayer().getUsername());

                        if (player != null) {
                            event.setResult(ResultedEvent.ComponentResult.denied(serializer.deserialize(ONLINE_MODE_RECONNECT)));
                            return null;
                        }
                    }

                    for (String s : plugin.getServerIds()) {
                        if (jedis.sismember("proxy:" + s + ":usersOnline", event.getPlayer().getUniqueId().toString())) {
                            event.setResult(ResultedEvent.ComponentResult.denied(serializer.deserialize(ALREADY_LOGGED_IN)));
                            return null;
                        }
                    }
                    return null;
                } finally {
                    continuation.resume();
                }
            }
        });
    }

    @Override
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                // this code was moved out from login event due being async..
                // and it can be cancelled but it will show as false in redis-bungee
                // which will register the player into the redis database.
                Pipeline pipeline = jedis.pipelined();
                plugin.getUuidTranslator().persistInfo(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), pipeline);
                RBUtils.createPlayer(event.getPlayer(), pipeline, false);
                pipeline.sync();
                // the end of moved code.

                jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getServerId(), AbstractDataManager.DataManagerMessage.Action.JOIN,
                        new AbstractDataManager.LoginPayload(event.getPlayer().getRemoteAddress().getAddress()))));
                return null;
            }
        });
    }

    @Override
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                RedisUtil.cleanUpPlayer(event.getPlayer().getUniqueId().toString(), pipeline);
                pipeline.sync();
                return null;
            }
        });

    }

    @Override
    @Subscribe
    public void onServerChange(ServerConnectedEvent event) {
        Optional<ServerConnection> optionalServerConnection = event.getPlayer().getCurrentServer();
        final String currentServer = optionalServerConnection.map(serverConnection -> serverConnection.getServerInfo().getName()).orElse(null);
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "server", event.getServer().getServerInfo().getName());
                jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getServerId(), AbstractDataManager.DataManagerMessage.Action.SERVER_CHANGE,
                        new AbstractDataManager.ServerChangePayload(event.getServer().getServerInfo().getName(), currentServer))));
                return null;
            }
        });
    }

    @Override
    @Subscribe(order = PostOrder.EARLY)
    public void onPing(ProxyPingEvent event) {
        if (exemptAddresses.contains(event.getConnection().getRemoteAddress().getAddress())) {
            return;
        }
        ServerPing.Builder ping = event.getPing().asBuilder();
        ping.onlinePlayers(plugin.getCount());
        event.setPing(ping.build());
    }

    @Override
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if(!(event.getSource() instanceof ServerConnection) || !RedisBungeeVelocityPlugin.IDENTIFIERS.contains(event.getIdentifier())) {
            return;
        }

        event.setResult(ForwardResult.handled());

        plugin.executeAsync(() -> {
            ByteArrayDataInput in = event.dataAsDataStream();

            String subchannel = in.readUTF();
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            String type;

            switch (subchannel) {
                case "PlayerList":
                    out.writeUTF("PlayerList");
                    Set<UUID> original = Collections.emptySet();
                    type = in.readUTF();
                    if (type.equals("ALL")) {
                        out.writeUTF("ALL");
                        original = plugin.getPlayers();
                    } else {
                        try {
                            original = plugin.getApi().getPlayersOnServer(type);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    Set<String> players = original.stream()
                        .map(uuid -> plugin.getUuidTranslator().getNameFromUuid(uuid, false))
                        .collect(Collectors.toSet());
                    out.writeUTF(Joiner.on(',').join(players));
                    break;
                case "PlayerCount":
                    out.writeUTF("PlayerCount");
                    type = in.readUTF();
                    if (type.equals("ALL")) {
                        out.writeUTF("ALL");
                        out.writeInt(plugin.getCount());
                    } else {
                        out.writeUTF(type);
                        try {
                            out.writeInt(plugin.getApi().getPlayersOnServer(type).size());
                        } catch (IllegalArgumentException e) {
                            out.writeInt(0);
                        }
                    }
                    break;
                case "LastOnline":
                    String user = in.readUTF();
                    out.writeUTF("LastOnline");
                    out.writeUTF(user);
                    out.writeLong(plugin.getApi().getLastOnline(plugin.getUuidTranslator().getTranslatedUuid(user, true)));
                    break;
                case "ServerPlayers":
                    String type1 = in.readUTF();
                    out.writeUTF("ServerPlayers");
                    Multimap<String, UUID> multimap = plugin.getApi().getServerToPlayers();

                    boolean includesUsers;

                    switch (type1) {
                        case "COUNT":
                            includesUsers = false;
                            break;
                        case "PLAYERS":
                            includesUsers = true;
                            break;
                        default:
                            // TODO: Should I raise an error?
                            return;
                    }

                    out.writeUTF(type1);

                    if (includesUsers) {
                        Multimap<String, String> human = HashMultimap.create();
                        for (Map.Entry<String, UUID> entry : multimap.entries()) {
                            human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                        }
                        serializeMultimap(human, true, out);
                    } else {
                        serializeMultiset(multimap.keys(), out);
                    }
                    break;
                case "Proxy":
                    out.writeUTF("Proxy");
                    out.writeUTF(plugin.getConfiguration().getServerId());
                    break;
                case "PlayerProxy":
                    String username = in.readUTF();
                    out.writeUTF("PlayerProxy");
                    out.writeUTF(username);
                    out.writeUTF(plugin.getApi().getProxy(plugin.getUuidTranslator().getTranslatedUuid(username, true)));
                    break;
                default:
                    return;
            }

            ((ServerConnection) event.getSource()).sendPluginMessage(event.getIdentifier(), out.toByteArray());
        });
        
    }


    @Override
    @Subscribe
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + plugin.getApi().getServerId())) {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.logInfo("Invoking command via PubSub: /" + message);
            ((RedisBungeeVelocityPlugin)plugin).getProxy().getCommandManager().executeAsync(RedisBungeeCommandSource.getSingleton(), message);

        }
    }
}
