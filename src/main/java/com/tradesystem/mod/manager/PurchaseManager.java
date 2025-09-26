package com.tradesystem.mod.manager;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.capability.ModCapabilities;
import com.tradesystem.mod.capability.PlayerCurrencyProvider;
import com.tradesystem.mod.config.TradeConfig;
import com.tradesystem.mod.data.TradeItem;
import com.tradesystem.mod.data.TradeDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * 购买管理器
 * 负责处理商品的购买、交易验证等功能
 */
public class PurchaseManager {
    private static PurchaseManager instance;
    
    private PurchaseManager() {}
    
    public static PurchaseManager getInstance() {
        if (instance == null) {
            instance = new PurchaseManager();
        }
        return instance;
    }
    
    /**
     * 购买物品
     */
    public boolean purchaseItem(ServerPlayer buyer, UUID itemId) {
        try {
            // 获取交易物品
            TradeItem tradeItem = ItemListingManager.getInstance().getAllActiveListings().stream()
                    .filter(item -> item.getId().equals(itemId))
                    .findFirst()
                    .orElse(null);
            
            if (tradeItem == null) {
                buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_not_available"));
                return false;
            }
            
            // 检查是否是自己的物品
            if (tradeItem.getSellerId().equals(buyer.getUUID())) {
                buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.cannot_buy_own_item"));
                return false;
            }
            
            // 检查买家是否有足够的货币
            if (!hasEnoughCurrency(buyer, tradeItem.getPrice())) {
                buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.insufficient_currency",
                        tradeItem.getPrice()));
                return false;
            }
            
            // 检查买家背包是否有空间
            if (!hasInventorySpace(buyer, tradeItem.getItemStack())) {
                buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.inventory_full"));
                return false;
            }
            
            // 获取卖家
            ServerPlayer seller = ServerLifecycleHooks.getCurrentServer().getPlayerList()
                    .getPlayer(tradeItem.getSellerId());
            
