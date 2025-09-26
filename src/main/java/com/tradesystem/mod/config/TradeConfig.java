package com.tradesystem.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Arrays;
import java.util.List;

/**
 * 交易系统配置类
 * 管理MOD的各种配置选项
 */
@Mod.EventBusSubscriber(modid = "tradesystem", bus = Mod.EventBusSubscriber.Bus.MOD)
public class TradeConfig {
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    // 交易系统基础配置
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_TRADE_SYSTEM;
    public static final ForgeConfigSpec.IntValue MAX_TRADE_ITEMS_PER_PLAYER;
    public static final ForgeConfigSpec.IntValue MAX_TRADE_PRICE;
    public static final ForgeConfigSpec.DoubleValue TRADE_TAX_RATE;
    
    // 货币系统配置
    public static final ForgeConfigSpec.IntValue INITIAL_PLAYER_MONEY;
    public static final ForgeConfigSpec.IntValue MAX_PLAYER_MONEY;
    public static final ForgeConfigSpec.ConfigValue<String> CURRENCY_NAME;
    
    // 数据存储配置
    public static final ForgeConfigSpec.IntValue DATA_SAVE_INTERVAL;
    public static final ForgeConfigSpec.IntValue MAX_TRADE_HISTORY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DATA_COMPRESSION;
    
    // 网络配置
    public static final ForgeConfigSpec.IntValue NETWORK_PACKET_SIZE_LIMIT;
    public static final ForgeConfigSpec.IntValue NETWORK_TIMEOUT;
    
    // 回收系统配置
    public static final ForgeConfigSpec.DoubleValue RECYCLE_RATE;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> RECYCLE_BLACKLIST;
    
    // 交易税费配置
    public static final ForgeConfigSpec.DoubleValue TRANSACTION_TAX_RATE;
    
    // 物品过期时间配置
    public static final ForgeConfigSpec.IntValue ITEM_EXPIRY_TIME;
    
    // 玩家最大上架数量配置
    public static final ForgeConfigSpec.IntValue MAX_LISTINGS_PER_PLAYER;
    
    // 物品黑名单配置
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BLACKLISTED_ITEMS;
    
    static {
        BUILDER.comment("交易系统基础配置").push("trade_system");
        
        ENABLE_TRADE_SYSTEM = BUILDER
                .comment("是否启用交易系统")
                .define("enable_trade_system", true);
        
        MAX_TRADE_ITEMS_PER_PLAYER = BUILDER
                .comment("每个玩家最大可交易物品数量")
                .defineInRange("max_trade_items_per_player", 50, 1, 1000);
        
        MAX_TRADE_PRICE = BUILDER
                .comment("单个物品最大交易价格")
                .defineInRange("max_trade_price", 1000000, 1, Integer.MAX_VALUE);
        
        TRADE_TAX_RATE = BUILDER
                .comment("交易税率 (0.0-1.0)")
                .defineInRange("trade_tax_rate", 0.05, 0.0, 1.0);
        
        BUILDER.pop();
        
        BUILDER.comment("货币系统配置").push("currency");
        
        INITIAL_PLAYER_MONEY = BUILDER
                .comment("玩家初始金币数量")
                .defineInRange("initial_player_money", 1000, 0, Integer.MAX_VALUE);
        
        MAX_PLAYER_MONEY = BUILDER
                .comment("玩家最大金币数量")
                .defineInRange("max_player_money", 10000000, 1, Integer.MAX_VALUE);
        
        CURRENCY_NAME = BUILDER
                .comment("货币名称")
                .define("currency_name", "金币");
        
        BUILDER.pop();
        
        BUILDER.comment("数据存储配置").push("data_storage");
        
        DATA_SAVE_INTERVAL = BUILDER
                .comment("数据保存间隔 (秒)")
                .defineInRange("data_save_interval", 300, 60, 3600);
        
        MAX_TRADE_HISTORY = BUILDER
                .comment("最大交易历史记录数量")
                .defineInRange("max_trade_history", 1000, 100, 10000);
        
        ENABLE_DATA_COMPRESSION = BUILDER
                .comment("是否启用数据压缩")
                .define("enable_data_compression", true);
        
        BUILDER.pop();
        
        BUILDER.comment("网络配置").push("network");
        
        NETWORK_PACKET_SIZE_LIMIT = BUILDER
                .comment("网络数据包大小限制 (字节)")
                .defineInRange("network_packet_size_limit", 30720, 1024, 65536);
        
        NETWORK_TIMEOUT = BUILDER
                .comment("网络超时时间 (毫秒)")
                .defineInRange("network_timeout", 5000, 1000, 30000);
        
        BUILDER.pop();
        
        BUILDER.comment("回收系统配置").push("recycle");
        
        RECYCLE_RATE = BUILDER
                .comment("系统回收价格倍率 (0.0-1.0)")
                .defineInRange("recycle_rate", 0.5, 0.0, 1.0);
        
        RECYCLE_BLACKLIST = BUILDER
                .comment("不可回收物品黑名单")
                .defineList("recycle_blacklist", 
                    java.util.Arrays.asList("minecraft:bedrock", "minecraft:barrier", "minecraft:command_block"),
                    obj -> obj instanceof String);
        
        BUILDER.pop();
        
        BUILDER.comment("交易税费配置").push("transaction");
        
        TRANSACTION_TAX_RATE = BUILDER
                 .comment("交易税费比例 (0.0-1.0)")
                 .defineInRange("transaction_tax_rate", 0.05, 0.0, 1.0);
         
         BUILDER.pop();
         
         BUILDER.comment("物品管理配置").push("items");
         
         ITEM_EXPIRY_TIME = BUILDER
                 .comment("物品过期时间（天）")
                 .defineInRange("expiry_time", 7, 1, 365);
         
         MAX_LISTINGS_PER_PLAYER = BUILDER
                 .comment("每个玩家最大上架数量")
                 .defineInRange("max_listings_per_player", 10, 1, 100);
         
         BLACKLISTED_ITEMS = BUILDER
                 .comment("禁止交易的物品列表")
                 .defineList("blacklisted_items", Arrays.asList(
                     "minecraft:bedrock",
                     "minecraft:barrier",
                     "minecraft:command_block"
                 ), obj -> obj instanceof String);
         
         BUILDER.pop();
    }
    
