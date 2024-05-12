package com.imaginarycode.minecraft.redisbungee.api.util.serialize;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.ByteArrayDataOutput;

import java.util.Collection;
import java.util.Map;

public class MultiMapSerialization {

    public static void serializeMultiset(Multiset<String> collection, ByteArrayDataOutput output) {
        output.writeInt(collection.elementSet().size());
        for (Multiset.Entry<String> entry : collection.entrySet()) {
            output.writeUTF(entry.getElement());
            output.writeInt(entry.getCount());
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static void serializeMultimap(Multimap<String, String> collection, boolean includeNames, ByteArrayDataOutput output) {
        output.writeInt(collection.keySet().size());
        for (Map.Entry<String, Collection<String>> entry : collection.asMap().entrySet()) {
            output.writeUTF(entry.getKey());
            if (includeNames) {
                serializeCollection(entry.getValue(), output);
            } else {
                output.writeInt(entry.getValue().size());
            }
        }
    }

    public static void serializeCollection(Collection<?> collection, ByteArrayDataOutput output) {
        output.writeInt(collection.size());
        for (Object o : collection) {
            output.writeUTF(o.toString());
        }
    }

}
