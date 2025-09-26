package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.client.gui.ItemManagementScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 下架响应数据包
 * 服务器向客户端发送下架操作的结果
 */
public class UnlistResponsePacket {
    
    private final UUID itemId;
    private final boolean success;
    private final String message;
    
    public UnlistResponsePacket(UUID itemId, boolean success, String message) {
        this.itemId = itemId;
        this.success = success;
        this.message = message;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(UnlistResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.itemId);
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.message);
    }
    
    /**
     * 解码数据包
     */
    public static UnlistResponsePacket decode(FriendlyByteBuf buffer) {
        UUID itemId = buffer.readUUID();
        boolean success = buffer.readBoolean();
        String message = buffer.readUtf();
        return new UnlistResponsePacket(itemId, success, message);
    }
    
    /**
     * 处理数据包（客户端）
     */
    public static void handle(UnlistResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            handleUnlistResponse(packet.itemId, packet.success, packet.message);
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 处理下架响应（客户端）
     */
    private static void handleUnlistResponse(UUID itemId, boolean success, String message) {
        // 在客户端处理下架响应
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ItemManagementScreen screen) {
            // 如果当前界面是商品管理界面，刷新数据
            screen.clearSelection();
            screen.refreshData();
        }
        
        // 显示消息给玩家
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.translatable(message));
        }
    }
    
    // Getters
    public UUID getItemId() {
        return itemId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
}