package com.thed.utils;

import com.google.gson.*;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

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

    public static Map<String, String> validateAndParseJson(String jsonString) throws IllegalArgumentException {
        Map<String, String> customProperties = new java.util.HashMap<>();
        if (jsonString != null && !jsonString.trim().isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                for (Object key : jsonObject.keySet()) {
                    customProperties.put(key.toString(), jsonObject.getString(key.toString()));
                }
                return customProperties;
            } catch (org.json.JSONException e) {
                throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse custom properties: " + e.getMessage());
            }
        }
        return customProperties;
    }
}