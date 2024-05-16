package net.limework.valiobungee.pluginmessage.protocol;

import net.limework.pluginmessageapi.protocol.PluginMessageHandler;
import net.limework.valiobungee.pluginmessage.protocol.messages.servebound.ProxiesListMessage;

public interface ServerboundHandler extends PluginMessageHandler {

    void handle(ProxiesListMessage message);

}
