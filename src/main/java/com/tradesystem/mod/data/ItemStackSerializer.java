package com.tradesystem.mod.data;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;

/**
 * ItemStack的JSON序列化器
 * 将ItemStack转换为JSON格式进行存储
 */
public class ItemStackSerializer implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    
    @Override
    public JsonElement serialize(ItemStack itemStack, Type type, JsonSerializationContext context) {
        if (itemStack.isEmpty()) {
            return JsonNull.INSTANCE;
        }
        
        JsonObject json = new JsonObject();
        
        // 保存物品ID
        ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        json.addProperty("item", itemId.toString());
        
        // 保存数量
        json.addProperty("count", itemStack.getCount());
        
        // 保存NBT数据
        CompoundTag nbt = itemStack.getTag();
        if (nbt != null && !nbt.isEmpty()) {
            json.addProperty("nbt", nbt.toString());
        }
        
        return json;
    }
    
    @Override
    public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) 
            throws JsonParseException {
        
        if (jsonElement.isJsonNull()) {
            return ItemStack.EMPTY;
        }
        
        JsonObject json = jsonElement.getAsJsonObject();
        
        try {
            // 获取物品ID
            String itemIdStr = json.get("item").getAsString();
            ResourceLocation itemId = ResourceLocation.tryParse(itemIdStr);
            
            // 获取物品
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                throw new JsonParseException("Unknown item: " + itemIdStr);
            }
            
            // 获取数量
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            
            // 创建ItemStack
            ItemStack itemStack = new ItemStack(item, count);
            
            // 设置NBT数据
            if (json.has("nbt")) {
                String nbtStr = json.get("nbt").getAsString();
                try {
                    CompoundTag nbt = TagParser.parseTag(nbtStr);
                    itemStack.setTag(nbt);
                } catch (Exception e) {
                    // NBT解析失败，忽略NBT数据但保留物品
                    System.err.println("Failed to parse NBT for item " + itemIdStr + ": " + e.getMessage());
                }
            }
            
            return itemStack;
            
        } catch (Exception e) {
            throw new JsonParseException("Failed to deserialize ItemStack", e);
        }
    }
}