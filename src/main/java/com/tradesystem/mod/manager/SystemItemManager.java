package com.tradesystem.mod.manager;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.data.SystemItem;
import com.tradesystem.mod.data.TradeItem;
import com.tradesystem.mod.capability.IPlayerCurrency;
import com.tradesystem.mod.capability.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统商品管理器
 * 负责管理系统商品的增删改查和数据持久化
 */
public class SystemItemManager {
    
    private static SystemItemManager instance;
    private final Map<String, SystemItem> systemItems = new ConcurrentHashMap<>();
    private final File dataFile;
    private boolean dirty = false;
    
    private SystemItemManager() {
        File serverDir = ServerLifecycleHooks.getCurrentServer().getServerDirectory();
        File tradeDir = new File(serverDir, "trade_system");
        if (!tradeDir.exists()) {
            tradeDir.mkdirs();
        }
        this.dataFile = new File(tradeDir, "system_items.dat");
        loadData();
    }
    
    public static SystemItemManager getInstance() {
        if (instance == null) {
            instance = new SystemItemManager();
        }
        return instance;
    }
    
    /**
     * 添加系统商品
     */
    public boolean addSystemItem(ItemStack itemStack, int price, int quantity, String adminName) {
        if (itemStack.isEmpty() || price <= 0 || quantity <= 0) {
            return false;
        }
        
        String itemKey = getItemKey(itemStack);
        
        // 检查是否已存在
        if (systemItems.containsKey(itemKey)) {
            TradeMod.getLogger().warn("System item already exists: {}", itemKey);
            return false;
        }
        
        SystemItem systemItem = new SystemItem(itemStack, price, quantity, adminName);
        systemItems.put(itemKey, systemItem);
        
        setDirty();
        TradeMod.getLogger().info("Added system item: {} by {}", itemKey, adminName);
        return true;
    }
    
