package com.tradesystem.mod.data;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * UUID的JSON序列化器
 * 将UUID转换为JSON格式进行存储
 */
public class UUIDSerializer implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
    
    @Override
    public JsonElement serialize(UUID uuid, Type type, JsonSerializationContext context) {
        if (uuid == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(uuid.toString());
    }
    
    @Override
    public UUID deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) 
            throws JsonParseException {
        
        if (jsonElement.isJsonNull()) {
            return null;
        }
        
        try {
            String uuidStr = jsonElement.getAsString();
            return UUID.fromString(uuidStr);
        } catch (Exception e) {
            throw new JsonParseException("Failed to deserialize UUID: " + jsonElement.getAsString(), e);
        }
    }
}