    public static final ForgeConfigSpec SPEC = BUILDER.build();
    
    // 配置值缓存
    public static boolean enableTradeSystem;
    public static int maxTradeItemsPerPlayer;
    public static int maxTradePrice;
    public static double tradeTaxRate;
    public static int initialPlayerMoney;
    public static int maxPlayerMoney;
    public static String currencyName;
    public static int dataSaveInterval;
    public static int maxTradeHistory;
    public static boolean enableDataCompression;
    public static int networkPacketSizeLimit;
    public static int networkTimeout;
    public static double recycleRate;
    public static java.util.List<? extends String> recycleBlacklist;
    public static double transactionTaxRate;
     public static int itemExpiryTime;
     public static int maxListingsPerPlayer;
     public static java.util.List<? extends String> blacklistedItems;
    
    /**
     * 配置加载事件
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 缓存配置值以提高性能
        enableTradeSystem = ENABLE_TRADE_SYSTEM.get();
        maxTradeItemsPerPlayer = MAX_TRADE_ITEMS_PER_PLAYER.get();
        maxTradePrice = MAX_TRADE_PRICE.get();
        tradeTaxRate = TRADE_TAX_RATE.get();
        initialPlayerMoney = INITIAL_PLAYER_MONEY.get();
        maxPlayerMoney = MAX_PLAYER_MONEY.get();
        currencyName = CURRENCY_NAME.get();
        dataSaveInterval = DATA_SAVE_INTERVAL.get();
        maxTradeHistory = MAX_TRADE_HISTORY.get();
        enableDataCompression = ENABLE_DATA_COMPRESSION.get();
        networkPacketSizeLimit = NETWORK_PACKET_SIZE_LIMIT.get();
        networkTimeout = NETWORK_TIMEOUT.get();
        recycleRate = RECYCLE_RATE.get();
        recycleBlacklist = RECYCLE_BLACKLIST.get();
        transactionTaxRate = TRANSACTION_TAX_RATE.get();
         itemExpiryTime = ITEM_EXPIRY_TIME.get();
         maxListingsPerPlayer = MAX_LISTINGS_PER_PLAYER.get();
         blacklistedItems = BLACKLISTED_ITEMS.get();
    }
    
    /**
      * 获取回收黑名单
      */
     public static java.util.List<? extends String> getRecycleBlacklist() {
         return recycleBlacklist;
     }
     
     /**
      * 获取每个玩家最大上架数量
      */
     public static int getMaxListingsPerPlayer() {
         return maxListingsPerPlayer;
     }
     
     /**
      * 获取禁止交易的物品列表
      */
     public static java.util.List<? extends String> getBlacklistedItems() {
         return blacklistedItems;
     }
}