package net.limework.pluginmessageapi.protocol;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class MessageEncoderDecoder {

    private final PluginMessageRegistry registry;

    public MessageEncoderDecoder(PluginMessageRegistry registry) {
        this.registry = registry;
    }

    public byte[] encode(PluginMessage pluginMessage) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // write packet id
        out.write(registry.idByMessage(pluginMessage.getClass()));
        pluginMessage.encode(out);
        return out.toByteArray();
    }

    public PluginMessage decode(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        int messageId = in.readInt();
        PluginMessage pluginMessage = registry.messageById(messageId);
        pluginMessage.decode(in);
        return pluginMessage;
    }

}
