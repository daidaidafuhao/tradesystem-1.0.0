package com.tradesystem.mod.network;

import com.tradesystem.mod.data.TradeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 下架物品数据包
 */
public class UnlistItemPacket {
    private final UUID tradeItemId;

    public UnlistItemPacket(UUID tradeItemId) {
        this.tradeItemId = tradeItemId;
    }

    public static void encode(UnlistItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.tradeItemId);
    }

    public static UnlistItemPacket decode(FriendlyByteBuf buffer) {
        return new UnlistItemPacket(buffer.readUUID());
    }

    public static void handle(UnlistItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                unlistItem(packet.tradeItemId, player);
            }
        });
        context.setPacketHandled(true);
    }

    private static void unlistItem(UUID tradeItemId, ServerPlayer player) {
        try {
            boolean success = TradeManager.getInstance().unlistItem(tradeItemId, player);
            
            if (success) {
                player.sendSystemMessage(Component.translatable("message.tradesystem.unlist.success"));
            } else {
                player.sendSystemMessage(Component.translatable("message.tradesystem.unlist.failed"));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("message.tradesystem.unlist.error"));
        }
    }
}