package com.tradesystem.mod.network;

import com.tradesystem.mod.data.TradeItem;
import com.tradesystem.mod.manager.ItemListingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 更新价格数据包
 * 用于处理玩家修改已上架商品价格的请求
 */
public class UpdatePricePacket {
    
    private final UUID itemId;
    private final int newPrice;
    
    public UpdatePricePacket(UUID itemId, int newPrice) {
        this.itemId = itemId;
        this.newPrice = newPrice;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(UpdatePricePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.itemId);
        buffer.writeInt(packet.newPrice);
    }
    
    /**
     * 解码数据包
     */
    public static UpdatePricePacket decode(FriendlyByteBuf buffer) {
        UUID itemId = buffer.readUUID();
        int newPrice = buffer.readInt();
        return new UpdatePricePacket(itemId, newPrice);
    }
    
    /**
     * 处理数据包
     */
    public static void handle(UpdatePricePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                handleUpdatePrice(player, packet.itemId, packet.newPrice);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 处理更新价格逻辑
     */
    private static void handleUpdatePrice(ServerPlayer player, UUID itemId, int newPrice) {
        try {
            // 验证新价格
            if (newPrice <= 0) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.invalid_price"));
                return;
            }
            
            // 使用ItemListingManager更新价格（带权限验证）
            boolean success = ItemListingManager.getInstance().updateItemPrice(itemId, newPrice, player.getUUID());
            
            if (success) {
                // 获取更新后的物品信息
                TradeItem tradeItem = ItemListingManager.getInstance().getAllActiveListings().stream()
                        .filter(item -> item.getId().equals(itemId))
                        .findFirst()
                        .orElse(null);
                
                if (tradeItem != null) {
                    // 触发数据同步到所有客户端
                    com.tradesystem.mod.data.DataService.getInstance().syncMarketDataToAllClients();
                    
                    // 发送成功消息
                    player.sendSystemMessage(Component.translatable("gui.tradesystem.message.price_updated_success",
                            tradeItem.getDisplayName(), newPrice));
                }
            } else {
                // 更新失败，可能是物品不存在或不是所有者
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.price_update_failed"));
            }
            
        } catch (Exception e) {
            // 记录错误并发送错误消息
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.price_update_error"));
            e.printStackTrace();
        }
    }
    
    // Getters
    public UUID getItemId() {
        return itemId;
    }
    
    public int getNewPrice() {
        return newPrice;
    }
}