package com.tradesystem.mod.manager;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.capability.ModCapabilities;
import com.tradesystem.mod.config.TradeConfig;
import com.tradesystem.mod.data.TradeDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统回收管理器
 * 负责处理物品的系统回收、价格计算等功能
 */
public class RecycleManager {
    private static RecycleManager instance;
    private final Map<String, Integer> baseRecyclePrices = new ConcurrentHashMap<>();
    private final Set<String> recyclableItems = new HashSet<>();
    
    private RecycleManager() {
        initializeRecyclePrices();
    }
    
    public static RecycleManager getInstance() {
        if (instance == null) {
            instance = new RecycleManager();
        }
        return instance;
    }
    
    /**
     * 初始化回收价格表
     */
    private void initializeRecyclePrices() {
        // 基础材料
        baseRecyclePrices.put("minecraft:diamond", 100);
        baseRecyclePrices.put("minecraft:emerald", 80);
        baseRecyclePrices.put("minecraft:gold_ingot", 50);
        baseRecyclePrices.put("minecraft:iron_ingot", 20);
        baseRecyclePrices.put("minecraft:copper_ingot", 10);
        baseRecyclePrices.put("minecraft:coal", 5);
        baseRecyclePrices.put("minecraft:redstone", 8);
        baseRecyclePrices.put("minecraft:lapis_lazuli", 12);
        
        // 稀有材料
        baseRecyclePrices.put("minecraft:netherite_ingot", 500);
        baseRecyclePrices.put("minecraft:ancient_debris", 400);
        baseRecyclePrices.put("minecraft:netherite_scrap", 100);
        baseRecyclePrices.put("minecraft:nether_star", 1000);
        baseRecyclePrices.put("minecraft:dragon_egg", 5000);
        baseRecyclePrices.put("minecraft:elytra", 2000);
        
        // 工具和武器
        baseRecyclePrices.put("minecraft:diamond_sword", 200);
        baseRecyclePrices.put("minecraft:diamond_pickaxe", 300);
        baseRecyclePrices.put("minecraft:diamond_axe", 300);
        baseRecyclePrices.put("minecraft:diamond_shovel", 100);
        baseRecyclePrices.put("minecraft:diamond_hoe", 200);
        
        baseRecyclePrices.put("minecraft:netherite_sword", 600);
        baseRecyclePrices.put("minecraft:netherite_pickaxe", 700);
        baseRecyclePrices.put("minecraft:netherite_axe", 700);
        baseRecyclePrices.put("minecraft:netherite_shovel", 500);
        baseRecyclePrices.put("minecraft:netherite_hoe", 600);
        
        // 装备
        baseRecyclePrices.put("minecraft:diamond_helmet", 500);
        baseRecyclePrices.put("minecraft:diamond_chestplate", 800);
        baseRecyclePrices.put("minecraft:diamond_leggings", 700);
        baseRecyclePrices.put("minecraft:diamond_boots", 400);
        
        baseRecyclePrices.put("minecraft:netherite_helmet", 1000);
        baseRecyclePrices.put("minecraft:netherite_chestplate", 1300);
        baseRecyclePrices.put("minecraft:netherite_leggings", 1200);
        baseRecyclePrices.put("minecraft:netherite_boots", 900);
        
        // 食物
        baseRecyclePrices.put("minecraft:golden_apple", 80);
        baseRecyclePrices.put("minecraft:enchanted_golden_apple", 1000);
        baseRecyclePrices.put("minecraft:cooked_beef", 3);
        baseRecyclePrices.put("minecraft:bread", 2);
        
        // 方块
        baseRecyclePrices.put("minecraft:diamond_block", 900);
        baseRecyclePrices.put("minecraft:emerald_block", 720);
        baseRecyclePrices.put("minecraft:gold_block", 450);
        baseRecyclePrices.put("minecraft:iron_block", 180);
        baseRecyclePrices.put("minecraft:netherite_block", 4500);
        
        // 药水和附魔书
        baseRecyclePrices.put("minecraft:experience_bottle", 15);
        baseRecyclePrices.put("minecraft:enchanted_book", 50);
        
        // 将所有有价格的物品添加到可回收列表
        recyclableItems.addAll(baseRecyclePrices.keySet());
        
        TradeMod.getLogger().info("初始化了 {} 种可回收物品", recyclableItems.size());
    }
    
