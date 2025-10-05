package com.tradesystem.mod.client;

import com.tradesystem.mod.data.SystemItem;
import com.tradesystem.mod.TradeMod;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客户端系统商品管理器
 * 负责在客户端管理系统商品数据
 */
public class ClientSystemItemManager {
    
    private static ClientSystemItemManager instance;
    private final List<SystemItem> systemItems = new CopyOnWriteArrayList<>();
    
    private ClientSystemItemManager() {}
    
    public static ClientSystemItemManager getInstance() {
        if (instance == null) {
            instance = new ClientSystemItemManager();
        }
        return instance;
    }
    
    /**
     * 更新系统商品列表
     */
    public void updateSystemItems(List<SystemItem> items) {
        systemItems.clear();
        if (items != null) {
            systemItems.addAll(items);
        }
        TradeMod.getLogger().info("客户端系统商品列表已更新，共 {} 个商品", systemItems.size());
    }
    
    /**
     * 获取所有系统商品
     */
    public List<SystemItem> getAllSystemItems() {
        return new ArrayList<>(systemItems);
    }
    
    /**
     * 获取活跃的系统商品
     */
    public List<SystemItem> getActiveSystemItems() {
        return systemItems.stream()
                .filter(SystemItem::isActive)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 根据ID获取系统商品
     */
    public SystemItem getSystemItemById(UUID id) {
        return systemItems.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据ItemStack获取系统商品
     */
    public SystemItem getSystemItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        
        return systemItems.stream()
                .filter(item -> ItemStack.isSameItemSameTags(item.getItemStack(), itemStack))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 搜索系统商品
     */
    public List<SystemItem> searchSystemItems(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getActiveSystemItems();
        }
        
        String lowerSearchTerm = searchTerm.toLowerCase();
        return systemItems.stream()
                .filter(SystemItem::isActive)
                .filter(item -> {
                    String itemName = item.getItemStack().getHoverName().getString().toLowerCase();
                    return itemName.contains(lowerSearchTerm);
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 清空系统商品列表
     */
    public void clear() {
        systemItems.clear();
        TradeMod.getLogger().info("客户端系统商品列表已清空");
    }
    
    /**
     * 获取系统商品数量
     */
    public int getSystemItemCount() {
        return systemItems.size();
    }
    
    /**
     * 获取活跃系统商品数量
     */
    public int getActiveSystemItemCount() {
        return (int) systemItems.stream().filter(SystemItem::isActive).count();
    }
}