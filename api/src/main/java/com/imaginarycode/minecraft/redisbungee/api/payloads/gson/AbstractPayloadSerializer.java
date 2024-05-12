/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.payloads.gson;

import com.google.gson.*;
import com.imaginarycode.minecraft.redisbungee.api.payloads.AbstractPayload;

import java.lang.reflect.Type;

public class AbstractPayloadSerializer implements JsonSerializer<AbstractPayload>, JsonDeserializer<AbstractPayload> {


    @Override
    public AbstractPayload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        return new AbstractPayload(jsonObject.get("proxy").getAsString(), jsonObject.get("class").getAsString()) {
        };
    }

    @Override
    public JsonElement serialize(AbstractPayload src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("proxy", new JsonPrimitive(src.senderProxy()));
        return jsonObject;
    }
}
