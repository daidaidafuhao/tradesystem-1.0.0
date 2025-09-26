package com.tradesystem.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 交易记录数据类
 */
public class TransactionRecord {
    private final UUID transactionId;
    private final UUID sellerId;
    private final String sellerName;
    private final UUID buyerId;
    private final String buyerName;
    private final ItemStack itemStack;
    private final int price;
    private final long timestamp;
    private final Type type;

    public TransactionRecord(UUID sellerId, String sellerName, UUID buyerId, String buyerName,
                           ItemStack itemStack, int price, Type type) {
        this.transactionId = UUID.randomUUID();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.itemStack = itemStack.copy();
        this.price = price;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    public TransactionRecord(CompoundTag tag) {
        this.transactionId = tag.getUUID("transactionId");
        this.sellerId = tag.getUUID("sellerId");
        this.sellerName = tag.getString("sellerName");
        this.buyerId = tag.getUUID("buyerId");
        this.buyerName = tag.getString("buyerName");
        this.itemStack = ItemStack.of(tag.getCompound("itemStack"));
        this.price = tag.getInt("price");
        this.timestamp = tag.getLong("timestamp");
        this.type = Type.valueOf(tag.getString("type"));
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("transactionId", this.transactionId);
        tag.putUUID("sellerId", this.sellerId);
        tag.putString("sellerName", this.sellerName);
        tag.putUUID("buyerId", this.buyerId);
        tag.putString("buyerName", this.buyerName);
        tag.put("itemStack", this.itemStack.save(new CompoundTag()));
        tag.putInt("price", this.price);
        tag.putLong("timestamp", this.timestamp);
        tag.putString("type", this.type.name());
        return tag;
    }

    // Getters
    public UUID getTransactionId() { return transactionId; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public UUID getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public ItemStack getItemStack() { return itemStack.copy(); }
    public int getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
    public Type getType() { return type; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        return sdf.format(new Date(timestamp));
    }

    public enum Type {
        BUY, SELL, RECYCLE
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TransactionRecord record = (TransactionRecord) obj;
        return transactionId.equals(record.transactionId);
    }

    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("TransactionRecord{id=%s, type=%s, item=%s, price=%d}",
                transactionId, type, itemStack.getHoverName().getString(), price);
    }
}