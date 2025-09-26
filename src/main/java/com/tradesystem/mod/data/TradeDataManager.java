package com.tradesystem.mod.data;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.config.TradeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 交易数据管理器
 * 负责管理所有交易相关的数据存储和同步
 */
public class TradeDataManager {
    
    private static TradeDataManager instance;
    private final MinecraftServer server;
    private final ScheduledExecutorService scheduler;
    
    // 数据存储映射
    private final Map<String, SavedData> dataMap = new HashMap<>();
    
    // 玩家交易数据缓存
    private final Map<UUID, Object> playerTradeCache = new HashMap<>();
    
    // 离线玩家货币缓存
    private final Map<UUID, Integer> offlinePlayerCurrency = new HashMap<>();
    
    // 离线玩家物品缓存
    private final Map<UUID, java.util.List<net.minecraft.world.item.ItemStack>> offlinePlayerItems = new HashMap<>();
    
    // 系统收入统计
    private long systemRevenue = 0;
    
    public TradeDataManager(MinecraftServer server) {
        this.server = server;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "TradeSystem-DataManager");
            thread.setDaemon(true);
            return thread;
        });
        
        // 启动定期保存任务
        startPeriodicSave();
        
        // 设置单例实例
        instance = this;
        
        TradeMod.getLogger().info("交易数据管理器初始化完成");
    }
    
    /**
     * 获取单例实例
     */
    public static TradeDataManager getInstance() {
        return instance;
    }
    
    /**
     * 启动定期保存任务
     */
    private void startPeriodicSave() {
        int saveInterval = TradeConfig.dataSaveInterval;
        scheduler.scheduleAtFixedRate(this::saveAllData, saveInterval, saveInterval, TimeUnit.SECONDS);
    }
    
    /**
     * 保存所有数据
     */
    public void saveAllData() {
        executeOnMainThread(() -> {
            try {
                synchronized (dataMap) {
                    for (SavedData data : dataMap.values()) {
                        data.setDirty();
                    }
                }
                TradeMod.getLogger().debug("交易数据保存完成");
            } catch (Exception e) {
                TradeMod.getLogger().error("保存交易数据时发生错误", e);
            }
        });
    }
    
    /**
     * 获取数据存储
     */
    public DimensionDataStorage getDataStorage() {
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        return overworld != null ? overworld.getDataStorage() : null;
    }
    
    /**
     * 在主线程执行任务
     */
    public void executeOnMainThread(Runnable task) {
        if (server != null) {
            server.execute(task);
        }
    }
    
    /**
     * 获取玩家交易数据缓存
     */
    public Map<UUID, Object> getPlayerTradeCache() {
        return playerTradeCache;
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        synchronized (playerTradeCache) {
            playerTradeCache.clear();
        }
    }
    
    /**
     * 关闭数据管理器
     */
    public void shutdown() {
        // 保存所有数据
        saveAllData();
        
        // 关闭调度器
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        TradeMod.getLogger().info("交易数据管理器已关闭");
    }
    
    /**
     * 添加离线玩家货币
     */
    public void addOfflinePlayerCurrency(UUID playerId, int amount) {
        synchronized (offlinePlayerCurrency) {
            offlinePlayerCurrency.merge(playerId, amount, Integer::sum);
        }
    }
    
    /**
     * 获取离线玩家货币
     */
    public int getOfflinePlayerCurrency(UUID playerId) {
        synchronized (offlinePlayerCurrency) {
            return offlinePlayerCurrency.getOrDefault(playerId, 0);
        }
    }
    
    /**
     * 清除离线玩家货币
     */
    public void clearOfflinePlayerCurrency(UUID playerId) {
        synchronized (offlinePlayerCurrency) {
            offlinePlayerCurrency.remove(playerId);
        }
    }
    
    /**
     * 添加离线玩家物品
     */
    public void addOfflinePlayerItem(UUID playerId, net.minecraft.world.item.ItemStack itemStack) {
        synchronized (offlinePlayerItems) {
            offlinePlayerItems.computeIfAbsent(playerId, k -> new java.util.ArrayList<>()).add(itemStack);
        }
    }
    
    /**
     * 获取离线玩家物品
     */
    public java.util.List<net.minecraft.world.item.ItemStack> getOfflinePlayerItems(UUID playerId) {
        synchronized (offlinePlayerItems) {
            return new java.util.ArrayList<>(offlinePlayerItems.getOrDefault(playerId, new java.util.ArrayList<>()));
        }
    }
    
    /**
     * 清除离线玩家物品
     */
    public void clearOfflinePlayerItems(UUID playerId) {
        synchronized (offlinePlayerItems) {
            offlinePlayerItems.remove(playerId);
        }
    }
    
    /**
     * 添加系统收入
     */
    public void addSystemRevenue(long amount) {
        this.systemRevenue += amount;
    }
    
    /**
     * 获取系统收入
     */
    public long getSystemRevenue() {
        return systemRevenue;
    }
    
    /**
     * 记录交易历史
     */
    public void recordTransaction(UUID buyerId, String buyerName, UUID sellerId, String sellerName, 
                                 net.minecraft.world.item.ItemStack itemStack, int price, long timestamp) {
        // 这里可以实现交易历史记录逻辑
        TradeMod.getLogger().info("记录交易: {} 购买了 {} 的物品，价格: {}", buyerName, sellerName, price);
    }
    
    /**
     * 记录回收交易
     */
    public void recordRecycleTransaction(UUID playerId, String playerName, 
                                       net.minecraft.world.item.ItemStack itemStack, int value, long timestamp) {
        TradeMod.getLogger().info("记录回收交易: {} 回收了物品，获得: {}", playerName, value);
    }
    
    /**
     * 获取所有交易物品
     */
    public java.util.List<com.tradesystem.mod.data.TradeItem> getAllTradeItems() {
        java.util.List<com.tradesystem.mod.data.TradeItem> items = new java.util.ArrayList<>();
        
        // 从数据服务获取数据
        DataService dataService = DataService.getInstance();
        if (dataService != null) {
            // 获取所有市场物品
            var marketItems = dataService.getAllMarketItems();
            for (var entry : marketItems.entrySet()) {
                try {
                    // 从NBT数据重建TradeItem对象
                    com.tradesystem.mod.data.TradeItem item = new com.tradesystem.mod.data.TradeItem(entry.getValue());
                    if (item != null && item.isActive()) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    TradeMod.getLogger().error("加载交易物品时出错: {}", e.getMessage());
                }
            }
        }
        
        TradeMod.getLogger().info("从数据存储加载了 {} 个交易物品", items.size());
        return items;
    }
    
    /**
     * 保存交易物品
     */
    public void saveTradeItem(com.tradesystem.mod.data.TradeItem item) {
        if (item == null) {
            return;
        }
        
        // 保存到数据服务
        DataService dataService = DataService.getInstance();
        if (dataService != null) {
            try {
                // 将TradeItem转换为NBT数据
                net.minecraft.nbt.CompoundTag itemData = item.toNBT();
                dataService.addMarketItem(item.getId().toString(), itemData);
                TradeMod.getLogger().debug("保存交易物品: {} - {}", item.getId(), item.getItemStack().getHoverName().getString());
            } catch (Exception e) {
                TradeMod.getLogger().error("保存交易物品时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 移除交易物品
     */
    public void removeTradeItem(UUID itemId) {
        // 从数据存储中移除交易物品
        TradeMod.getLogger().debug("移除交易物品: {}", itemId);
    }
}