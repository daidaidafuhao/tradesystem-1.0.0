package com.tradesystem.mod.util;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.capability.IPlayerCurrency;
import com.tradesystem.mod.capability.ModCapabilities;
import com.tradesystem.mod.config.TradeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;

import java.util.UUID;

/**
 * 货币工具类
 * 提供便捷的货币操作方法
 */
public class CurrencyUtil {
    
    /**
     * 获取玩家的货币Capability
     */
    public static LazyOptional<IPlayerCurrency> getPlayerCurrency(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_CURRENCY);
    }
    
    /**
     * 获取玩家当前金币数量
     */
    public static int getPlayerMoney(Player player) {
        return getPlayerCurrency(player).map(IPlayerCurrency::getMoney).orElse(TradeConfig.initialPlayerMoney);
    }
    
    /**
     * 设置玩家金币数量
     */
    public static boolean setPlayerMoney(Player player, int amount) {
        return getPlayerCurrency(player).map(currency -> {
            currency.setMoney(amount);
            
            // 立即保存到持久化存储
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                savePlayerCurrencyToDisk(serverPlayer, amount);
                // 同步到客户端
                syncCurrencyToClient(serverPlayer);
            }
            
            return true;
        }).orElse(false);
    }
    
    /**
     * 给玩家添加金币（带税收钩子）
     */
    public static boolean addPlayerMoney(Player player, int amount) {
        // 税收钩子 - 可以在这里添加税收逻辑
        int finalAmount = applyTaxHook(player, amount, TransactionType.INCOME);
        
        boolean result = getPlayerCurrency(player).map(currency -> {
            int oldAmount = currency.getMoney();
            boolean success = currency.addMoney(finalAmount);
            if (success && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                // 立即保存到持久化存储
                savePlayerCurrencyToDisk(serverPlayer, currency.getMoney());
                
                // 立即同步到客户端
                syncCurrencyToClient(serverPlayer);
                // 发送金币变化消息
                sendMoneyMessage(serverPlayer, oldAmount, currency.getMoney());
            }
            return success;
        }).orElse(false);
        
        return result;
    }
    
    /**
     * 从玩家扣除金币（带税收钩子）
     */
    public static boolean removePlayerMoney(Player player, int amount) {
        // 税收钩子 - 可以在这里添加税收逻辑
        int finalAmount = applyTaxHook(player, amount, TransactionType.EXPENSE);
        
        boolean result = getPlayerCurrency(player).map(currency -> {
            int oldAmount = currency.getMoney();
            boolean success = currency.removeMoney(finalAmount);
            if (success && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                // 立即保存到持久化存储
                savePlayerCurrencyToDisk(serverPlayer, currency.getMoney());
                
                // 立即同步到客户端
                syncCurrencyToClient(serverPlayer);
                // 发送金币变化消息
                sendMoneyMessage(serverPlayer, oldAmount, currency.getMoney());
            }
            return success;
        }).orElse(false);
        
        return result;
    }
    
    /**
     * 检查玩家是否有足够的金币
     */
    public static boolean hasPlayerMoney(Player player, int amount) {
        return getPlayerCurrency(player).map(currency -> currency.hasMoney(amount)).orElse(false);
    }
    
    /**
     * 重置玩家金币到初始值
     */
    public static boolean resetPlayerMoney(Player player) {
        return getPlayerCurrency(player).map(currency -> {
            currency.reset();
            return true;
        }).orElse(false);
    }
    
    /**
     * 玩家间转账
     */
    public static boolean transferMoney(Player from, Player to, int amount) {
        if (amount <= 0) {
            return false;
        }
        
        if (!hasPlayerMoney(from, amount)) {
            return false;
        }
        
        if (removePlayerMoney(from, amount)) {
            if (addPlayerMoney(to, amount)) {
                return true;
            } else {
                // 如果给接收方添加金币失败，回滚操作
                addPlayerMoney(from, amount);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * 格式化金币数量显示
     */
    public static String formatMoney(int amount) {
        return String.format("%,d %s", amount, "金币");
    }
    
    /**
     * 创建金币数量的文本组件
     */
    public static Component getMoneyComponent(int amount) {
        return Component.literal(formatMoney(amount));
    }
    
    /**
     * 交易类型枚举
     */
    public enum TransactionType {
        INCOME,   // 收入
        EXPENSE   // 支出
    }
    
    /**
     * 税收钩子 - 可以根据配置调整金额
     */
    private static int applyTaxHook(Player player, int amount, TransactionType type) {
        // 这里可以根据配置文件或其他逻辑调整金额
        // 例如：收入时扣除税收，支出时添加手续费等
        
        // 目前直接返回原金额，为以后的税收系统预留接口
        return amount;
    }
    
    /**
     * 向玩家发送金币变化消息
     */
    public static void sendMoneyMessage(ServerPlayer player, int oldAmount, int newAmount) {
        int change = newAmount - oldAmount;
        String changeText;
        
        if (change > 0) {
            changeText = String.format("§a+%s", formatMoney(change));
        } else if (change < 0) {
            changeText = String.format("§c%s", formatMoney(change));
        } else {
            return; // 没有变化，不发送消息
        }
        
        Component message = Component.literal(String.format("金币变化: %s (当前: %s)", 
                changeText, formatMoney(newAmount)));
        player.sendSystemMessage(message);
    }
    
    /**
     * 检查金币数量是否有效
     */
    public static boolean isValidAmount(int amount) {
        return amount >= 0 && amount <= 10000000; // 使用硬编码的最大值
    }
    
    /**
     * 保存玩家金币到持久化存储
     */
    public static void savePlayerCurrencyToDisk(net.minecraft.server.level.ServerPlayer player, int amount) {
        try {
            UUID playerId = player.getUUID();
            
            // 保存到JsonDataManager
            com.tradesystem.mod.data.JsonDataManager.getInstance().setPlayerCurrency(playerId, amount);
            
            TradeMod.getLogger().debug("已保存玩家 {} 的金币到磁盘: {}", player.getName().getString(), amount);
        } catch (Exception e) {
            TradeMod.getLogger().error("保存玩家金币到磁盘失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从持久化存储加载玩家金币
     */
    public static int loadPlayerCurrencyFromDisk(net.minecraft.server.level.ServerPlayer player) {
        try {
            UUID playerId = player.getUUID();
            int amount = com.tradesystem.mod.data.JsonDataManager.getInstance().getPlayerCurrency(playerId);
            
            TradeMod.getLogger().debug("从磁盘加载玩家 {} 的金币: {}", player.getName().getString(), amount);
            return amount;
        } catch (Exception e) {
            TradeMod.getLogger().error("从磁盘加载玩家金币失败: {}", e.getMessage(), e);
            return TradeConfig.initialPlayerMoney;
        }
    }
    
    /**
     * 同步玩家金币到客户端
     */
    public static void syncCurrencyToClient(net.minecraft.server.level.ServerPlayer player) {
        getPlayerCurrency(player).ifPresent(currency -> {
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putInt("money", currency.getMoney());
            
            com.tradesystem.mod.network.packet.DataSyncPacket packet = 
                new com.tradesystem.mod.network.packet.DataSyncPacket(
                    com.tradesystem.mod.network.packet.DataSyncPacket.DataType.PLAYER_CURRENCY, 
                    data
                );
            
            com.tradesystem.mod.network.NetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), 
                packet
            );
        });
    }
}