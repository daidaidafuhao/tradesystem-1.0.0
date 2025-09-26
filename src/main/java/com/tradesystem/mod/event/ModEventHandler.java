package com.tradesystem.mod.event;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.capability.ModCapabilities;
import com.tradesystem.mod.capability.PlayerCurrencyProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * MOD事件处理器
 * 处理玩家相关事件和Capability附加
 */
@Mod.EventBusSubscriber(modid = TradeMod.MODID)
public class ModEventHandler {
    
    /**
     * 为玩家实体附加Capability
     */
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(ModCapabilities.PLAYER_CURRENCY).isPresent()) {
                event.addCapability(ModCapabilities.PLAYER_CURRENCY_LOCATION, new PlayerCurrencyProvider());
            }
        }
    }
    
    /**
     * 玩家登录事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            
            // 检查并处理离线期间收到的金币
            int offlineCurrency = com.tradesystem.mod.data.TradeDataManager.getInstance().getOfflinePlayerCurrency(playerId);
            if (offlineCurrency > 0) {
                // 将离线金币添加到玩家账户
                com.tradesystem.mod.util.CurrencyUtil.addPlayerMoney(player, offlineCurrency);
                // 清除离线金币记录
                com.tradesystem.mod.data.TradeDataManager.getInstance().clearOfflinePlayerCurrency(playerId);
                TradeMod.getLogger().info("玩家 {} 登录时收到离线金币: {}", player.getName().getString(), offlineCurrency);
                
                // 通知玩家收到离线金币
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "gui.tradesystem.message.offline_currency_received", 
                    com.tradesystem.mod.util.CurrencyUtil.formatMoney(offlineCurrency)));
            }
            
            // 从持久化存储加载玩家金币
            int savedCurrency = com.tradesystem.mod.util.CurrencyUtil.loadPlayerCurrencyFromDisk(player);
            
            player.getCapability(ModCapabilities.PLAYER_CURRENCY).ifPresent(currency -> {
                // 设置从磁盘加载的金币数量
                currency.setMoney(savedCurrency);
                
                TradeMod.getLogger().info("玩家 {} 登录，从磁盘加载金币: {}", player.getName().getString(), savedCurrency);
                
                // 如果玩家金币为0或异常，重置为初始金币
                if (currency.getMoney() < 0) {
                    TradeMod.getLogger().warn("玩家 {} 金币异常 ({}), 重置为初始金币", player.getName().getString(), currency.getMoney());
                    currency.setMoney(com.tradesystem.mod.config.TradeConfig.initialPlayerMoney);
                    // 立即保存修正后的金币
                    com.tradesystem.mod.util.CurrencyUtil.savePlayerCurrencyToDisk(player, currency.getMoney());
                }
                
                // 同步金币到客户端
                com.tradesystem.mod.util.CurrencyUtil.syncCurrencyToClient(player);
            });
            
            // 同步市场数据到新登录的玩家
            com.tradesystem.mod.data.DataService.getInstance().syncMarketDataToPlayer(player);
            TradeMod.getLogger().debug("已为玩家 {} 同步市场数据", player.getName().getString());
        }
    }
    
    /**
     * 玩家登出事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 保存玩家金币到持久化存储
            player.getCapability(ModCapabilities.PLAYER_CURRENCY).ifPresent(currency -> {
                com.tradesystem.mod.util.CurrencyUtil.savePlayerCurrencyToDisk(player, currency.getMoney());
                TradeMod.getLogger().info("玩家 {} 登出，已保存金币: {}", player.getName().getString(), currency.getMoney());
            });
        }
    }
    
    /**
     * 玩家克隆事件（死亡重生时保持数据）
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            TradeMod.getLogger().info("玩家 {} 死亡重生，正在保持金币数据", event.getEntity().getName().getString());
            
            // 先从磁盘加载玩家的真实金币数据
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                int savedMoney = com.tradesystem.mod.util.CurrencyUtil.loadPlayerCurrencyFromDisk(serverPlayer);
                TradeMod.getLogger().info("从磁盘加载玩家 {} 死亡前的金币: {}", serverPlayer.getName().getString(), savedMoney);
                
                // 直接设置从磁盘加载的金币数据
                event.getEntity().getCapability(ModCapabilities.PLAYER_CURRENCY).ifPresent(newCurrency -> {
                    newCurrency.setMoney(savedMoney);
                    TradeMod.getLogger().info("玩家 {} 死亡重生后金币设置为: {}", serverPlayer.getName().getString(), savedMoney);
                    
                    // 立即保存到磁盘确保数据一致性
                    com.tradesystem.mod.util.CurrencyUtil.savePlayerCurrencyToDisk(serverPlayer, savedMoney);
                    
                    // 立即同步到客户端
                    com.tradesystem.mod.util.CurrencyUtil.syncCurrencyToClient(serverPlayer);
                });
            } else {
                // 如果不是服务端玩家，使用原来的逻辑作为备用
                event.getOriginal().getCapability(ModCapabilities.PLAYER_CURRENCY).ifPresent(oldCurrency -> {
                    event.getEntity().getCapability(ModCapabilities.PLAYER_CURRENCY).ifPresent(newCurrency -> {
                        int oldMoney = oldCurrency.getMoney();
                        newCurrency.deserializeNBT(oldCurrency.serializeNBT());
                        int newMoney = newCurrency.getMoney();
                        
                        TradeMod.getLogger().info("玩家 {} 死亡前金币: {}, 死亡后金币: {}", 
                                event.getEntity().getName().getString(), oldMoney, newMoney);
                        
                        // 确保数据正确传输
                        if (newMoney != oldMoney) {
                            TradeMod.getLogger().warn("金币数据传输异常，手动设置金币数量");
                            newCurrency.setMoney(oldMoney);
                        }
                    });
                });
            }
        }
    }
}