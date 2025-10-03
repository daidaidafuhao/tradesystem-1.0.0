package com.tradesystem.mod.network;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.network.packet.DataSyncPacket;
import com.tradesystem.mod.network.packet.OpenTradeGuiPacket;
import com.tradesystem.mod.network.packet.RequestCurrencySyncPacket;
import com.tradesystem.mod.network.packet.RequestTradeHistorySyncPacket;
import com.tradesystem.mod.network.packet.TestPacket;
import com.tradesystem.mod.network.packet.UnlistItemPacket;
import com.tradesystem.mod.network.ListItemPacket;
import com.tradesystem.mod.network.RecycleItemPacket;
import com.tradesystem.mod.network.PurchaseItemPacket;
import com.tradesystem.mod.network.UpdatePricePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络处理器
 * 负责管理客户端和服务器之间的网络通信
 */
public class NetworkHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryParse(TradeMod.MODID + ":main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    /**
     * 注册网络包
     */
    public static void registerPackets() {
        TradeMod.getLogger().info("注册网络包...");
        
        // 注册数据同步包 - 添加调试信息
        TradeMod.getLogger().info("注册DataSyncPacket，ID: {}", packetId);
        INSTANCE.registerMessage(nextId(), DataSyncPacket.class, 
                DataSyncPacket::encode, DataSyncPacket::decode, DataSyncPacket::handle);
        
        // 注册测试包
        TradeMod.getLogger().info("注册TestPacket，ID: {}", packetId);
        INSTANCE.registerMessage(nextId(), TestPacket.class,
                TestPacket::encode, TestPacket::decode, TestPacket::handle);
        
        // 注册请求金币同步包
        INSTANCE.registerMessage(nextId(), RequestCurrencySyncPacket.class,
                RequestCurrencySyncPacket::encode, RequestCurrencySyncPacket::decode, RequestCurrencySyncPacket::handle);
        
        // 注册请求交易历史同步包
        INSTANCE.registerMessage(nextId(), RequestTradeHistorySyncPacket.class,
                RequestTradeHistorySyncPacket::encode, RequestTradeHistorySyncPacket::decode, RequestTradeHistorySyncPacket::handle);
        
        // 注册打开交易界面包
        INSTANCE.registerMessage(nextId(), OpenTradeGuiPacket.class,
                OpenTradeGuiPacket::toBytes, OpenTradeGuiPacket::new, OpenTradeGuiPacket::handle);
        
        // 注册上架物品包
        INSTANCE.registerMessage(nextId(), ListItemPacket.class,
                ListItemPacket::encode, ListItemPacket::decode, ListItemPacket::handle);
        
        // 注册回收物品包
        INSTANCE.registerMessage(nextId(), RecycleItemPacket.class,
                RecycleItemPacket::encode, RecycleItemPacket::decode, RecycleItemPacket::handle);
        
        // 注册购买物品包
        INSTANCE.registerMessage(nextId(), PurchaseItemPacket.class,
                PurchaseItemPacket::encode, PurchaseItemPacket::decode, PurchaseItemPacket::handle);
        
        // 注册更新价格包
        INSTANCE.registerMessage(nextId(), UpdatePricePacket.class,
                UpdatePricePacket::encode, UpdatePricePacket::decode, UpdatePricePacket::handle);
        
        // 注册下架物品包
        INSTANCE.registerMessage(nextId(), UnlistItemPacket.class,
                UnlistItemPacket::encode, UnlistItemPacket::decode, UnlistItemPacket::handle);
        
        // 注册下架响应包
        INSTANCE.registerMessage(nextId(), com.tradesystem.mod.network.packet.UnlistResponsePacket.class,
                com.tradesystem.mod.network.packet.UnlistResponsePacket::encode, 
                com.tradesystem.mod.network.packet.UnlistResponsePacket::decode, 
                com.tradesystem.mod.network.packet.UnlistResponsePacket::handle);
        
        TradeMod.getLogger().info("网络包注册完成，已注册 {} 个包", packetId);
    }
    
    /**
     * 发送数据包到玩家
     */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        TradeMod.getLogger().info("发送数据包到玩家 {}: {}", player.getName().getString(), packet.getClass().getSimpleName());
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * 发送数据包到服务器
     */
    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 获取下一个包ID
     */
    private static int nextId() {
        return packetId++;
    }
}