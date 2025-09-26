package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.client.gui.GuiManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打开交易界面数据包
 * 用于服务端通知客户端打开交易界面
 */
public class OpenTradeGuiPacket {
    
    /**
     * 构造函数
     */
    public OpenTradeGuiPacket() {
        // 空构造函数，这个数据包不需要携带数据
    }
    
    /**
     * 从缓冲区读取数据包
     */
    public OpenTradeGuiPacket(FriendlyByteBuf buf) {
        // 这个数据包不需要读取任何数据
    }
    
    /**
     * 将数据包写入缓冲区
     */
    public void toBytes(FriendlyByteBuf buf) {
        // 这个数据包不需要写入任何数据
    }
    
    /**
     * 处理数据包
     */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // 确保在客户端执行
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                GuiManager.openTradeMarket();
            });
        });
        return true;
    }
}