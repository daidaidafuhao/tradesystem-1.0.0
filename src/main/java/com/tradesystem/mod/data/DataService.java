package com.tradesystem.mod.data;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.packet.DataSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据服务类
 * 提供高级的数据操作接口，管理缓存和同步
 */
public class DataService {
    
    private static DataService instance;
    private MinecraftServer server;
    private TradeSavedData savedData;
    
    // 缓存数据
    private final Map<UUID, CompoundTag> playerDataCache = new ConcurrentHashMap<>();
    private final Map<String, CompoundTag> marketItemsCache = new ConcurrentHashMap<>();
    private volatile CompoundTag systemStatsCache = new CompoundTag();
    
    // 缓存更新标记
    private volatile boolean marketCacheDirty = true;
    private volatile boolean statsCacheDirty = true;
    
    private DataService() {}
    
    /**
     * 获取数据服务实例
     */
    public static DataService getInstance() {
        if (instance == null) {
            synchronized (DataService.class) {
                if (instance == null) {
                    instance = new DataService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化数据服务
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        this.savedData = TradeSavedData.get(server);
        
        // 初始化缓存
        refreshAllCaches();
        
        TradeMod.getLogger().info("数据服务初始化完成");
    }
    
    /**
     * 刷新所有缓存
     */
    public void refreshAllCaches() {
        refreshMarketCache();
        refreshStatsCache();
        TradeMod.getLogger().debug("所有缓存已刷新");
    }
    
    /**
     * 刷新市场缓存
     */
    private void refreshMarketCache() {
        if (savedData != null) {
            marketItemsCache.clear();
            
            // 直接从持久化存储获取市场物品数据，避免循环依赖
            Map<String, CompoundTag> storedItems = savedData.getAllMarketItems();
            for (Map.Entry<String, CompoundTag> entry : storedItems.entrySet()) {
                marketItemsCache.put(entry.getKey(), entry.getValue().copy());
            }
            
            marketCacheDirty = false;
            TradeMod.getLogger().debug("市场缓存已刷新，共 {} 个物品", marketItemsCache.size());
        }
    }
    
    /**
     * 刷新统计缓存
     */
    private void refreshStatsCache() {
        if (savedData != null) {
            systemStatsCache = savedData.getSystemStats();
            statsCacheDirty = false;
        }
    }
    
    // === 玩家数据操作 ===
    
    /**
     * 获取玩家数据
     */
    public CompoundTag getPlayerData(UUID playerId) {
        // 先从缓存获取
        CompoundTag cached = playerDataCache.get(playerId);
        if (cached != null) {
            return cached.copy();
        }
        
        // 从存储获取并缓存
        if (savedData != null) {
            CompoundTag data = savedData.getPlayerData(playerId);
            playerDataCache.put(playerId, data.copy());
            return data;
        }
        
        return new CompoundTag();
    }
    
    /**
     * 设置玩家数据
     */
    public void setPlayerData(UUID playerId, CompoundTag data) {
        if (savedData != null) {
            savedData.setPlayerData(playerId, data);
            playerDataCache.put(playerId, data.copy());
            
            // 同步到客户端
            syncPlayerDataToClient(playerId, data);
        }
    }
    
    /**
     * 清除玩家数据缓存
     */
    public void clearPlayerDataCache(UUID playerId) {
        playerDataCache.remove(playerId);
    }
    
    // === 市场数据操作 ===
    
    /**
     * 获取所有市场物品
     */
    public Map<String, CompoundTag> getAllMarketItems() {
        if (marketCacheDirty) {
            refreshMarketCache();
        }
        return new HashMap<>(marketItemsCache);
    }
    
    /**
     * 添加市场物品
     */
    public void addMarketItem(String itemId, CompoundTag itemData) {
        if (savedData != null) {
            savedData.addMarketItem(itemId, itemData);
            marketItemsCache.put(itemId, itemData.copy());
            
            // 同步到所有客户端
            syncMarketDataToAllClients();
        }
    }
    
    /**
     * 移除市场物品
     */
    public void removeMarketItem(String itemId) {
        if (savedData != null) {
            savedData.removeMarketItem(itemId);
            marketItemsCache.remove(itemId);
            
            // 同步到所有客户端
            syncMarketDataToAllClients();
        }
    }
    
    /**
     * 获取市场物品
     */
    public CompoundTag getMarketItem(String itemId) {
        if (marketCacheDirty) {
            refreshMarketCache();
        }
        CompoundTag item = marketItemsCache.get(itemId);
        return item != null ? item.copy() : null;
    }
    
    // === 交易历史操作 ===
    
    /**
     * 添加交易记录
     */
    public void addTradeRecord(UUID buyerId, UUID sellerId, String itemId, long price) {
        if (savedData != null) {
            CompoundTag record = new CompoundTag();
            record.putString("buyer_id", buyerId.toString());
            record.putString("seller_id", sellerId.toString());
            record.putString("item_id", itemId);
            record.putLong("price", price);
            record.putLong("timestamp", System.currentTimeMillis());
            
            savedData.addTradeHistory(record);
            statsCacheDirty = true; // 标记统计缓存需要更新
            
            TradeMod.getLogger().info("添加交易记录: {} -> {}, 物品: {}, 价格: {}", 
                    sellerId, buyerId, itemId, price);
        }
    }
    
    /**
     * 获取交易历史
     */
    public List<CompoundTag> getTradeHistory() {
        return savedData != null ? savedData.getTradeHistory() : new ArrayList<>();
    }
    
    // === 统计数据操作 ===
    
    /**
     * 获取系统统计
     */
    public CompoundTag getSystemStats() {
        if (statsCacheDirty) {
            refreshStatsCache();
        }
        return systemStatsCache.copy();
    }
    
    /**
     * 获取总交易数
     */
    public long getTotalTrades() {
        return savedData != null ? savedData.getTotalTrades() : 0;
    }
    
    /**
     * 获取总交易金额
     */
    public long getTotalMoneyTraded() {
        return savedData != null ? savedData.getTotalMoneyTraded() : 0;
    }
    
    // === 数据同步方法 ===
    
    /**
     * 同步玩家数据到客户端
     */
    private void syncPlayerDataToClient(UUID playerId, CompoundTag data) {
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                DataSyncPacket packet = new DataSyncPacket(DataSyncPacket.DataType.PLAYER_CURRENCY, data);
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
    
    /**
     * 同步市场数据到所有客户端
     */
    public void syncMarketDataToAllClients() {
        if (server != null) {
            // 先刷新市场缓存以确保数据是最新的
            refreshMarketCache();
            
            CompoundTag marketData = new CompoundTag();
            for (Map.Entry<String, CompoundTag> entry : marketItemsCache.entrySet()) {
                marketData.put(entry.getKey(), entry.getValue());
            }
            
            TradeMod.getLogger().info("同步市场数据到所有客户端，共 {} 个物品", marketItemsCache.size());
            
            DataSyncPacket packet = new DataSyncPacket(DataSyncPacket.DataType.MARKET_ITEMS, marketData);
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
        }
    }
    
    /**
     * 同步市场数据到指定玩家
     */
    public void syncMarketDataToPlayer(ServerPlayer player) {
        if (server != null) {
            CompoundTag marketData = new CompoundTag();
            for (Map.Entry<String, CompoundTag> entry : marketItemsCache.entrySet()) {
                marketData.put(entry.getKey(), entry.getValue());
            }
            
            DataSyncPacket packet = new DataSyncPacket(DataSyncPacket.DataType.MARKET_ITEMS, marketData);
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
            TradeMod.getLogger().debug("同步市场数据到玩家: {} (共 {} 个物品)", 
                    player.getName().getString(), marketItemsCache.size());
        }
    }
    
    /**
     * 同步统计数据到客户端
     */
    public void syncStatsToClient(ServerPlayer player) {
        if (server != null) {
            CompoundTag stats = getSystemStats();
            DataSyncPacket packet = new DataSyncPacket(DataSyncPacket.DataType.SYSTEM_STATS, stats);
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
    
    // === 维护方法 ===
    
    /**
     * 清理旧数据
     */
    public void cleanupOldData() {
        if (savedData != null) {
            savedData.clearOldData();
            refreshAllCaches();
        }
    }
    
    /**
     * 强制保存数据
     */
    public void forceSave() {
        if (savedData != null) {
            savedData.setDirty();
            TradeMod.getLogger().info("强制保存交易数据");
        }
    }
    
    /**
     * 关闭数据服务
     */
    public void shutdown() {
        playerDataCache.clear();
        marketItemsCache.clear();
        systemStatsCache = new CompoundTag();
        
        if (savedData != null) {
            savedData.setDirty();
        }
        
        TradeMod.getLogger().info("数据服务已关闭");
    }
}