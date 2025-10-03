package com.tradesystem.mod.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 交易管理器
 * 负责管理所有交易相关的操作
 */
public class TradeManager {
    private static TradeManager instance;
    
    // 存储所有活跃的交易项目
    private final Map<UUID, TradeItem> activeTradeItems = new ConcurrentHashMap<>();
    
    // 存储交易历史记录
    private final Map<UUID, List<TransactionRecord>> playerTransactionHistory = new ConcurrentHashMap<>();
    
    private TradeManager() {}
    
    public static TradeManager getInstance() {
        if (instance == null) {
            instance = new TradeManager();
        }
        return instance;
    }
    
    /**
     * 下架物品
     */
    public boolean unlistItem(UUID tradeItemId, ServerPlayer player) {
        try {
            TradeItem tradeItem = activeTradeItems.get(tradeItemId);
            if (tradeItem == null || !tradeItem.getSellerId().equals(player.getUUID())) {
                return false;
            }
            
            // 将物品返还给玩家
            giveItemToPlayer(player, tradeItem.getItemStack());
            
            // 从活跃交易中移除
            activeTradeItems.remove(tradeItemId);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 上架物品
     */
    public boolean listItem(ServerPlayer player, ItemStack itemStack, int price) {
        try {
            // 验证物品和价格
            if (itemStack.isEmpty() || price <= 0) {
                return false;
            }
            
            // 检查玩家是否有足够的物品
            if (!hasEnoughItems(player, itemStack)) {
                return false;
            }
            
            // 从玩家背包中移除物品
            removeItemFromInventory(player, itemStack);
            
            // 创建交易项目
            TradeItem tradeItem = new TradeItem(
                player.getUUID(),
                player.getName().getString(),
                itemStack,
                price
            );
            
            // 添加到活跃交易列表
            activeTradeItems.put(tradeItem.getId(), tradeItem);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 购买物品
     */
    public boolean purchaseItem(UUID tradeItemId, ServerPlayer buyer) {
        return purchaseItem(tradeItemId, buyer, 1);
    }
    
    /**
     * 购买指定数量的物品
     */
    public boolean purchaseItem(UUID tradeItemId, ServerPlayer buyer, int quantity) {
        try {
            // 从ItemListingManager获取交易物品
            TradeItem tradeItem = com.tradesystem.mod.manager.ItemListingManager.getInstance()
                    .getAllActiveListings().stream()
                    .filter(item -> item.getId().equals(tradeItemId))
                    .findFirst()
                    .orElse(null);
                    
            if (tradeItem == null || !tradeItem.isActive()) {
                return false;
            }
            
            // 检查是否是自己的物品
            if (tradeItem.getSellerId().equals(buyer.getUUID())) {
                buyer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.tradesystem.message.cannot_buy_own_item"));
                return false;
            }
            
            // 检查购买数量是否有效
            if (quantity <= 0 || quantity > tradeItem.getItemStack().getCount()) {
                return false;
            }
            
            // 计算总价格
            int totalPrice = tradeItem.getPrice() * quantity;
            
            // 检查买家是否有足够的金币
            if (!hasEnoughMoney(buyer, totalPrice)) {
                return false;
            }
            
            // 扣除买家金币
            deductMoney(buyer, totalPrice);
            
            // 给卖家添加金币
            addMoney(tradeItem.getSellerId(), totalPrice);
            
            // 创建购买的物品副本
            ItemStack purchasedItem = tradeItem.getItemStack().copy();
            purchasedItem.setCount(quantity);
            
            // 给买家添加物品
            giveItemToPlayer(buyer, purchasedItem);
            
            // 记录交易历史
            recordTransaction(tradeItem, buyer, quantity, totalPrice);
            
            // 如果购买了全部数量，移除交易物品（不返还给卖家）
            if (quantity >= tradeItem.getItemStack().getCount()) {
                com.tradesystem.mod.manager.ItemListingManager.getInstance()
                        .removeItemFromListing(tradeItemId, tradeItem.getSellerId());
            } else {
                // 否则减少物品数量
                ItemStack remainingItem = tradeItem.getItemStack().copy();
                remainingItem.setCount(remainingItem.getCount() - quantity);
                
                // 更新交易物品的数量
                // 注意：这里需要ItemListingManager支持更新物品数量的方法
                // 暂时先移除再重新上架
                com.tradesystem.mod.manager.ItemListingManager.getInstance()
                        .removeItemFromListing(tradeItemId, tradeItem.getSellerId());
                
                // 创建新的交易物品来替代剩余物品
                TradeItem newTradeItem = new TradeItem(
                    tradeItem.getSellerId(),
                    tradeItem.getSellerName(),
                    remainingItem,
                    tradeItem.getPrice()
                );
                
                // 添加到活跃交易列表
                activeTradeItems.put(newTradeItem.getId(), newTradeItem);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 回收物品
     */
    public boolean recycleItem(ServerPlayer player, ItemStack itemStack) {
        try {
            if (itemStack.isEmpty()) {
                return false;
            }
            
            // 计算回收价格（市场价的50%）
            int recyclePrice = calculateRecyclePrice(itemStack);
            
            // 给玩家添加金币
            addMoney(player.getUUID(), recyclePrice);
            
            // 记录回收交易
            TransactionRecord record = new TransactionRecord(
                player.getUUID(),
                player.getName().getString(),
                UUID.randomUUID(), // 系统UUID
                "System",
                itemStack,
                recyclePrice,
                TransactionRecord.Type.RECYCLE
            );
            
            addTransactionRecord(player.getUUID(), record);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // 辅助方法
    private boolean hasEnoughItems(ServerPlayer player, ItemStack required) {
        // 简化实现，实际应该检查背包
        return true;
    }
    
    private void removeItemFromInventory(ServerPlayer player, ItemStack itemStack) {
        // 简化实现，实际应该从背包移除物品
    }
    
    private boolean hasEnoughMoney(ServerPlayer player, int amount) {
        return com.tradesystem.mod.util.CurrencyUtil.getPlayerMoney(player) >= amount;
    }
    
    private void deductMoney(ServerPlayer player, int amount) {
        com.tradesystem.mod.util.CurrencyUtil.removePlayerMoney(player, amount);
    }
    
    private void addMoney(UUID playerId, int amount) {
        // 如果玩家在线，直接添加金币
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                // 在线玩家，直接添加金币
                com.tradesystem.mod.util.CurrencyUtil.addPlayerMoney(player, amount);
            } else {
                // 离线玩家，保存到离线数据中
                com.tradesystem.mod.data.TradeDataManager.getInstance().addOfflinePlayerCurrency(playerId, amount);
                com.tradesystem.mod.TradeMod.getLogger().info("卖家离线，金币已保存到离线数据: 玩家ID={}, 金额={}", playerId, amount);
            }
        }
    }
    
    private void giveItemToPlayer(ServerPlayer player, ItemStack itemStack) {
        // 尝试添加到玩家背包
        if (!player.getInventory().add(itemStack)) {
            // 如果背包满了，掉落在地上
            player.drop(itemStack, false);
        }
    }
    
    private int calculateRecyclePrice(ItemStack itemStack) {
        // 简化实现，返回固定价格
        return 10;
    }
    
    /**
     * 记录交易历史
     */
    private void recordTransaction(TradeItem tradeItem, ServerPlayer buyer) {
        recordTransaction(tradeItem, buyer, 1, tradeItem.getPrice());
    }
    
    /**
     * 记录交易历史（带数量和总价）
     */
    private void recordTransaction(TradeItem tradeItem, ServerPlayer buyer, int quantity, int totalPrice) {
        ItemStack itemStack = tradeItem.getItemStack().copy();
        
        // 调试：检查TradeItem的物品数据
        System.out.println("TradeManager.recordTransaction - 物品类型: " + itemStack.getItem().toString() + 
                          ", 数量: " + itemStack.getCount() + 
                          ", NBT: " + (itemStack.getTag() != null ? itemStack.getTag().toString() : "null"));
        
        TransactionRecord record = new TransactionRecord(
            tradeItem.getSellerId(),
            tradeItem.getSellerName(),
            buyer.getUUID(),
            buyer.getName().getString(),
            itemStack,
            totalPrice,
            TransactionRecord.Type.SELL
        );
        
        // 添加到内存中的交易历史
        addTransactionRecord(tradeItem.getSellerId(), record);
        addTransactionRecord(buyer.getUUID(), record);
        
        // 持久化存储交易记录到服务端
        saveTransactionToServer(record);
    }
    
    /**
     * 将交易记录保存到服务端持久化存储
     */
    private void saveTransactionToServer(TransactionRecord record) {
        try {
            // 获取数据服务实例
            com.tradesystem.mod.data.DataService dataService = com.tradesystem.mod.data.DataService.getInstance();
            if (dataService != null) {
                // 将交易记录转换为NBT格式
                net.minecraft.nbt.CompoundTag recordNBT = new net.minecraft.nbt.CompoundTag();
                recordNBT.putUUID("transaction_id", record.getTransactionId());
                recordNBT.putUUID("seller_id", record.getSellerId());
                recordNBT.putString("seller_name", record.getSellerName());
                recordNBT.putUUID("buyer_id", record.getBuyerId());
                recordNBT.putString("buyer_name", record.getBuyerName());
                recordNBT.put("item", record.getItemStack().save(new net.minecraft.nbt.CompoundTag()));
                recordNBT.putInt("price", record.getPrice());
                recordNBT.putLong("timestamp", record.getTimestamp());
                recordNBT.putString("type", record.getType().name());
                
                // 保存到TradeSavedData
                com.tradesystem.mod.data.TradeSavedData savedData = com.tradesystem.mod.data.TradeSavedData.get(
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
                if (savedData != null) {
                    savedData.addTradeHistory(recordNBT);
                    com.tradesystem.mod.TradeMod.getLogger().info("交易记录已保存到服务端: {} -> {} ({})", 
                        record.getSellerName(), record.getBuyerName(), record.getItemStack().getDisplayName().getString());
                }
            }
        } catch (Exception e) {
            com.tradesystem.mod.TradeMod.getLogger().error("保存交易记录到服务端时出错: {}", e.getMessage(), e);
        }
    }
    
    private void addTransactionRecord(UUID playerId, TransactionRecord record) {
        playerTransactionHistory.computeIfAbsent(playerId, k -> new ArrayList<>()).add(record);
    }
    
    // Getters
    public Collection<TradeItem> getActiveTradeItems() {
        return activeTradeItems.values();
    }
    
    public List<TransactionRecord> getPlayerTransactionHistory(UUID playerId) {
        return playerTransactionHistory.getOrDefault(playerId, new ArrayList<>());
    }
    
    /**
     * 更新交易物品的价格
     */
    public boolean updateItemPrice(UUID tradeItemId, ServerPlayer player, int newPrice) {
        try {
            // 验证价格
            if (newPrice <= 0) {
                return false;
            }
            
            TradeItem tradeItem = activeTradeItems.get(tradeItemId);
            if (tradeItem == null || !tradeItem.getSellerId().equals(player.getUUID())) {
                return false;
            }
            
            // 更新价格
            tradeItem.setPrice(newPrice);
            
            // 同时更新ItemListingManager中的数据
            com.tradesystem.mod.manager.ItemListingManager.getInstance().updateItemPrice(tradeItemId, newPrice);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}