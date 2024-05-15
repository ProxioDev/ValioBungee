package net.limework.pluginmessageapi.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PluginMessageRegistry {

    private final Map<Integer, Supplier<PluginMessage>> messageById = new HashMap<>();
    private final Map<Class<? extends PluginMessage>, Integer> idByMessage = new HashMap<>();


    public void register(int id, Class<? extends PluginMessage> clazz, Supplier<PluginMessage> messageSupplier) {
        messageById.put(id, messageSupplier);
        idByMessage.put(clazz, id);
    }


    public PluginMessage messageById(int id) {
        if (!messageById.containsKey(id)) {
            throw new IllegalArgumentException("plugin Message id " + id + " does not exists in the registry");
        }
        return messageById.get(id).get();
    }


    public int idByMessage(Class<? extends PluginMessage> clazz) {
        if (!idByMessage.containsKey(clazz)) {
            throw new IllegalArgumentException("plugin Message clazz " + clazz + " does not exists in the registry");
        }
        return idByMessage.get(clazz);
    }

}
