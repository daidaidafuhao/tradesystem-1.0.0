package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 请求货币同步数据包
 * 客户端向服务器请求同步最新的金币数据
 */
public class RequestCurrencySyncPacket {
    
    public RequestCurrencySyncPacket() {
        // 空构造函数，因为这个包不需要携带数据
    }
    
    /**
     * 编码数据包
     */
    public static void encode(RequestCurrencySyncPacket packet, FriendlyByteBuf buffer) {
        // 不需要编码任何数据，因为这是一个简单的请求包
    }
    
    /**
     * 解码数据包
     */
    public static RequestCurrencySyncPacket decode(FriendlyByteBuf buffer) {
        return new RequestCurrencySyncPacket();
    }
    
    /**
     * 处理数据包
     */
    public static void handle(RequestCurrencySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                try {
                    // 同步玩家的金币数据到客户端
                    CurrencyUtil.syncCurrencyToClient(player);
                    TradeMod.getLogger().debug("为玩家 {} 同步金币数据", player.getName().getString());
                } catch (Exception e) {
                    TradeMod.getLogger().error("同步玩家 {} 的金币数据时出错: {}", 
                            player.getName().getString(), e.getMessage());
                }
            }
        });
        context.setPacketHandled(true);
    }
}