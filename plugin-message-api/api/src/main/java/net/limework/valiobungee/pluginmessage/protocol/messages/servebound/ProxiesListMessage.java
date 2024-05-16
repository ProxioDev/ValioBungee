package net.limework.valiobungee.pluginmessage.protocol.messages.servebound;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import net.limework.pluginmessageapi.protocol.PluginMessage;
import net.limework.pluginmessageapi.protocol.PluginMessageHandler;
import net.limework.valiobungee.pluginmessage.models.Proxy;
import net.limework.valiobungee.pluginmessage.protocol.ServerboundHandler;

public class ProxiesListMessage extends PluginMessage {

    private Proxy[] proxies;

    public ProxiesListMessage() {

    }

    public Proxy[] proxies() {
        return proxies;
    }

    @Override
    public void decode(ByteArrayDataInput in) {
        int proxiesNumber = in.readInt();
        this.proxies = new Proxy[proxiesNumber];
        for (int i = 0; i < proxiesNumber; i++) {
            String id = in.readUTF();
            int players = in.readInt();
            proxies[i] = new Proxy(id, players);
        }
    }

    @Override
    public void encode(ByteArrayDataOutput out) {
        out.writeInt(proxies.length);
        for (Proxy proxy : proxies) {
            out.writeUTF(proxy.proxyId());
            out.writeInt(proxy.players());
        }
    }

    @Override
    public void handle(PluginMessageHandler handler) {
        ((ServerboundHandler) handler).handle(this);
    }
}
