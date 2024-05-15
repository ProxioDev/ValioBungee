package net.limework.pluginmessageapi.protocol;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public abstract class PluginMessage {


    public abstract void decode(ByteArrayDataInput in);

    public abstract void encode(ByteArrayDataOutput out);

    public abstract void handle(PluginMessageHandler handler);


}
