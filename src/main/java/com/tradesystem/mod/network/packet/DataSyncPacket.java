package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
        TradeMod.getLogger().info("DataSyncPacket.handle被调用 - 数据类型: {}, 接收方: {}", 
            packet.dataType, context.getDirection().getReceptionSide());
        
        context.enqueueWork(() -> {
            TradeMod.getLogger().info("DataSyncPacket开始处理工作队列 - 数据类型: {}", packet.dataType);
            
            if (context.getDirection().getReceptionSide().isClient()) {
                TradeMod.getLogger().info("检测到客户端接收，准备执行客户端代码");
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    TradeMod.getLogger().info("DistExecutor客户端代码开始执行");
                    handleClientSide(packet);
                    TradeMod.getLogger().info("DistExecutor客户端代码执行完成");
                });
            } else {
                TradeMod.getLogger().info("检测到服务端接收，执行服务端处理");
                handleServerSide(packet, context.getSender());
            }
        });
        context.setPacketHandled(true);
        TradeMod.getLogger().info("DataSyncPacket处理完成，数据类型: {}", packet.dataType);
    }
    
    /**
     * 客户端处理数据包
     */
    private static void handleClientSide(DataSyncPacket packet) {
        TradeMod.getLogger().info("=== handleClientSide 开始执行 ===");
        TradeMod.getLogger().info("处理数据类型: {}", packet.dataType);
        
        try {
            switch (packet.dataType) {
                case PLAYER_CURRENCY:
                    TradeMod.getLogger().info("处理玩家货币同步");
                    handlePlayerCurrencySync(packet.data);
                    break;
                case MARKET_ITEMS:
                    TradeMod.getLogger().info("处理市场物品同步");
                    handleMarketItemsSync(packet.data);
                    break;
                case TRADE_HISTORY:
                    TradeMod.getLogger().info("处理交易历史同步");
                    handleTradeHistorySync(packet.data);
                    break;
                case SYSTEM_STATS:
                    TradeMod.getLogger().info("处理系统统计同步");
                    handleSystemStatsSync(packet.data);
                    break;
                default:
                    TradeMod.getLogger().warn("未知的数据类型: {}", packet.dataType);
            }
            TradeMod.getLogger().info("=== handleClientSide 执行完成 ===");
        } catch (Exception e) {
            TradeMod.getLogger().error("客户端处理数据同步包时出错: {}", e.getMessage(), e);
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
        try {
            TradeMod.getLogger().info("客户端开始处理交易历史同步数据");
            
            if (data.contains("history") && data.contains("count")) {
                net.minecraft.nbt.ListTag historyList = data.getList("history", 10); // 10 = CompoundTag type
                int count = data.getInt("count");
                
                TradeMod.getLogger().info("接收到 {} 条交易历史记录", count);
                
                // 转换NBT数据为TransactionRecord列表
                java.util.List<com.tradesystem.mod.client.gui.MyTradesScreen.TransactionRecord> records = new java.util.ArrayList<>();
                
                for (int i = 0; i < historyList.size(); i++) {
                    CompoundTag recordTag = historyList.getCompound(i);
                    
                    TradeMod.getLogger().info("处理第 {} 条记录", i + 1);
                    
                    try {
                        // 从NBT数据构建TransactionRecord
                        CompoundTag itemStackTag = recordTag.getCompound("item");
                        net.minecraft.world.item.ItemStack itemStack = net.minecraft.world.item.ItemStack.of(itemStackTag);
                        
                        TradeMod.getLogger().info("物品数据: {}, 物品类型: {}, 数量: {}", 
                            itemStackTag.toString(), itemStack.getItem().toString(), itemStack.getCount());
                        
                        com.tradesystem.mod.client.gui.MyTradesScreen.TransactionRecord record = 
                            new com.tradesystem.mod.client.gui.MyTradesScreen.TransactionRecord(
                                recordTag.getUUID("transaction_id"),
                                recordTag.getUUID("seller_id"),
                                recordTag.getString("seller_name"),
                                recordTag.getUUID("buyer_id"),
                                recordTag.getString("buyer_name"),
                                itemStack,
                                recordTag.getInt("price"),
                                recordTag.getLong("timestamp"),
                                com.tradesystem.mod.client.gui.MyTradesScreen.TransactionRecord.Type.valueOf(recordTag.getString("type"))
                            );
                    
                        records.add(record);
                    } catch (Exception e) {
                        TradeMod.getLogger().error("解析记录 {} 时发生错误: {}", i + 1, e.getMessage());
                        continue;
                    }
                }
                
                TradeMod.getLogger().info("成功转换 {} 条交易记录", records.size());
                
                // 更新MyTradesScreen的交易历史
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.screen instanceof com.tradesystem.mod.client.gui.MyTradesScreen) {
                    com.tradesystem.mod.client.gui.MyTradesScreen screen = (com.tradesystem.mod.client.gui.MyTradesScreen) minecraft.screen;
                    TradeMod.getLogger().info("找到MyTradesScreen实例，开始更新交易历史");
                    screen.updateTransactionHistory(records);
                    TradeMod.getLogger().info("交易历史更新完成");
                } else {
                    TradeMod.getLogger().warn("当前屏幕不是MyTradesScreen实例，无法更新交易历史。当前屏幕: {}", 
                        minecraft.screen != null ? minecraft.screen.getClass().getSimpleName() : "null");
                }
                
                TradeMod.getLogger().info("客户端已接收并更新 {} 条交易历史记录", count);
                
            } else {
                TradeMod.getLogger().warn("接收到的交易历史数据格式不正确");
            }
        } catch (Exception e) {
            TradeMod.getLogger().error("处理交易历史同步时出错: {}", e.getMessage(), e);
        }
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