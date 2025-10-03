package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 测试数据包 - 用于验证客户端网络连接
 */
public class TestPacket {
    
    private final String message;
    
    public TestPacket(String message) {
        this.message = message;
    }
    
    public static void encode(TestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.message);
    }
    
    public static TestPacket decode(FriendlyByteBuf buffer) {
        return new TestPacket(buffer.readUtf());
    }
    
    public static void handle(TestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        TradeMod.getLogger().info("TestPacket.handle被调用 - 消息: {}, 接收方: {}", 
            packet.message, context.getDirection().getReceptionSide());
        
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                TradeMod.getLogger().info("TestPacket客户端接收到消息: {}", packet.message);
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    TradeMod.getLogger().info("TestPacket客户端代码执行成功！消息: {}", packet.message);
                });
            } else {
                TradeMod.getLogger().info("TestPacket服务端接收到消息: {}", packet.message);
            }
        });
        context.setPacketHandled(true);
    }
    
    public String getMessage() {
        return message;
    }
}