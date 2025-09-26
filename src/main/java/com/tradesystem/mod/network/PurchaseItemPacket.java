package com.tradesystem.mod.network;

import com.tradesystem.mod.data.TradeManager;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 购买物品数据包
 */
public class PurchaseItemPacket {
    private final UUID tradeItemId;
    private final int quantity;

    public PurchaseItemPacket(UUID tradeItemId) {
        this.tradeItemId = tradeItemId;
        this.quantity = 1; // 默认数量为1，保持向后兼容
    }
    
    public PurchaseItemPacket(UUID tradeItemId, int quantity) {
        this.tradeItemId = tradeItemId;
        this.quantity = quantity;
    }

    public static void encode(PurchaseItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.tradeItemId);
        buffer.writeInt(packet.quantity);
    }

    public static PurchaseItemPacket decode(FriendlyByteBuf buffer) {
        UUID tradeItemId = buffer.readUUID();
        int quantity = buffer.readInt();
        return new PurchaseItemPacket(tradeItemId, quantity);
    }

    public static void handle(PurchaseItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                purchaseItem(packet.tradeItemId, player, packet.quantity);
            }
        });
        context.setPacketHandled(true);
    }

    private static void purchaseItem(UUID tradeItemId, ServerPlayer player, int quantity) {
        try {
            boolean success = TradeManager.getInstance().purchaseItem(tradeItemId, player, quantity);
            
            if (success) {
                player.sendSystemMessage(Component.translatable("message.tradesystem.purchase.success"));
            } else {
                player.sendSystemMessage(Component.translatable("message.tradesystem.purchase.failed"));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("message.tradesystem.purchase.error"));
        }
    }
}