    /**
     * 回收物品
     */
    public boolean recycleItem(ServerPlayer player, ItemStack itemStack) {
        try {
            if (player == null || itemStack.isEmpty()) {
                return false;
            }
            
            // 检查物品是否可回收
            if (!isItemRecyclable(itemStack)) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_not_recyclable"));
                return false;
            }
            
            // 计算回收价格
            int recyclePrice = calculateRecyclePrice(itemStack);
            if (recyclePrice <= 0) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.no_recycle_value"));
                return false;
            }
            
            // 从玩家背包中移除物品
            if (!removeItemFromInventory(player, itemStack)) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_not_found_inventory"));
                return false;
            }
            
            // 给玩家货币
            if (!addCurrencyToPlayer(player, recyclePrice)) {
                // 如果添加货币失败，返还物品
                giveItemToPlayer(player, itemStack);
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.currency_add_failed"));
                return false;
            }
            
            // 记录回收历史
            recordRecycleTransaction(player, itemStack, recyclePrice);
            
            // 发送成功消息
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.recycle_success",
                    itemStack.getHoverName(), itemStack.getCount(), recyclePrice));
            
            TradeMod.getLogger().info("玩家 {} 回收物品: {} x{} 获得: {} 货币",
                    player.getName().getString(), itemStack.getHoverName().getString(),
                    itemStack.getCount(), recyclePrice);
            
            return true;
            
        } catch (Exception e) {
            TradeMod.getLogger().error("回收物品时发生错误: {}", e.getMessage());
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.recycle_error"));
            return false;
        }
    }
    
    /**
     * 批量回收物品
     */
    public boolean recycleMultipleItems(ServerPlayer player, List<ItemStack> itemStacks) {
        if (player == null || itemStacks == null || itemStacks.isEmpty()) {
            return false;
        }
        
        int totalValue = 0;
        List<ItemStack> recyclableItems = new ArrayList<>();
        
        // 检查所有物品并计算总价值
        for (ItemStack itemStack : itemStacks) {
            if (isItemRecyclable(itemStack)) {
                int price = calculateRecyclePrice(itemStack);
                if (price > 0) {
                    totalValue += price;
                    recyclableItems.add(itemStack.copy());
                }
            }
        }
        
        if (recyclableItems.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.no_recyclable_items"));
            return false;
        }
        
        // 移除所有可回收物品
        int recycledCount = 0;
        int actualValue = 0;
        
        for (ItemStack itemStack : recyclableItems) {
            if (removeItemFromInventory(player, itemStack)) {
                actualValue += calculateRecyclePrice(itemStack);
                recycledCount++;
                recordRecycleTransaction(player, itemStack, calculateRecyclePrice(itemStack));
            }
        }
        
        if (recycledCount > 0) {
            // 给玩家货币
            addCurrencyToPlayer(player, actualValue);
            
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.batch_recycle_success",
                    recycledCount, actualValue));
            
            TradeMod.getLogger().info("玩家 {} 批量回收了 {} 个物品，获得 {} 货币",
                    player.getName().getString(), recycledCount, actualValue);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 计算物品回收价格
     */
    public int calculateRecyclePrice(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return 0;
        }
        
        String itemName = itemStack.getItem().toString();
        int basePrice = baseRecyclePrices.getOrDefault(itemName, 0);
        
        if (basePrice <= 0) {
            return 0;
        }
        
        // 基础价格乘以数量
        int totalPrice = basePrice * itemStack.getCount();
        
        // 考虑耐久度
        totalPrice = applyDurabilityModifier(itemStack, totalPrice);
        
        // 考虑附魔
        totalPrice = applyEnchantmentModifier(itemStack, totalPrice);
        
        // 应用回收率
        double recycleRate = TradeConfig.RECYCLE_RATE.get();
        totalPrice = (int) (totalPrice * recycleRate);
        
        return Math.max(1, totalPrice); // 至少值1货币
    }
    
    /**
     * 应用耐久度修正
     */
    private int applyDurabilityModifier(ItemStack itemStack, int basePrice) {
        if (!itemStack.isDamageableItem()) {
            return basePrice;
        }
        
        int maxDamage = itemStack.getMaxDamage();
        int currentDamage = itemStack.getDamageValue();
        
        if (maxDamage <= 0) {
            return basePrice;
        }
        
        double durabilityRatio = (double) (maxDamage - currentDamage) / maxDamage;
        return (int) (basePrice * durabilityRatio);
    }
    
    /**
     * 应用附魔修正
     */
    private int applyEnchantmentModifier(ItemStack itemStack, int basePrice) {
        if (!itemStack.isEnchanted()) {
            return basePrice;
        }
        
        // 计算附魔价值
        int enchantmentValue = EnchantmentHelper.getEnchantments(itemStack).entrySet().stream()
                .mapToInt(entry -> entry.getKey().getMaxLevel() * entry.getValue() * 10)
                .sum();
        
        return basePrice + enchantmentValue;
    }
    
    /**
     * 检查物品是否可回收
     */
    public boolean isItemRecyclable(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        
        String itemName = itemStack.getItem().toString();
        
        // 检查是否在可回收列表中
        if (!recyclableItems.contains(itemName)) {
            return false;
        }
        
        // 检查是否在黑名单中
        if (TradeConfig.getRecycleBlacklist().contains(itemName)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取物品的回收价格预览
     */
    public int getRecyclePricePreview(ItemStack itemStack) {
        return calculateRecyclePrice(itemStack);
    }
    
    /**
     * 从玩家背包中移除物品
     */
    private boolean removeItemFromInventory(ServerPlayer player, ItemStack itemStack) {
        // 检查玩家是否有足够的物品
        if (!hasEnoughItems(player, itemStack)) {
            return false;
        }
        
        // 移除物品
        int remainingCount = itemStack.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingCount > 0; i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(slotStack, itemStack)) {
                int removeCount = Math.min(remainingCount, slotStack.getCount());
                slotStack.shrink(removeCount);
                remainingCount -= removeCount;
            }
        }
        
        return remainingCount == 0;
    }
    
    /**
     * 检查玩家是否有足够的物品
     */
    private boolean hasEnoughItems(ServerPlayer player, ItemStack required) {
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, required)) {
                totalCount += stack.getCount();
                if (totalCount >= required.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 给玩家添加货币
     */
    private boolean addCurrencyToPlayer(ServerPlayer player, int amount) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY).map(cap -> {
            return cap.addMoney(amount);
        }).orElse(false);
    }
    
    /**
     * 给玩家物品
     */
    private boolean giveItemToPlayer(ServerPlayer player, ItemStack itemStack) {
        return player.getInventory().add(itemStack);
    }
    
    /**
     * 记录回收交易
     */
    private void recordRecycleTransaction(ServerPlayer player, ItemStack itemStack, int price) {
        TradeDataManager.getInstance().recordRecycleTransaction(
                player.getUUID(),
                player.getName().getString(),
                itemStack,
                price,
                System.currentTimeMillis()
        );
    }
    
    /**
     * 添加自定义回收价格
     */
    public void addCustomRecyclePrice(String itemName, int price) {
        baseRecyclePrices.put(itemName, price);
        recyclableItems.add(itemName);
        TradeMod.getLogger().info("添加自定义回收价格: {} = {}", itemName, price);
    }
    
    /**
     * 移除回收价格
     */
    public void removeRecyclePrice(String itemName) {
        baseRecyclePrices.remove(itemName);
        recyclableItems.remove(itemName);
        TradeMod.getLogger().info("移除回收价格: {}", itemName);
    }
    
    /**
     * 获取所有可回收物品
     */
    public Set<String> getRecyclableItems() {
        return new HashSet<>(recyclableItems);
    }
    
    /**
     * 获取物品的基础回收价格
     */
    public int getBaseRecyclePrice(String itemName) {
        return baseRecyclePrices.getOrDefault(itemName, 0);
    }
    
    /**
     * 重新加载回收配置
     */
    public void reloadConfig() {
        baseRecyclePrices.clear();
        recyclableItems.clear();
        initializeRecyclePrices();
        TradeMod.getLogger().info("重新加载回收配置");
    }
}