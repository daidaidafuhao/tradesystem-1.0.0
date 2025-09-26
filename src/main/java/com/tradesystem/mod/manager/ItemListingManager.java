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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 商品上架管理器
 * 负责处理商品的上架、下架、搜索等功能
 */
public class ItemListingManager {
    private static ItemListingManager instance;
    private final Map<UUID, TradeItem> activeListings = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerListings = new ConcurrentHashMap<>();
    
    // 客户端缓存（仅在客户端使用）
    private final Map<UUID, TradeItem> clientCache = new ConcurrentHashMap<>();
    
    private ItemListingManager() {}
    
    public static ItemListingManager getInstance() {
        if (instance == null) {
            instance = new ItemListingManager();
        }
        return instance;
    }
    
    /**
     * 上架物品
     */
    public boolean listItem(ServerPlayer seller, ItemStack itemStack, int price) {
        try {
            // 验证输入参数
            if (seller == null || itemStack.isEmpty() || price <= 0) {
                return false;
            }
            
            // 检查玩家上架数量限制
            if (!canPlayerListMore(seller.getUUID())) {
                seller.sendSystemMessage(Component.translatable("gui.tradesystem.message.max_listings_reached"));
                return false;
            }
            
            // 检查物品是否在黑名单中
            if (isItemBlacklisted(itemStack)) {
                seller.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_blacklisted"));
                return false;
            }
            
            // 从玩家背包中移除物品（在listItem方法中不需要移除，因为ListItemPacket已经处理了）
            // if (!removeItemFromInventory(seller, itemStack)) {
            //     seller.sendSystemMessage(Component.translatable("gui.tradesystem.message.insufficient_items"));
            //     return false;
            // }
            
            // 创建交易物品
            TradeItem tradeItem = new TradeItem(
                seller.getUUID(),
                seller.getName().getString(),
                itemStack,
                price
            );
            
            // 添加到活跃列表
            activeListings.put(tradeItem.getId(), tradeItem);
            playerListings.computeIfAbsent(seller.getUUID(), k -> new HashSet<>()).add(tradeItem.getId());
            
            // 保存到数据管理器
            TradeDataManager.getInstance().saveTradeItem(tradeItem);
            
            // 同步市场数据到所有客户端
            com.tradesystem.mod.data.DataService.getInstance().syncMarketDataToAllClients();
            
            // 发送成功消息
            seller.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_listed_success",
                    itemStack.getHoverName(), price));
            
            TradeMod.getLogger().info("玩家 {} 上架物品: {} x{} 价格: {}",
                    seller.getName().getString(), itemStack.getHoverName().getString(),
                    itemStack.getCount(), price);
            
