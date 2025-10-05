package com.tradesystem.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 交易物品数据类
 * 表示一个上架的交易物品
 */
public class TradeItem {
    private UUID id;
    private UUID sellerId;
    private String sellerName;
    private ItemStack itemStack;
    private int price;
    private long listTime;
    private boolean active;
    private boolean isSystemItem = false; // 标记是否为系统商品
    
    /**
     * 构造函数
     */
    public TradeItem(UUID sellerId, String sellerName, ItemStack itemStack, int price) {
        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemStack = itemStack.copy();
        this.price = price;
        this.listTime = System.currentTimeMillis();
        this.active = true;
        
        // 调试日志：检查TradeItem创建时的物品数据
        System.out.println("TradeItem创建 - 物品类型: " + itemStack.getItem().toString() + 
                          ", 数量: " + itemStack.getCount() + 
                          ", NBT: " + (itemStack.getTag() != null ? itemStack.getTag().toString() : "null"));
    }
    
    /**
     * 从NBT数据构造
     */
    public TradeItem(CompoundTag tag) {
        this.id = tag.getUUID("id");
        this.sellerId = tag.getUUID("sellerId");
        this.sellerName = tag.getString("sellerName");
        this.itemStack = ItemStack.of(tag.getCompound("itemStack"));
        this.price = tag.getInt("price");
        this.listTime = tag.getLong("listTime");
        this.active = tag.getBoolean("active");
        this.isSystemItem = tag.getBoolean("isSystemItem");
        
        // 调试日志：检查TradeItem从NBT加载时的物品数据
        System.out.println("TradeItem从NBT加载 - 物品类型: " + itemStack.getItem().toString() + 
                          ", 数量: " + itemStack.getCount() + 
                          ", NBT: " + (itemStack.getTag() != null ? itemStack.getTag().toString() : "null"));
    }
    
    /**
     * 默认构造函数
     */
    public TradeItem() {
        this.id = UUID.randomUUID();
        this.itemStack = ItemStack.EMPTY;
        this.price = 0;
        this.listTime = System.currentTimeMillis();
        this.active = true;
        this.isSystemItem = false;
    }
    
    /**
     * 转换为NBT数据
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", this.id);
        tag.putUUID("sellerId", this.sellerId);
        tag.putString("sellerName", this.sellerName);
        tag.put("itemStack", this.itemStack.save(new CompoundTag()));
        tag.putInt("price", this.price);
        tag.putLong("listTime", this.listTime);
        tag.putBoolean("active", this.active);
        return tag;
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItemStack() { return itemStack.copy(); }
    public int getPrice() { return price; }
    public long getListTime() { return listTime; }
    public boolean isActive() { return active; }
    
    // Setters
    public void setActive(boolean active) { this.active = active; }
    public void setPrice(int price) { this.price = price; }
    public void setSystemItem(boolean isSystemItem) { this.isSystemItem = isSystemItem; }
    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack.copy(); }
    public void setQuantity(int quantity) { this.itemStack.setCount(quantity); }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setSellerId(UUID sellerId) { this.sellerId = sellerId; }
    
    // 系统商品相关方法
    public boolean isSystemItem() { return isSystemItem; }
    
    /**
     * 检查物品是否过期（可配置过期时间）
     */
    public boolean isExpired(long maxAge) {
        return System.currentTimeMillis() - listTime > maxAge;
    }
    
    /**
     * 获取物品显示名称
     */
    public String getDisplayName() {
        return itemStack.getHoverName().getString();
    }
    
    /**
     * 获取物品数量
     */
    public int getCount() {
        return itemStack.getCount();
    }
    
    /**
     * 获取物品描述
     */
    public String getDescription() {
        return "";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TradeItem tradeItem = (TradeItem) obj;
        return id.equals(tradeItem.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("TradeItem{id=%s, seller=%s, item=%s, price=%d, active=%b}",
                id, sellerName, getDisplayName(), price, active);
    }
}