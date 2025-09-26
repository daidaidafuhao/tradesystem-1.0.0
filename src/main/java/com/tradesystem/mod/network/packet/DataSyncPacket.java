package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 数据同步网络包
 * 用于在客户端和服务器之间同步交易数据
 */
public class DataSyncPacket {
    
    public enum DataType {
        PLAYER_CURRENCY,    // 玩家货币数据
        MARKET_ITEMS,       // 市场物品数据
        TRADE_HISTORY,      // 交易历史数据
        SYSTEM_STATS        // 系统统计数据
    }
    
    private final DataType dataType;
    private final CompoundTag data;
    
    public DataSyncPacket(DataType dataType, CompoundTag data) {
        this.dataType = dataType;
        this.data = data;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(DataSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.dataType);
        buffer.writeNbt(packet.data);
    }
    
    /**
     * 解码数据包
     */
    public static DataSyncPacket decode(FriendlyByteBuf buffer) {
        DataType dataType = buffer.readEnum(DataType.class);
        CompoundTag data = buffer.readNbt();
        return new DataSyncPacket(dataType, data);
    }
    
    /**
     * 处理数据包
     */
    public static void handle(DataSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            
            if (context.getDirection().getReceptionSide().isClient()) {
                // 客户端接收数据
                handleClientSide(packet);
            } else {
                // 服务器接收数据
                handleServerSide(packet, player);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 客户端处理数据包
     */
    private static void handleClientSide(DataSyncPacket packet) {
        try {
            switch (packet.dataType) {
                case PLAYER_CURRENCY:
                    handlePlayerCurrencySync(packet.data);
                    break;
                case MARKET_ITEMS:
                    handleMarketItemsSync(packet.data);
                    break;
                case TRADE_HISTORY:
                    handleTradeHistorySync(packet.data);
                    break;
                case SYSTEM_STATS:
                    handleSystemStatsSync(packet.data);
                    break;
            }
        } catch (Exception e) {
            TradeMod.getLogger().error("客户端处理数据同步包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 服务器处理数据包
     */
    private static void handleServerSide(DataSyncPacket packet, Player player) {
        if (player == null) {
            TradeMod.getLogger().warn("服务器接收到无效玩家的数据同步包");
            return;
        }
        
        try {
            switch (packet.dataType) {
                case PLAYER_CURRENCY:
                    // 服务器通常不接受客户端的货币数据
                    TradeMod.getLogger().warn("玩家 {} 尝试同步货币数据到服务器", player.getName().getString());
                    break;
                default:
                    TradeMod.getLogger().debug("服务器接收到数据类型: {}", packet.dataType);
                    break;
            }
        } catch (Exception e) {
            TradeMod.getLogger().error("服务器处理数据同步包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 处理玩家货币数据同步
     */
    private static void handlePlayerCurrencySync(CompoundTag data) {
        try {
            int money = data.getInt("money");
            com.tradesystem.mod.client.ClientCurrencyManager.getInstance().setPlayerMoney(money);
            TradeMod.getLogger().debug("同步玩家货币数据: {}", money);
        } catch (Exception e) {
            TradeMod.getLogger().error("处理玩家货币同步时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 处理市场物品数据同步
     */
    private static void handleMarketItemsSync(CompoundTag data) {
        try {
            TradeMod.getLogger().info("接收到市场物品同步数据，共 {} 个物品", data.size());
            // 更新客户端的ItemListingManager缓存
            com.tradesystem.mod.manager.ItemListingManager.getInstance().updateClientCache(data);
            TradeMod.getLogger().info("客户端市场数据已更新");
        } catch (Exception e) {
            TradeMod.getLogger().error("处理市场物品同步时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 处理交易历史数据同步
     */
    private static void handleTradeHistorySync(CompoundTag data) {
        // TODO: 实现客户端交易历史更新
        TradeMod.getLogger().debug("同步交易历史数据");
    }
    
    /**
     * 处理系统统计数据同步
     */
    private static void handleSystemStatsSync(CompoundTag data) {
        // TODO: 实现客户端统计数据更新
        TradeMod.getLogger().debug("同步系统统计数据，总交易数: {}", data.getLong("total_trades"));
    }
    
    // Getters
    public DataType getDataType() {
        return dataType;
    }
    
    public CompoundTag getData() {
        return data;
    }
}