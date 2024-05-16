package net.limework.valiobungee.pluginmessage.protocol.messages.proxybound;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import net.limework.pluginmessageapi.protocol.PluginMessage;


public abstract class RequestMessage extends PluginMessage {


    public RequestMessage() {

    }

    @Override
    public void encode(ByteArrayDataOutput out) {
        // do nothing
    }

    @Override
    public void decode(ByteArrayDataInput in) {
        // do nothing
    }
}
