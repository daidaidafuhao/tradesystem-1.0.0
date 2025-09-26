package com.tradesystem.mod.data;

import com.google.gson.*;
import com.tradesystem.mod.TradeMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON数据管理器
 * 使用JSON格式持久化交易系统数据
 */
public class JsonDataManager {
    private static JsonDataManager instance;
    private final Gson gson;
    private Path dataDirectory;
    
    // 数据缓存
    private final Map<UUID, TradeItem> tradeItems = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerCurrency = new ConcurrentHashMap<>();
    private final List<TransactionRecord> transactionHistory = new ArrayList<>();
    private final Map<String, Object> systemStats = new ConcurrentHashMap<>();
    
    private JsonDataManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
                .registerTypeAdapter(UUID.class, new UUIDSerializer())
                .create();
    }
    
    public static JsonDataManager getInstance() {
        if (instance == null) {
            instance = new JsonDataManager();
        }
        return instance;
    }
    
    /**
     * 初始化数据目录
     */
    public void initialize(MinecraftServer server) {
        try {
            dataDirectory = Paths.get(server.getServerDirectory().getAbsolutePath(), "tradesystem");
            Files.createDirectories(dataDirectory);
            
            TradeMod.getLogger().info("JSON数据管理器初始化完成，数据目录: {}", dataDirectory);
            
            // 加载所有数据
            loadAllData();
            
        } catch (IOException e) {
            TradeMod.getLogger().error("初始化JSON数据管理器失败", e);
        }
    }
    
    /**
     * 加载所有数据
     */
    private void loadAllData() {
        loadTradeItems();
        loadPlayerCurrency();
        loadTransactionHistory();
        loadSystemStats();
    }
    
    /**
     * 保存所有数据
     */
    public void saveAllData() {
        saveTradeItems();
        savePlayerCurrency();
        saveTransactionHistory();
        saveSystemStats();
    }
    
    /**
     * 加载交易物品数据
     */
    private void loadTradeItems() {
        Path file = dataDirectory.resolve("trade_items.json");
        if (!Files.exists(file)) {
            TradeMod.getLogger().info("交易物品数据文件不存在，创建新文件");
            return;
        }
        
        try (FileReader reader = new FileReader(file.toFile())) {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            if (data != null) {
                for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                    try {
                        UUID id = UUID.fromString(entry.getKey());
                        TradeItem item = gson.fromJson(entry.getValue(), TradeItem.class);
                        if (item != null) {
                            tradeItems.put(id, item);
                        }
                    } catch (Exception e) {
                        TradeMod.getLogger().warn("加载交易物品数据失败: {}", entry.getKey(), e);
                    }
                }
            }
            TradeMod.getLogger().info("加载了 {} 个交易物品", tradeItems.size());
        } catch (IOException e) {
            TradeMod.getLogger().error("读取交易物品数据失败", e);
        }
    }
    
    /**
     * 保存交易物品数据
     */
    private void saveTradeItems() {
        Path file = dataDirectory.resolve("trade_items.json");
        try (FileWriter writer = new FileWriter(file.toFile())) {
            JsonObject data = new JsonObject();
            for (Map.Entry<UUID, TradeItem> entry : tradeItems.entrySet()) {
                data.add(entry.getKey().toString(), gson.toJsonTree(entry.getValue()));
            }
            gson.toJson(data, writer);
            TradeMod.getLogger().debug("保存了 {} 个交易物品", tradeItems.size());
        } catch (IOException e) {
            TradeMod.getLogger().error("保存交易物品数据失败", e);
        }
    }
    
    /**
     * 加载玩家货币数据
     */
    private void loadPlayerCurrency() {
        Path file = dataDirectory.resolve("player_currency.json");
        if (!Files.exists(file)) {
            TradeMod.getLogger().info("玩家货币数据文件不存在，创建新文件");
            return;
        }
        
        try (FileReader reader = new FileReader(file.toFile())) {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            if (data != null) {
                for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                    try {
                        UUID playerId = UUID.fromString(entry.getKey());
                        int currency = entry.getValue().getAsInt();
                        playerCurrency.put(playerId, currency);
                    } catch (Exception e) {
                        TradeMod.getLogger().warn("加载玩家货币数据失败: {}", entry.getKey(), e);
                    }
                }
            }
            TradeMod.getLogger().info("加载了 {} 个玩家的货币数据", playerCurrency.size());
        } catch (IOException e) {
            TradeMod.getLogger().error("读取玩家货币数据失败", e);
        }
    }
    
    /**
     * 保存玩家货币数据
     */
    private void savePlayerCurrency() {
        Path file = dataDirectory.resolve("player_currency.json");
        try (FileWriter writer = new FileWriter(file.toFile())) {
            JsonObject data = new JsonObject();
            for (Map.Entry<UUID, Integer> entry : playerCurrency.entrySet()) {
                data.addProperty(entry.getKey().toString(), entry.getValue());
            }
            gson.toJson(data, writer);
            TradeMod.getLogger().debug("保存了 {} 个玩家的货币数据", playerCurrency.size());
        } catch (IOException e) {
            TradeMod.getLogger().error("保存玩家货币数据失败", e);
        }
    }
    
    /**
     * 加载交易历史数据
     */
    private void loadTransactionHistory() {
        Path file = dataDirectory.resolve("transaction_history.json");
        if (!Files.exists(file)) {
            TradeMod.getLogger().info("交易历史数据文件不存在，创建新文件");
            return;
        }
        
        try (FileReader reader = new FileReader(file.toFile())) {
            TransactionRecord[] records = gson.fromJson(reader, TransactionRecord[].class);
            if (records != null) {
                transactionHistory.clear();
                Collections.addAll(transactionHistory, records);
            }
            TradeMod.getLogger().info("加载了 {} 条交易历史记录", transactionHistory.size());
        } catch (IOException e) {
            TradeMod.getLogger().error("读取交易历史数据失败", e);
        }
    }
    
    /**
     * 保存交易历史数据
     */
    private void saveTransactionHistory() {
        Path file = dataDirectory.resolve("transaction_history.json");
        try (FileWriter writer = new FileWriter(file.toFile())) {
            gson.toJson(transactionHistory, writer);
            TradeMod.getLogger().debug("保存了 {} 条交易历史记录", transactionHistory.size());
        } catch (IOException e) {
            TradeMod.getLogger().error("保存交易历史数据失败", e);
        }
    }
    
    /**
     * 加载系统统计数据
     */
    private void loadSystemStats() {
        Path file = dataDirectory.resolve("system_stats.json");
        if (!Files.exists(file)) {
            TradeMod.getLogger().info("系统统计数据文件不存在，创建新文件");
            initializeDefaultStats();
            return;
        }
        
        try (FileReader reader = new FileReader(file.toFile())) {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            if (data != null) {
                systemStats.clear();
                for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                    systemStats.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            TradeMod.getLogger().info("加载了系统统计数据");
        } catch (IOException e) {
            TradeMod.getLogger().error("读取系统统计数据失败", e);
            initializeDefaultStats();
        }
    }
    
    /**
     * 保存系统统计数据
     */
    private void saveSystemStats() {
        Path file = dataDirectory.resolve("system_stats.json");
        try (FileWriter writer = new FileWriter(file.toFile())) {
            gson.toJson(systemStats, writer);
            TradeMod.getLogger().debug("保存了系统统计数据");
        } catch (IOException e) {
            TradeMod.getLogger().error("保存系统统计数据失败", e);
        }
    }
    
    /**
     * 初始化默认统计数据
     */
    private void initializeDefaultStats() {
        systemStats.put("total_transactions", 0);
        systemStats.put("total_revenue", 0L);
        systemStats.put("active_listings", 0);
        systemStats.put("server_start_time", System.currentTimeMillis());
    }
    
    // ==================== 数据访问方法 ====================
    
    /**
     * 获取所有交易物品
     */
    public Map<UUID, TradeItem> getAllTradeItems() {
        return new HashMap<>(tradeItems);
    }
    
    /**
     * 保存交易物品
     */
    public void saveTradeItem(TradeItem item) {
        tradeItems.put(item.getId(), item);
        saveTradeItems(); // 立即保存
    }
    
    /**
     * 删除交易物品
     */
    public void removeTradeItem(UUID itemId) {
        tradeItems.remove(itemId);
        saveTradeItems(); // 立即保存
    }
    
    /**
     * 获取玩家货币
     */
    public int getPlayerCurrency(UUID playerId) {
        return playerCurrency.getOrDefault(playerId, com.tradesystem.mod.config.TradeConfig.initialPlayerMoney);
    }
    
    /**
     * 设置玩家货币
     */
    public void setPlayerCurrency(UUID playerId, int amount) {
        playerCurrency.put(playerId, amount);
        savePlayerCurrency(); // 立即保存
    }
    
    /**
     * 添加交易记录
     */
    public void addTransactionRecord(TransactionRecord record) {
        transactionHistory.add(record);
        // 限制历史记录数量，避免文件过大
        if (transactionHistory.size() > 10000) {
            transactionHistory.remove(0);
        }
        saveTransactionHistory(); // 立即保存
    }
    
    /**
     * 获取交易历史
     */
    public List<TransactionRecord> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }
    
    /**
     * 更新系统统计
     */
    public void updateSystemStat(String key, Object value) {
        systemStats.put(key, value);
        saveSystemStats(); // 立即保存
    }
    
    /**
     * 获取系统统计
     */
    public Object getSystemStat(String key) {
        return systemStats.get(key);
    }
    
    /**
     * 定期保存数据（可以由定时任务调用）
     */
    public void periodicSave() {
        saveAllData();
        TradeMod.getLogger().debug("执行定期数据保存");
    }
    
    /**
     * 服务器关闭时的清理工作
     */
    public void shutdown() {
        saveAllData();
        TradeMod.getLogger().info("JSON数据管理器已保存所有数据并关闭");
    }
}