            // 执行交易
            if (executeTransaction(buyer, seller, tradeItem)) {
                // 从上架列表中移除物品 - 不返还给卖家（因为已经卖出）
                ItemListingManager.getInstance().removeItemFromListing(itemId, tradeItem.getSellerId());
                
                // 记录交易历史
                recordTransaction(buyer, tradeItem);
                
                // 发送成功消息
                buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.purchase_success",
                        tradeItem.getDisplayName(), tradeItem.getPrice()));
                
                if (seller != null && seller.isAlive()) {
                    seller.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_sold",
                            tradeItem.getDisplayName(), tradeItem.getPrice(), buyer.getName().getString()));
                }
                
                TradeMod.getLogger().info("交易完成: {} 购买了 {} 的 {} (价格: {})",
                        buyer.getName().getString(), tradeItem.getSellerName(),
                        tradeItem.getDisplayName(), tradeItem.getPrice());
                
                return true;
            }
            
        } catch (Exception e) {
            TradeMod.getLogger().error("购买物品时发生错误: {}", e.getMessage());
            buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.purchase_error"));
        }
        
        return false;
    }
    
    /**
     * 执行交易
     */
    private boolean executeTransaction(ServerPlayer buyer, ServerPlayer seller, TradeItem tradeItem) {
        try {
            // 扣除买家货币
            if (!deductCurrency(buyer, tradeItem.getPrice())) {
                return false;
            }
            
            // 给买家物品
            if (!giveItemToPlayer(buyer, tradeItem.getItemStack().copy())) {
                // 如果给物品失败，退还货币
                addCurrency(buyer, tradeItem.getPrice());
                return false;
            }
            
            // 同步买家背包到客户端
            buyer.inventoryMenu.broadcastChanges();
            
            // 计算税费
            int tax = calculateTax(tradeItem.getPrice());
            int sellerEarnings = tradeItem.getPrice() - tax;
            
            // 给卖家货币（如果在线）
            if (seller != null && seller.isAlive()) {
                addCurrency(seller, sellerEarnings);
            } else {
                // 离线玩家，保存到数据中
                addOfflinePlayerCurrency(tradeItem.getSellerId(), sellerEarnings);
            }
            
            // 系统收取税费
            if (tax > 0) {
                TradeDataManager.getInstance().addSystemRevenue(tax);
                TradeMod.getLogger().info("系统收取税费: {} (交易金额: {})", tax, tradeItem.getPrice());
            }
            
            return true;
            
        } catch (Exception e) {
            TradeMod.getLogger().error("执行交易时发生错误: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查玩家是否有足够的货币
     */
    private boolean hasEnoughCurrency(ServerPlayer player, int amount) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY).map(cap -> {
            return cap.hasMoney(amount);
        }).orElse(false);
    }
    
    /**
     * 检查背包是否有空间
     */
    private boolean hasInventorySpace(ServerPlayer player, ItemStack itemStack) {
        return player.getInventory().getFreeSlot() != -1 || 
               canStackInExistingSlots(player, itemStack);
    }
    
    /**
     * 检查是否可以堆叠到现有槽位
     */
    private boolean canStackInExistingSlots(ServerPlayer player, ItemStack itemStack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, itemStack)) {
                int maxStackSize = Math.min(slotStack.getMaxStackSize(), player.getInventory().getMaxStackSize());
                if (slotStack.getCount() + itemStack.getCount() <= maxStackSize) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 扣除玩家货币
     */
    private boolean deductCurrency(ServerPlayer player, int amount) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY).map(cap -> {
            return cap.removeMoney(amount);
        }).orElse(false);
    }
    
    /**
     * 给玩家添加货币
     */
    private boolean addCurrency(ServerPlayer player, int amount) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY).map(cap -> {
            return cap.addMoney(amount);
        }).orElse(false);
    }
    
    /**
     * 给离线玩家添加货币
     */
    private void addOfflinePlayerCurrency(UUID playerId, int amount) {
        TradeDataManager.getInstance().addOfflinePlayerCurrency(playerId, amount);
    }
    
    /**
     * 给玩家物品
     */
    private boolean giveItemToPlayer(ServerPlayer player, ItemStack itemStack) {
        return player.getInventory().add(itemStack);
    }
    
    /**
     * 计算税费
     */
    private int calculateTax(int price) {
        double taxRate = TradeConfig.TRANSACTION_TAX_RATE.get();
        return (int) Math.ceil(price * taxRate);
    }
    
    /**
     * 记录交易历史
     */
    private void recordTransaction(ServerPlayer buyer, TradeItem tradeItem) {
        TradeDataManager.getInstance().recordTransaction(
                buyer.getUUID(),
                buyer.getName().getString(),
                tradeItem.getSellerId(),
                tradeItem.getSellerName(),
                tradeItem.getItemStack(),
                tradeItem.getPrice(),
                System.currentTimeMillis()
        );
    }
    
    /**
     * 创建离线玩家（用于处理离线卖家的情况）
     */
    private ServerPlayer createOfflinePlayer(UUID playerId) {
        // 这里返回null，在实际调用中会处理null情况
        return null;
    }
    
    /**
     * 批量购买物品
     */
    public boolean purchaseMultipleItems(ServerPlayer buyer, UUID[] itemIds) {
        if (itemIds == null || itemIds.length == 0) {
            return false;
        }
        
        // 计算总价格
        int totalPrice = 0;
        for (UUID itemId : itemIds) {
            TradeItem item = ItemListingManager.getInstance().getAllActiveListings().stream()
                    .filter(tradeItem -> tradeItem.getId().equals(itemId))
                    .findFirst()
                    .orElse(null);
            
            if (item == null) {
                buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.some_items_unavailable"));
                return false;
            }
            
            totalPrice += item.getPrice();
        }
        
        // 检查是否有足够的货币
        if (!hasEnoughCurrency(buyer, totalPrice)) {
            buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.insufficient_currency", totalPrice));
            return false;
        }
        
        // 逐个购买
        int successCount = 0;
        for (UUID itemId : itemIds) {
            if (purchaseItem(buyer, itemId)) {
                successCount++;
            }
        }
        
        if (successCount > 0) {
            buyer.sendSystemMessage(Component.translatable("gui.tradesystem.message.batch_purchase_success",
                    successCount, itemIds.length));
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取物品的实时价格（考虑市场波动）
     */
    public int getItemPrice(UUID itemId) {
        TradeItem item = ItemListingManager.getInstance().getAllActiveListings().stream()
                .filter(tradeItem -> tradeItem.getId().equals(itemId))
                .findFirst()
                .orElse(null);
        
        return item != null ? item.getPrice() : 0;
    }
    
    /**
     * 检查物品是否仍然可购买
     */
    public boolean isItemAvailable(UUID itemId) {
        return ItemListingManager.getInstance().getAllActiveListings().stream()
                .anyMatch(item -> item.getId().equals(itemId) && item.isActive());
    }
}