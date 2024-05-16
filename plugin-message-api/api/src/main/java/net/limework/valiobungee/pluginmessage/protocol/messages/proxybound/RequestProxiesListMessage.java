package net.limework.valiobungee.pluginmessage.protocol.messages.proxybound;

import net.limework.pluginmessageapi.protocol.PluginMessageHandler;
import net.limework.valiobungee.pluginmessage.protocol.ProxyboundHandler;

public class RequestProxiesListMessage extends RequestMessage {

    public RequestProxiesListMessage() {
    }

    @Override
    public void handle(PluginMessageHandler handler) {
        ((ProxyboundHandler) handler).handle(this);
    }
}
