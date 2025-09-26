package com.tradesystem.mod.data;

import com.tradesystem.mod.TradeMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * 交易数据存储类
 * 负责持久化存储所有交易相关数据
 */
public class TradeSavedData extends SavedData {
    
    private static final String DATA_NAME = "tradesystem_data";
    
    // 全局市场数据
    private final Map<String, CompoundTag> globalMarketItems = new HashMap<>();
    
    // 玩家交易数据
    private final Map<UUID, CompoundTag> playerTradeData = new HashMap<>();
    
    // 交易历史记录
    private final List<CompoundTag> tradeHistory = new ArrayList<>();
    
    // 系统统计数据
    private CompoundTag systemStats = new CompoundTag();
    
    public TradeSavedData() {
        initializeSystemStats();
    }
    
    /**
     * 初始化系统统计数据
     */
    private void initializeSystemStats() {
        systemStats.putLong("total_trades", 0L);
        systemStats.putLong("total_money_traded", 0L);
        systemStats.putLong("creation_time", System.currentTimeMillis());
        systemStats.putLong("last_update", System.currentTimeMillis());
    }
    
    /**
     * 获取或创建数据实例
     */
    public static TradeSavedData get(MinecraftServer server) {
        return server.getLevel(net.minecraft.server.level.ServerLevel.OVERWORLD)
                .getDataStorage()
                .computeIfAbsent(
                        TradeSavedData::load,
                        TradeSavedData::new,
                        DATA_NAME
                );
    }
    
    /**
     * 从NBT加载数据
     */
    public static TradeSavedData load(CompoundTag nbt) {
        TradeSavedData data = new TradeSavedData();
        
        // 加载全局市场数据
        if (nbt.contains("global_market", Tag.TAG_COMPOUND)) {
            CompoundTag marketTag = nbt.getCompound("global_market");
            for (String key : marketTag.getAllKeys()) {
                data.globalMarketItems.put(key, marketTag.getCompound(key));
            }
        }
        
        // 加载玩家交易数据
        if (nbt.contains("player_data", Tag.TAG_COMPOUND)) {
            CompoundTag playerTag = nbt.getCompound("player_data");
            for (String key : playerTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    data.playerTradeData.put(playerId, playerTag.getCompound(key));
                } catch (IllegalArgumentException e) {
                    TradeMod.getLogger().warn("无效的玩家UUID: {}", key);
                }
            }
        }
        
        // 加载交易历史
        if (nbt.contains("trade_history", Tag.TAG_LIST)) {
            ListTag historyList = nbt.getList("trade_history", Tag.TAG_COMPOUND);
            for (int i = 0; i < historyList.size(); i++) {
                data.tradeHistory.add(historyList.getCompound(i));
            }
        }
        
        // 加载系统统计
        if (nbt.contains("system_stats", Tag.TAG_COMPOUND)) {
            data.systemStats = nbt.getCompound("system_stats");
        }
        
        TradeMod.getLogger().info("交易数据加载完成，市场物品: {}, 玩家数据: {}, 历史记录: {}",
                data.globalMarketItems.size(), data.playerTradeData.size(), data.tradeHistory.size());
        
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag nbt) {
        // 保存全局市场数据
        CompoundTag marketTag = new CompoundTag();
        for (Map.Entry<String, CompoundTag> entry : globalMarketItems.entrySet()) {
            marketTag.put(entry.getKey(), entry.getValue());
        }
        nbt.put("global_market", marketTag);
        
        // 保存玩家交易数据
        CompoundTag playerTag = new CompoundTag();
        for (Map.Entry<UUID, CompoundTag> entry : playerTradeData.entrySet()) {
            playerTag.put(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("player_data", playerTag);
        
        // 保存交易历史
        ListTag historyList = new ListTag();
        for (CompoundTag historyEntry : tradeHistory) {
            historyList.add(historyEntry);
        }
        nbt.put("trade_history", historyList);
        
        // 保存系统统计
        systemStats.putLong("last_update", System.currentTimeMillis());
        nbt.put("system_stats", systemStats);
        
        return nbt;
    }
    
    // === 全局市场数据操作 ===
    
    public void addMarketItem(String itemId, CompoundTag itemData) {
        globalMarketItems.put(itemId, itemData);
        setDirty();
    }
    
    public void removeMarketItem(String itemId) {
        globalMarketItems.remove(itemId);
        setDirty();
    }
    
    public CompoundTag getMarketItem(String itemId) {
        return globalMarketItems.get(itemId);
    }
    
    public Map<String, CompoundTag> getAllMarketItems() {
        return new HashMap<>(globalMarketItems);
    }
    
    // === 玩家数据操作 ===
    
    public void setPlayerData(UUID playerId, CompoundTag data) {
        playerTradeData.put(playerId, data);
        setDirty();
    }
    
    public CompoundTag getPlayerData(UUID playerId) {
        return playerTradeData.getOrDefault(playerId, new CompoundTag());
    }
    
    // === 交易历史操作 ===
    
    public void addTradeHistory(CompoundTag tradeRecord) {
        tradeHistory.add(tradeRecord);
        
        // 限制历史记录数量
        while (tradeHistory.size() > 1000) {
            tradeHistory.remove(0);
        }
        
        // 更新统计数据
        long totalTrades = systemStats.getLong("total_trades") + 1;
        systemStats.putLong("total_trades", totalTrades);
        
        if (tradeRecord.contains("price")) {
            long totalMoney = systemStats.getLong("total_money_traded") + tradeRecord.getLong("price");
            systemStats.putLong("total_money_traded", totalMoney);
        }
        
        setDirty();
    }
    
    public List<CompoundTag> getTradeHistory() {
        return new ArrayList<>(tradeHistory);
    }
    
    // === 系统统计操作 ===
    
    public CompoundTag getSystemStats() {
        return systemStats.copy();
    }
    
    public long getTotalTrades() {
        return systemStats.getLong("total_trades");
    }
    
    public long getTotalMoneyTraded() {
        return systemStats.getLong("total_money_traded");
    }
    
    // === 数据清理操作 ===
    
    public void clearOldData() {
        long currentTime = System.currentTimeMillis();
        long oneWeekAgo = currentTime - (7 * 24 * 60 * 60 * 1000L); // 一周前
        
        // 清理旧的交易历史
        tradeHistory.removeIf(record -> {
            long timestamp = record.getLong("timestamp");
            return timestamp < oneWeekAgo;
        });
        
        setDirty();
        TradeMod.getLogger().info("清理旧数据完成，剩余历史记录: {}", tradeHistory.size());
    }
}