package net.limework.valiobungee.pluginmessage.protocol;

import net.limework.pluginmessageapi.protocol.PluginMessageHandler;
import net.limework.valiobungee.pluginmessage.protocol.messages.proxybound.RequestProxiesListMessage;

public interface ProxyboundHandler extends PluginMessageHandler {

    void handle(RequestProxiesListMessage message);

}