    /**
     * 移除系统商品（通过UUID）
     */
    public boolean removeSystemItem(UUID itemId) {
        SystemItem systemItem = systemItems.values().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
        
        if (systemItem != null) {
            String itemKey = getItemKey(systemItem.getItemStack());
            systemItems.remove(itemKey);
            setDirty();
            TradeMod.getLogger().info("Removed system item: {}", itemId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 移除系统商品
     */
    public boolean removeSystemItem(ItemStack itemStack) {
        String itemKey = getItemKey(itemStack);
        SystemItem removed = systemItems.remove(itemKey);
        
        if (removed != null) {
            setDirty();
            TradeMod.getLogger().info("Removed system item: {}", itemKey);
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新系统商品价格（通过UUID）
     */
    public boolean updateSystemItemPrice(UUID itemId, int newPrice) {
        if (newPrice <= 0) {
            return false;
        }
        
        SystemItem systemItem = systemItems.values().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
        
        if (systemItem != null) {
            systemItem.setPrice(newPrice);
            setDirty();
            TradeMod.getLogger().info("Updated price for system item: {} to {}", itemId, newPrice);
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新系统商品价格
     */
    public boolean updateItemPrice(ItemStack itemStack, int newPrice) {
        if (newPrice <= 0) {
            return false;
        }
        
        String itemKey = getItemKey(itemStack);
        SystemItem systemItem = systemItems.get(itemKey);
        
        if (systemItem != null) {
            systemItem.setPrice(newPrice);
            setDirty();
            TradeMod.getLogger().info("Updated price for system item: {} to {}", itemKey, newPrice);
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新系统商品数量（通过UUID）
     */
    public boolean updateSystemItemQuantity(UUID itemId, int newQuantity) {
        if (newQuantity <= 0) {
            return false;
        }
        
        SystemItem systemItem = systemItems.values().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
        
        if (systemItem != null) {
            systemItem.setQuantity(newQuantity);
            setDirty();
            TradeMod.getLogger().info("Updated quantity for system item: {} to {}", itemId, newQuantity);
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新系统商品数量
     */
    public boolean updateItemQuantity(ItemStack itemStack, int newQuantity) {
        if (newQuantity <= 0) {
            return false;
        }
        
        String itemKey = getItemKey(itemStack);
        SystemItem systemItem = systemItems.get(itemKey);
        
        if (systemItem != null) {
            systemItem.setQuantity(newQuantity);
            setDirty();
            TradeMod.getLogger().info("Updated quantity for system item: {} to {}", itemKey, newQuantity);
            return true;
        }
        
        return false;
    }
    
    /**
     * 切换系统商品状态（通过UUID）
     */
    public boolean toggleSystemItemStatus(UUID itemId) {
        SystemItem systemItem = systemItems.values().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
        
        if (systemItem != null) {
            systemItem.toggleActive();
            setDirty();
            TradeMod.getLogger().info("Toggled status for system item: {} to {}", 
                    itemId, systemItem.isActive());
            return true;
        }
        
        return false;
    }
    
    /**
     * 切换系统商品状态
     */
    public boolean toggleItemStatus(ItemStack itemStack) {
        String itemKey = getItemKey(itemStack);
        SystemItem systemItem = systemItems.get(itemKey);
        
        if (systemItem != null) {
            systemItem.toggleActive();
            setDirty();
            TradeMod.getLogger().info("Toggled status for system item: {} to {}", 
                    itemKey, systemItem.isActive());
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取系统商品
     */
    public SystemItem getSystemItem(ItemStack itemStack) {
        String itemKey = getItemKey(itemStack);
        return systemItems.get(itemKey);
    }
    
    /**
     * 检查是否是系统商品
     */
    public boolean isSystemItem(ItemStack itemStack) {
        return getSystemItem(itemStack) != null;
    }
    
    /**
     * 获取所有系统商品
     */
    public List<SystemItem> getAllSystemItems() {
        return new ArrayList<>(systemItems.values());
    }
    
    /**
     * 获取所有活跃的系统商品
     */
    public List<SystemItem> getActiveSystemItems() {
        return systemItems.values().stream()
                .filter(SystemItem::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取系统商品作为TradeItem列表
     */
    public List<TradeItem> getSystemItemsAsTradeItems() {
        return getActiveSystemItems().stream()
                .map(SystemItem::toTradeItem)
                .collect(Collectors.toList());
    }
    
    /**
     * 搜索系统商品
     */
    public List<SystemItem> searchSystemItems(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveSystemItems();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return getActiveSystemItems().stream()
                .filter(item -> item.getDisplayName().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }
    
    /**
     * 购买系统商品
     */
    public boolean purchaseSystemItem(ServerPlayer player, ItemStack itemStack, int quantity) {
        SystemItem systemItem = getSystemItem(itemStack);
        
        if (systemItem == null || !systemItem.isActive()) {
            return false;
        }
        
        int totalPrice = systemItem.getPrice() * quantity;
        
        // 检查玩家货币
        if (!hasEnoughCurrency(player, totalPrice)) {
            return false;
        }
        
        // 检查背包空间
        if (!hasInventorySpace(player, itemStack, quantity)) {
            return false;
        }
        
        // 扣除货币
        if (!deductCurrency(player, totalPrice)) {
            return false;
        }
        
        // 给予物品
        ItemStack purchaseItem = systemItem.getItemStack().copy();
        purchaseItem.setCount(quantity);
        
        if (!player.getInventory().add(purchaseItem)) {
            // 如果添加失败，退还货币
            addCurrency(player, totalPrice);
            return false;
        }
        
        TradeMod.getLogger().info("Player {} purchased {} x{} from system for {} coins (infinite stock)", 
                player.getName().getString(), systemItem.getDisplayName(), quantity, totalPrice);
        
        return true;
    }
    
    /**
     * 检查管理员权限
     */
    public boolean hasAdminPermission(ServerPlayer player) {
        // 检查OP权限
        if (player.hasPermissions(2)) {
            return true;
        }
        
        // 这里可以添加更复杂的权限检查逻辑
        // 例如检查特定的权限节点或权限组
        
        return false;
    }
    
    /**
     * 生成物品键
     */
    private String getItemKey(ItemStack itemStack) {
        StringBuilder key = new StringBuilder();
        key.append(itemStack.getItem().toString());
        
        // 如果有NBT数据，也包含在键中
        if (itemStack.hasTag()) {
            key.append("_").append(itemStack.getTag().hashCode());
        }
        
        // 添加时间戳确保唯一性
        key.append("_").append(System.currentTimeMillis());
        
        return key.toString();
    }
    
    /**
     * 检查玩家是否有足够货币
     */
    private boolean hasEnoughCurrency(ServerPlayer player, int amount) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY).map(cap -> {
            return cap.hasMoney(amount);
        }).orElse(false);
    }
    
    /**
     * 检查背包空间
     */
    private boolean hasInventorySpace(ServerPlayer player, ItemStack itemStack, int quantity) {
        // 简化的空间检查
        return player.getInventory().getFreeSlot() != -1 || 
               canStackInExistingSlots(player, itemStack, quantity);
    }
    
    /**
     * 检查是否可以堆叠到现有槽位
     */
    private boolean canStackInExistingSlots(ServerPlayer player, ItemStack itemStack, int quantity) {
        int remainingQuantity = quantity;
        
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingQuantity > 0; i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            
            if (ItemStack.isSameItemSameTags(slotStack, itemStack)) {
                int maxStackSize = Math.min(slotStack.getMaxStackSize(), itemStack.getMaxStackSize());
                int availableSpace = maxStackSize - slotStack.getCount();
                
                if (availableSpace > 0) {
                    remainingQuantity -= Math.min(availableSpace, remainingQuantity);
                }
            }
        }
        
        return remainingQuantity <= 0;
    }
    
    /**
     * 扣除玩家货币
     */
    private boolean deductCurrency(ServerPlayer player, int amount) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY).map(cap -> {
            if (cap.hasMoney(amount)) {
                cap.removeMoney(amount);
                return true;
            }
            return false;
        }).orElse(false);
    }
    
    /**
     * 给予玩家货币
     */
    private boolean addCurrency(ServerPlayer player, int amount) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY).map(cap -> {
            cap.addMoney(amount);
            return true;
        }).orElse(false);
    }
    
    /**
     * 标记数据为脏
     */
    private void setDirty() {
        this.dirty = true;
    }
    
    /**
     * 保存数据
     */
    public void saveData() {
        if (!dirty) {
            return;
        }
        
        try {
            CompoundTag rootTag = new CompoundTag();
            ListTag itemsTag = new ListTag();
            
            for (SystemItem item : systemItems.values()) {
                itemsTag.add(item.toNBT());
            }
            
            rootTag.put("systemItems", itemsTag);
            rootTag.putLong("lastSaved", System.currentTimeMillis());
            
            NbtIo.writeCompressed(rootTag, dataFile);
            dirty = false;
            
            TradeMod.getLogger().info("Saved {} system items to disk", systemItems.size());
            
        } catch (IOException e) {
            TradeMod.getLogger().error("Failed to save system items data", e);
        }
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        if (!dataFile.exists()) {
            TradeMod.getLogger().info("System items data file not found, starting with empty data");
            return;
        }
        
        try {
            CompoundTag rootTag = NbtIo.readCompressed(dataFile);
            
            if (rootTag.contains("systemItems")) {
                ListTag itemsTag = rootTag.getList("systemItems", Tag.TAG_COMPOUND);
                
                for (int i = 0; i < itemsTag.size(); i++) {
                    CompoundTag itemTag = itemsTag.getCompound(i);
                    SystemItem item = SystemItem.fromNBT(itemTag);
                    
                    if (!item.getItemStack().isEmpty()) {
                        String itemKey = getItemKey(item.getItemStack());
                        systemItems.put(itemKey, item);
                    }
                }
            }
            
            TradeMod.getLogger().info("Loaded {} system items from disk", systemItems.size());
            
        } catch (IOException e) {
            TradeMod.getLogger().error("Failed to load system items data", e);
        }
    }
    
    /**
     * 检查玩家是否为管理员
     */
    public boolean isAdmin(ServerPlayer player) {
        // 检查玩家是否有管理员权限
        return player.hasPermissions(2) || player.getServer().getPlayerList().isOp(player.getGameProfile());
    }
    
    /**
     * 购买系统商品
     */
    public boolean purchaseSystemItem(ServerPlayer player, UUID itemId, int quantity) {
        SystemItem systemItem = systemItems.values().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
        
        if (systemItem == null || !systemItem.isActive()) {
            return false;
        }
        
        int totalPrice = systemItem.getPrice() * quantity;
        
        // 检查玩家金钱
        IPlayerCurrency cap = player.getCapability(ModCapabilities.PLAYER_CURRENCY).orElse(null);
        if (cap == null || cap.getMoney() < totalPrice) {
            return false;
        }
        
        // 扣除金钱
        cap.removeMoney(totalPrice);
        
        // 同步货币到客户端
        com.tradesystem.mod.util.CurrencyUtil.syncCurrencyToClient(player);
        
        // 系统商品为无限购买，不减少数量，不改变状态
        // 注释：保持原有的无限购买设计，quantity字段仅用于显示
        
        // 给予物品
        ItemStack itemToGive = systemItem.getItemStack().copy();
        itemToGive.setCount(quantity);
        
        if (!player.getInventory().add(itemToGive)) {
            // 如果背包满了，掉落物品
            player.drop(itemToGive, false);
        }
        
        TradeMod.getLogger().info("Player {} purchased {} x{} for {} coins (infinite stock)", 
                player.getName().getString(), systemItem.getDisplayName(), quantity, totalPrice);
        
        return true;
    }
    
    /**
     * 定期保存数据
     */
    public void tick() {
        if (dirty) {
            saveData();
        }
    }
    
    /**
     * 关闭时保存数据
     */
    public void shutdown() {
        saveData();
        TradeMod.getLogger().info("SystemItemManager shutdown complete");
    }
}