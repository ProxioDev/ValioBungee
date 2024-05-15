package net.limework.pluginmessageapi.protocol.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import net.limework.pluginmessageapi.protocol.PluginMessageHandler;
import net.limework.pluginmessageapi.protocol.PluginMessage;

// example plugin message class
public class HelloMessage extends PluginMessage {

    private String helloWorld;

    private static final String DEFAULT_MESSAGE = "hello from Limework!";

    public HelloMessage(String helloWorld) {
        this.helloWorld = helloWorld != null ? helloWorld : DEFAULT_MESSAGE;
    }

    @Override
    public void decode(ByteArrayDataInput in) {
        this.helloWorld = in.readUTF();
    }

    @Override
    public void encode(ByteArrayDataOutput out) {
        out.writeUTF(this.helloWorld);
    }

    @Override
    public void handle(PluginMessageHandler messageHandler) {
        // messageHandler.handle(this);
    }

    public void setHelloWorldMessage(String helloWorld) {
        this.helloWorld = helloWorld != null ? helloWorld : DEFAULT_MESSAGE;
    }

    public String helloWorld() {
        return helloWorld;
    }
}