            return true;
            
        } catch (Exception e) {
            TradeMod.getLogger().error("上架物品时发生错误: {}", e.getMessage());
            seller.sendSystemMessage(Component.translatable("gui.tradesystem.message.listing_error"));
            return false;
        }
    }
    
    /**
     * 下架物品
     */
    public boolean unlistItem(ServerPlayer player, UUID itemId) {
        try {
            TradeItem tradeItem = activeListings.get(itemId);
            if (tradeItem == null) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_not_found"));
                return false;
            }
            
            // 检查是否是物品的所有者
            if (!tradeItem.getSellerId().equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.not_owner"));
                return false;
            }
            
            // 将物品返还给玩家
            if (!giveItemToPlayer(player, tradeItem.getItemStack())) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.inventory_full"));
                return false;
            }
            
            // 从列表中移除
            activeListings.remove(itemId);
            Set<UUID> playerItems = playerListings.get(player.getUUID());
            if (playerItems != null) {
                playerItems.remove(itemId);
            }
            
            // 从数据管理器中删除
            com.tradesystem.mod.data.JsonDataManager.getInstance().removeTradeItem(itemId);
            TradeDataManager.getInstance().removeTradeItem(itemId);
            
            // 从DataService中移除物品数据
            com.tradesystem.mod.data.DataService.getInstance().removeMarketItem(itemId.toString());
            
            // 同步市场数据到所有客户端
            com.tradesystem.mod.data.DataService.getInstance().syncMarketDataToAllClients();
            
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.item_unlisted_success",
                    tradeItem.getDisplayName()));
            
            TradeMod.getLogger().info("玩家 {} 下架物品: {}",
                    player.getName().getString(), tradeItem.getDisplayName());
            
            return true;
            
        } catch (Exception e) {
            TradeMod.getLogger().error("下架物品时发生错误: {}", e.getMessage());
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.unlisting_error"));
            return false;
        }
    }
    
    /**
     * 下架物品（通过物品ID和玩家ID）
     */
    /**
     * 下架物品（用于购买完成后，不返还物品给卖家）
     */
    public boolean removeItemFromListing(UUID itemId, UUID playerId) {
        try {
            // 添加调试日志
            TradeMod.getLogger().info("ItemListingManager.removeItemFromListing: 物品ID={}, 玩家ID={}", itemId, playerId);
            
            TradeItem tradeItem = activeListings.get(itemId);
            if (tradeItem == null) {
                TradeMod.getLogger().warn("物品不存在: {}", itemId);
                return false;
            }
            
            // 检查是否是物品的所有者
            if (!tradeItem.getSellerId().equals(playerId)) {
                TradeMod.getLogger().warn("玩家 {} 不是物品 {} 的所有者", playerId, itemId);
                return false;
            }
            
            // 从列表中移除（不返还物品）
            activeListings.remove(itemId);
            Set<UUID> playerItems = playerListings.get(playerId);
            if (playerItems != null) {
                playerItems.remove(itemId);
            }
            
            // 从数据管理器中删除
            com.tradesystem.mod.data.JsonDataManager.getInstance().removeTradeItem(itemId);
            TradeDataManager.getInstance().removeTradeItem(itemId);
            
            // 从DataService中移除物品数据
            com.tradesystem.mod.data.DataService.getInstance().removeMarketItem(itemId.toString());
            
            // 同步市场数据到所有客户端
            com.tradesystem.mod.data.DataService.getInstance().syncMarketDataToAllClients();
            
            TradeMod.getLogger().info("物品已从市场移除: {} (玩家ID: {})",
                    tradeItem.getDisplayName(), playerId);
            
            return true;
            
        } catch (Exception e) {
            TradeMod.getLogger().error("从市场移除物品时发生错误: {}", e.getMessage());
            return false;
        }
    }

    public boolean unlistItem(UUID itemId, UUID playerId) {
        try {
            // 添加调试日志
            TradeMod.getLogger().info("ItemListingManager.unlistItem: 物品ID={}, 玩家ID={}", itemId, playerId);
            
            TradeItem tradeItem = activeListings.get(itemId);
            if (tradeItem == null) {
                TradeMod.getLogger().warn("物品不存在: {}", itemId);
                return false;
            }
            
            // 检查是否是物品的所有者
            if (!tradeItem.getSellerId().equals(playerId)) {
                TradeMod.getLogger().warn("玩家 {} 不是物品 {} 的所有者", playerId, itemId);
                return false;
            }
            
            // 尝试获取在线玩家并返还物品
            ServerPlayer player = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(playerId);
            if (player != null) {
                // 玩家在线，直接返还物品
                TradeMod.getLogger().info("玩家在线，直接返还物品: {}", player.getName().getString());
                if (!giveItemToPlayer(player, tradeItem.getItemStack())) {
                    // 如果背包满了，掉落在地上
                    player.drop(tradeItem.getItemStack(), false);
                }
                
                // 发送成功消息
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "gui.tradesystem.message.item_unlisted_success", tradeItem.getDisplayName()));
            } else {
                // 玩家离线，将物品保存到离线存储
                TradeMod.getLogger().info("玩家离线，保存到离线存储: {}", playerId);
                TradeDataManager.getInstance().addOfflinePlayerItem(playerId, tradeItem.getItemStack());
            }
            
            // 从列表中移除
            activeListings.remove(itemId);
            Set<UUID> playerItems = playerListings.get(playerId);
            if (playerItems != null) {
                playerItems.remove(itemId);
            }
            
            // 从数据管理器中删除
            com.tradesystem.mod.data.JsonDataManager.getInstance().removeTradeItem(itemId);
            TradeDataManager.getInstance().removeTradeItem(itemId);
            
            // 同步市场数据到所有客户端
            com.tradesystem.mod.data.DataService.getInstance().syncMarketDataToAllClients();
            
            TradeMod.getLogger().info("物品已下架: {} (玩家ID: {})",
                    tradeItem.getDisplayName(), playerId);
            
            return true;
            
        } catch (Exception e) {
            TradeMod.getLogger().error("下架物品时发生错误: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取所有活跃的交易物品
     */
    public List<TradeItem> getAllActiveListings() {
        // 检查是否在客户端环境
        try {
            // 尝试获取客户端Minecraft实例来判断是否在客户端
            net.minecraft.client.Minecraft.getInstance();
            // 如果成功，说明在客户端，使用缓存数据
            return clientCache.values().stream()
                    .filter(TradeItem::isActive)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // 如果失败，说明在服务端，使用实际数据
            return activeListings.values().stream()
                    .filter(TradeItem::isActive)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 清空客户端缓存（仅客户端使用）
     */
    public void clearClientCache() {
        clientCache.clear();
    }
    
    /**
     * 添加物品到客户端缓存（仅客户端使用）
     */
    public void addClientItem(TradeItem item) {
        if (item != null) {
            clientCache.put(item.getId(), item);
        }
    }
    
    /**
     * 更新客户端缓存（用于接收服务器同步的市场数据）
     */
    public void updateClientCache(net.minecraft.nbt.CompoundTag data) {
        try {
            TradeMod.getLogger().info("开始更新客户端缓存，接收到 {} 个物品数据", data.size());
            
            // 清空现有缓存
            clientCache.clear();
            
            // 从NBT数据重建TradeItem对象
            for (String key : data.getAllKeys()) {
                net.minecraft.nbt.CompoundTag itemTag = data.getCompound(key);
                try {
                    TradeItem item = new TradeItem(itemTag);
                    if (item != null) {
                        clientCache.put(item.getId(), item);
                        TradeMod.getLogger().debug("添加物品到客户端缓存: {} - {} (卖家: {})", 
                                item.getId(), item.getItemStack().getDisplayName().getString(), item.getSellerName());
                    }
                } catch (Exception e) {
                    TradeMod.getLogger().warn("无法从NBT数据创建TradeItem: {} - {}", key, e.getMessage());
                }
            }
            
            TradeMod.getLogger().info("客户端缓存更新完成，共 {} 个物品", clientCache.size());
            
            // 通知所有打开的BuyScreen刷新显示
            notifyBuyScreenRefresh();
            
            // 通知ItemManagementScreen刷新显示
            notifyItemManagementScreenRefresh();
        } catch (Exception e) {
            TradeMod.getLogger().error("更新客户端缓存时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 通知BuyScreen刷新显示
     */
    private void notifyBuyScreenRefresh() {
        // 在客户端环境下，通知当前打开的BuyScreen刷新
        if (isClientSide()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.tradesystem.mod.client.gui.BuyScreen) {
                ((com.tradesystem.mod.client.gui.BuyScreen) mc.screen).refreshItems();
                TradeMod.getLogger().debug("已通知BuyScreen刷新物品列表");
            }
        }
    }
    
    /**
     * 通知ItemManagementScreen刷新显示
     */
    private void notifyItemManagementScreenRefresh() {
        // 在客户端环境下，通知当前打开的ItemManagementScreen刷新
        if (isClientSide()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.tradesystem.mod.client.gui.ItemManagementScreen) {
                ((com.tradesystem.mod.client.gui.ItemManagementScreen) mc.screen).refreshData();
                TradeMod.getLogger().debug("已通知ItemManagementScreen刷新商品列表");
            }
        }
    }
    
    /**
     * 获取玩家的所有上架物品
     */
    public List<TradeItem> getPlayerListings(UUID playerId) {
        // 在客户端环境下，从客户端缓存获取
        if (isClientSide()) {
            List<TradeItem> playerItems = clientCache.values().stream()
                    .filter(item -> item.getSellerId().equals(playerId))
                    .filter(TradeItem::isActive)
                    .collect(Collectors.toList());
            
            TradeMod.getLogger().debug("客户端获取玩家商品: 玩家ID={}, 缓存总数={}, 玩家商品数={}", 
                    playerId, clientCache.size(), playerItems.size());
            
            return playerItems;
        }
        
        // 在服务端环境下，从活跃列表获取
        Set<UUID> playerItems = playerListings.get(playerId);
        if (playerItems == null) {
            return new ArrayList<>();
        }
        
        return playerItems.stream()
                .map(activeListings::get)
                .filter(Objects::nonNull)
                .filter(TradeItem::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查是否在客户端环境
     */
    private boolean isClientSide() {
        try {
            net.minecraft.client.Minecraft.getInstance();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 搜索物品
     */
    public List<TradeItem> searchItems(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveListings();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return activeListings.values().stream()
                .filter(TradeItem::isActive)
                .filter(item -> item.getDisplayName().toLowerCase().contains(lowerKeyword) ||
                               item.getSellerName().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }
    
    /**
     * 按价格范围搜索
     */
    public List<TradeItem> searchByPriceRange(int minPrice, int maxPrice) {
        return activeListings.values().stream()
                .filter(TradeItem::isActive)
                .filter(item -> item.getPrice() >= minPrice && item.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新物品价格（带权限验证）
     */
    public boolean updateItemPrice(UUID itemId, int newPrice, UUID playerId) {
        TradeItem item = activeListings.get(itemId);
        if (item != null && item.isActive() && item.getSellerId().equals(playerId)) {
            item.setPrice(newPrice);
            // 保存到数据管理器
            TradeDataManager.getInstance().saveTradeItem(item);
            // 同步数据到DataService
            com.tradesystem.mod.data.DataService.getInstance().addMarketItem(itemId.toString(), item.toNBT());
            
            // 触发数据同步到所有客户端
            com.tradesystem.mod.data.DataService.getInstance().syncMarketDataToAllClients();
            return true;
        }
        return false;
    }
    
    /**
     * 更新物品价格（兼容旧版本，无权限验证）
     */
    public boolean updateItemPrice(UUID itemId, int newPrice) {
        TradeItem item = activeListings.get(itemId);
        if (item != null && item.isActive()) {
            item.setPrice(newPrice);
            // 保存到数据管理器
            TradeDataManager.getInstance().saveTradeItem(item);
            // 同步数据到DataService
            com.tradesystem.mod.data.DataService.getInstance().addMarketItem(itemId.toString(), item.toNBT());
            return true;
        }
        return false;
    }
    
    /**
     * 检查玩家是否可以上架更多物品
     */
    private boolean canPlayerListMore(UUID playerId) {
        Set<UUID> playerItems = playerListings.get(playerId);
        int currentCount = playerItems != null ? playerItems.size() : 0;
        return currentCount < TradeConfig.getMaxListingsPerPlayer();
    }
    
    /**
     * 检查物品是否在黑名单中
     */
    private boolean isItemBlacklisted(ItemStack itemStack) {
        String itemName = itemStack.getItem().toString();
        return TradeConfig.getBlacklistedItems().contains(itemName);
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
     * 给玩家物品
     */
    private boolean giveItemToPlayer(ServerPlayer player, ItemStack itemStack) {
        return player.getInventory().add(itemStack);
    }
    
    /**
     * 清理过期物品
     */
    public void cleanupExpiredItems() {
        long maxAge = TradeConfig.ITEM_EXPIRY_TIME.get() * 24 * 60 * 60 * 1000L; // 转换为毫秒
        
        List<UUID> expiredItems = activeListings.values().stream()
                .filter(item -> item.isExpired(maxAge))
                .map(TradeItem::getId)
                .collect(Collectors.toList());
        
        for (UUID itemId : expiredItems) {
            TradeItem item = activeListings.remove(itemId);
            if (item != null) {
                // 从玩家列表中移除
                Set<UUID> playerItems = playerListings.get(item.getSellerId());
                if (playerItems != null) {
                    playerItems.remove(itemId);
                }
                
                // 从数据管理器中删除
                com.tradesystem.mod.data.JsonDataManager.getInstance().removeTradeItem(itemId);
                TradeDataManager.getInstance().removeTradeItem(itemId);
                
                TradeMod.getLogger().info("清理过期物品: {} (卖家: {})",
                        item.getDisplayName(), item.getSellerName());
            }
        }
    }
    
    /**
     * 加载数据
     */
    public void loadData() {
        // 优先从JSON数据管理器加载
        Map<UUID, TradeItem> jsonItems = com.tradesystem.mod.data.JsonDataManager.getInstance().getAllTradeItems();
        if (!jsonItems.isEmpty()) {
            for (TradeItem item : jsonItems.values()) {
                if (item.isActive()) {
                    activeListings.put(item.getId(), item);
                    playerListings.computeIfAbsent(item.getSellerId(), k -> new HashSet<>()).add(item.getId());
                }
            }
            TradeMod.getLogger().info("从JSON加载了 {} 个活跃的交易物品", activeListings.size());
        } else {
            // 回退到原有的数据管理器
            List<TradeItem> items = TradeDataManager.getInstance().getAllTradeItems();
            for (TradeItem item : items) {
                if (item.isActive()) {
                    activeListings.put(item.getId(), item);
                    playerListings.computeIfAbsent(item.getSellerId(), k -> new HashSet<>()).add(item.getId());
                }
            }
            TradeMod.getLogger().info("从传统数据源加载了 {} 个活跃的交易物品", activeListings.size());
        }
    }
    
    /**
     * 保存数据
     */
    public void saveData() {
        // 同时保存到JSON和传统数据管理器
        for (TradeItem item : activeListings.values()) {
            com.tradesystem.mod.data.JsonDataManager.getInstance().saveTradeItem(item);
            TradeDataManager.getInstance().saveTradeItem(item);
        }
        TradeMod.getLogger().info("保存了 {} 个交易物品到JSON和传统数据源", activeListings.size());
    }
}