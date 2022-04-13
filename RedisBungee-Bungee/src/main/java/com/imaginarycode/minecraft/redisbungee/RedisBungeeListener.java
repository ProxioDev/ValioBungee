package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.internal.AbstractRedisBungeeListener;
import com.imaginarycode.minecraft.redisbungee.internal.DataManager;
import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.events.bungee.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.RedisUtil;
import com.imaginarycode.minecraft.redisbungee.internal.util.RedisCallable;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.net.InetAddress;
import java.util.List;

public class RedisBungeeListener extends AbstractRedisBungeeListener<LoginEvent, PostLoginEvent, PlayerDisconnectEvent, ServerConnectedEvent, ProxyPingEvent, PluginMessageEvent, PubSubMessageEvent> implements Listener {


    public RedisBungeeListener(RedisBungeePlugin<?> plugin, List<InetAddress> exemptAddresses) {
        super(plugin, exemptAddresses);
    }

    @Override
    @EventHandler
    public void onLogin(LoginEvent event) {
        event.registerIntent((Plugin) plugin);
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                try {
                    if (event.isCancelled()) {
                        return null;
                    }

                    // We make sure they aren't trying to use an existing player's name.
                    // This is problematic for online-mode servers as they always disconnect old clients.
                    if (plugin.isOnlineMode()) {
                        ProxiedPlayer player = (ProxiedPlayer) plugin.getPlayer(event.getConnection().getName());

                        if (player != null) {
                            event.setCancelled(true);
                            // TODO: Make it accept a BaseComponent[] like everything else.
                            event.setCancelReason(ONLINE_MODE_RECONNECT);
                            return null;
                        }
                    }

                    for (String s : plugin.getServerIds()) {
                        if (jedis.sismember("proxy:" + s + ":usersOnline", event.getConnection().getUniqueId().toString())) {
                            event.setCancelled(true);
                            // TODO: Make it accept a BaseComponent[] like everything else.
                            event.setCancelReason(ALREADY_LOGGED_IN);
                            return null;
                        }
                    }
                    return null;
                } finally {
                    event.completeIntent((Plugin) plugin);
                }
            }
        });
    }

    @Override
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                // this code was moved out from login event due being async..
                // and it can be cancelled but it will show as false in redis-bungee
                // which will register the player into the redis database.
                Pipeline pipeline = jedis.pipelined();
                plugin.getUuidTranslator().persistInfo(event.getPlayer().getName(), event.getPlayer().getUniqueId(), pipeline);
                RBUtils.createPlayer(event.getPlayer(), pipeline, false);
                pipeline.sync();
                // the end of moved code.

                jedis.publish("redisbungee-data", gson.toJson(new DataManager.DataManagerMessage(
                        event.getPlayer().getUniqueId(), plugin.getApi().getServerId(), DataManager.DataManagerMessage.Action.JOIN,
                        new DataManager.LoginPayload(event.getPlayer().getAddress().getAddress()))));
                return null;
            }
        });
    }

    @Override
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
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
    @EventHandler
    public void onServerChange(ServerConnectedEvent event) {

    }

    @Override
    @EventHandler
    public void onPing(ProxyPingEvent event) {

    }

    @Override
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {

    }

    @Override
    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + plugin.getApi().getServerId())) {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.logInfo("Invoking command via PubSub: /" + message);
            plugin.executeProxyCommand(message);
        }
    }
}
