package net.limework.valiobungee.pluginmessage.protocol.messages;

import net.limework.pluginmessageapi.protocol.PluginMessageRegistry;
import net.limework.valiobungee.pluginmessage.protocol.messages.proxybound.RequestProxiesListMessage;
import net.limework.valiobungee.pluginmessage.protocol.messages.servebound.ProxiesListMessage;

public class Registry {

    private static final PluginMessageRegistry serverbound = new PluginMessageRegistry();

    private static final PluginMessageRegistry proxybound = new PluginMessageRegistry();

    static {
        registerProxybound();
        registerServerbound();
    }

    private static void registerServerbound() {
        serverbound.register(0, ProxiesListMessage.class, ProxiesListMessage::new);
    }


    private static void registerProxybound() {
        proxybound.register(0, RequestProxiesListMessage.class, RequestProxiesListMessage::new);
    }


    public static PluginMessageRegistry serverbound() {
        return serverbound;
    }


    public static PluginMessageRegistry proxybound() {
        return proxybound;
    }


}
