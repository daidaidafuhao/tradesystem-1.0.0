package com.tradesystem.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * 系统商品数据类
 * 表示由管理员管理的系统商品，可以无限购买
 */
public class SystemItem {
    
    private UUID id;
    private ItemStack itemStack;
    private int price;
    private int quantity; // 显示数量，实际为无限
    private boolean active; // 是否上架
    private long createdTime;
    private long lastModified;
    private String createdBy; // 创建者（管理员）
    
    public SystemItem() {
        this.id = UUID.randomUUID();
        this.itemStack = ItemStack.EMPTY;
        this.price = 0;
        this.quantity = 1;
        this.active = true;
        this.createdTime = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.createdBy = "";
    }
    
    public SystemItem(ItemStack itemStack, int price, int quantity, String createdBy) {
        this.id = UUID.randomUUID();
        this.itemStack = itemStack.copy();
        this.price = price;
        this.quantity = quantity;
        this.active = true;
        this.createdTime = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.createdBy = createdBy;
    }
    
    // Getters
    public UUID getId() {
        return id;
    }
    
    public ItemStack getItemStack() {
        return itemStack.copy();
    }
    
    public int getPrice() {
        return price;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public String getItemId() {
        return itemStack.getItem().toString();
    }
    
    public String getDisplayName() {
        return itemStack.getHoverName().getString();
    }
    
    // Setters
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setPrice(int price) {
        this.price = Math.max(0, price);
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setActive(boolean active) {
        this.active = active;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * 切换上架状态
     */
    public void toggleActive() {
        this.active = !this.active;
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * 检查物品是否匹配
     */
    public boolean matches(ItemStack other) {
        return ItemStack.isSameItemSameTags(this.itemStack, other);
    }
    
    /**
     * 创建用于交易的物品副本
     */
    public ItemStack createTradeItem() {
        ItemStack tradeItem = this.itemStack.copy();
        tradeItem.setCount(this.quantity);
        return tradeItem;
    }
    
    /**
     * 序列化到NBT
     */
    public CompoundTag serializeNBT() {
        return toNBT();
    }
    
    /**
     * 从NBT反序列化
     */
    public void deserializeNBT(CompoundTag tag) {
        // 加载物品数据
        if (tag.contains("item")) {
            this.itemStack = ItemStack.of(tag.getCompound("item"));
        }
        
        // 加载基本信息
        if (tag.contains("id")) {
            this.id = tag.getUUID("id");
        }
        this.price = tag.getInt("price");
        this.quantity = tag.getInt("quantity");
        this.active = tag.getBoolean("active");
        this.createdTime = tag.getLong("createdTime");
        this.lastModified = tag.getLong("lastModified");
        this.createdBy = tag.getString("createdBy");
    }
    
    /**
     * 序列化到NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        
        // 保存物品数据
        CompoundTag itemTag = new CompoundTag();
        itemStack.save(itemTag);
        tag.put("item", itemTag);
        
        // 保存基本信息
        tag.putUUID("id", id);
        tag.putInt("price", price);
        tag.putInt("quantity", quantity);
        tag.putBoolean("active", active);
        tag.putLong("createdTime", createdTime);
        tag.putLong("lastModified", lastModified);
        tag.putString("createdBy", createdBy);
        
        return tag;
    }
    
    /**
     * 从NBT反序列化
     */
    public static SystemItem fromNBT(CompoundTag tag) {
        SystemItem item = new SystemItem();
        
        // 加载物品数据
        if (tag.contains("item")) {
            item.itemStack = ItemStack.of(tag.getCompound("item"));
        }
        
        // 加载基本信息
        if (tag.contains("id")) {
            item.id = tag.getUUID("id");
        }
        item.price = tag.getInt("price");
        item.quantity = tag.getInt("quantity");
        item.active = tag.getBoolean("active");
        item.createdTime = tag.getLong("createdTime");
        item.lastModified = tag.getLong("lastModified");
        item.createdBy = tag.getString("createdBy");
        
        return item;
    }
    
    /**
     * 转换为TradeItem用于市场显示
     */
    public TradeItem toTradeItem() {
        TradeItem tradeItem = new TradeItem();
        tradeItem.setItemStack(this.itemStack.copy());
        tradeItem.setPrice(this.price);
        tradeItem.setQuantity(this.quantity);
        tradeItem.setSellerName("系统商店"); // 系统商品的卖家名称
        tradeItem.setSellerId(null); // 系统商品没有卖家ID
        tradeItem.setActive(this.active);
        tradeItem.setSystemItem(true); // 标记为系统商品
        return tradeItem;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SystemItem that = (SystemItem) obj;
        return ItemStack.isSameItemSameTags(this.itemStack, that.itemStack);
    }
    
    @Override
    public int hashCode() {
        return itemStack.getItem().hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("SystemItem{item=%s, price=%d, quantity=%d, active=%s}", 
                getDisplayName(), price, quantity, active);
    }
}