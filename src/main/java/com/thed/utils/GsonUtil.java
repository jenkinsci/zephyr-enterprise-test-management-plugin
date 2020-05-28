package com.thed.utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;

public class GsonUtil {

    public static Gson CUSTOM_GSON;

    static {
        JsonSerializer<Date> ser = new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                    context) {
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        };

        JsonDeserializer<Date> deser = new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context) throws JsonParseException {
                return json == null ? null : new Date(json.getAsLong());
            }
        };

        GsonBuilder builder = new GsonBuilder();
        CUSTOM_GSON = builder.registerTypeAdapter(Date.class, ser)
                .registerTypeAdapter(Date.class, deser).create();
    }

}