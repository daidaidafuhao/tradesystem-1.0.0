package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.data.DataService;
import com.tradesystem.mod.data.JsonDataManager;
import com.tradesystem.mod.data.TransactionRecord;
import com.tradesystem.mod.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 请求交易历史同步网络包
 * 客户端发送此包请求服务器同步交易历史数据
 */
public class RequestTradeHistorySyncPacket {
    
    private final UUID playerId;
    
    public RequestTradeHistorySyncPacket(UUID playerId) {
        this.playerId = playerId;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(RequestTradeHistorySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
    }
    
    /**
     * 解码数据包
     */
    public static RequestTradeHistorySyncPacket decode(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        return new RequestTradeHistorySyncPacket(playerId);
    }
    
    /**
     * 处理数据包
     */
    public static void handle(RequestTradeHistorySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getUUID().equals(packet.playerId)) {
                handleServerSide(packet, player);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 服务器端处理
     */
    private static void handleServerSide(RequestTradeHistorySyncPacket packet, ServerPlayer player) {
        try {
            TradeMod.getLogger().info("收到玩家 {} 的交易历史同步请求", player.getName().getString());
            
            // 从DataService获取交易历史数据
            DataService dataService = DataService.getInstance();
            if (dataService != null) {
                TradeMod.getLogger().info("使用DataService同步交易历史到玩家 {}", player.getName().getString());
                dataService.syncTransactionHistoryToPlayer(player);
            } else {
                TradeMod.getLogger().warn("DataService不可用，尝试使用JsonDataManager");
                // 如果DataService不可用，尝试从JsonDataManager获取
                JsonDataManager jsonDataManager = JsonDataManager.getInstance();
                if (jsonDataManager != null) {
                    List<TransactionRecord> history = jsonDataManager.getTransactionHistory();
                    
                    // 过滤出与该玩家相关的交易记录
                    List<TransactionRecord> playerHistory = history.stream()
                            .filter(record -> record.getSellerId().equals(packet.playerId) || 
                                            record.getBuyerId().equals(packet.playerId))
                            .collect(java.util.stream.Collectors.toList());
                    
                    // 转换为NBT数据并发送
                    CompoundTag historyData = new CompoundTag();
                    ListTag historyList = new ListTag();
                    
                    for (TransactionRecord record : playerHistory) {
                        CompoundTag recordTag = new CompoundTag();
                        recordTag.putUUID("transaction_id", record.getTransactionId());
                        recordTag.putUUID("seller_id", record.getSellerId());
                        recordTag.putString("seller_name", record.getSellerName());
                        recordTag.putUUID("buyer_id", record.getBuyerId());
                        recordTag.putString("buyer_name", record.getBuyerName());
                        
                        // 调试物品数据
                        net.minecraft.world.item.ItemStack itemStack = record.getItemStack();
                        CompoundTag itemTag = itemStack.save(new CompoundTag());
                        TradeMod.getLogger().info("服务端物品数据 - 物品: {}, 数量: {}, NBT: {}", 
                            itemStack.getItem().toString(), itemStack.getCount(), itemTag.toString());
                        
                        recordTag.put("item", itemTag);
                        recordTag.putInt("price", record.getPrice());
                        recordTag.putLong("timestamp", record.getTimestamp());
                        recordTag.putString("type", record.getType().name());
                        
                        historyList.add(recordTag);
                    }
                    
                    historyData.put("history", historyList);
                    historyData.putInt("count", playerHistory.size());
                    
                    // 发送测试包验证网络连接
                    TestPacket testPacket = new TestPacket("交易历史测试消息");
                    NetworkHandler.sendToPlayer(testPacket, player);
                    
                    // 发送同步包
                    DataSyncPacket syncPacket = new DataSyncPacket(DataSyncPacket.DataType.TRADE_HISTORY, historyData);
                    TradeMod.getLogger().info("准备发送DataSyncPacket到玩家 {}", player.getName().getString());
                    NetworkHandler.sendToPlayer(syncPacket, player);
                    
                    TradeMod.getLogger().info("已向玩家 {} 同步 {} 条交易历史记录", 
                            player.getName().getString(), playerHistory.size());
                }
            }
            
        } catch (Exception e) {
            TradeMod.getLogger().error("处理交易历史同步请求时出错: {}", e.getMessage(), e);
        }
    }
    
    // Getter
    public UUID getPlayerId() {
        return playerId;
    }
}