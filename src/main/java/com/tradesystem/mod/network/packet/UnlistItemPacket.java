package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.manager.ItemListingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 下架物品数据包
 * 用于处理玩家下架交易物品的请求
 */
public class UnlistItemPacket {
    
    private final UUID itemId;
    
    public UnlistItemPacket(UUID itemId) {
        this.itemId = itemId;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(UnlistItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.itemId);
    }
    
    /**
     * 解码数据包
     */
    public static UnlistItemPacket decode(FriendlyByteBuf buffer) {
        UUID itemId = buffer.readUUID();
        return new UnlistItemPacket(itemId);
    }
    
    /**
     * 处理数据包
     */
    public static void handle(UnlistItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                handleUnlistItem(player, packet.itemId);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 处理下架物品请求
     */
    private static void handleUnlistItem(ServerPlayer player, UUID itemId) {
        try {
            boolean success = ItemListingManager.getInstance().unlistItem(player, itemId);
            String message;
            
            if (success) {
                message = "gui.tradesystem.message.unlist_success";
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(message));
            } else {
                message = "gui.tradesystem.message.unlist_failed";
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(message));
            }
            
            // 发送响应包给客户端，通知刷新界面
            com.tradesystem.mod.network.packet.UnlistResponsePacket responsePacket = 
                new com.tradesystem.mod.network.packet.UnlistResponsePacket(itemId, success, message);
            com.tradesystem.mod.network.NetworkHandler.sendToPlayer(responsePacket, player);
            
        } catch (Exception e) {
            com.tradesystem.mod.TradeMod.getLogger().error("处理下架物品请求时出错: {}", e.getMessage());
            String errorMessage = "gui.tradesystem.message.unlist_error";
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(errorMessage));
            
            // 发送错误响应包
            com.tradesystem.mod.network.packet.UnlistResponsePacket responsePacket = 
                new com.tradesystem.mod.network.packet.UnlistResponsePacket(itemId, false, errorMessage);
            com.tradesystem.mod.network.NetworkHandler.sendToPlayer(responsePacket, player);
        }
    }
    
    // Getter
    public UUID getItemId() {
        return itemId;
    }
}