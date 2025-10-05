package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.manager.SystemItemManager;
import com.tradesystem.mod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 购买系统商品数据包
 * 用于处理玩家购买系统商品的请求
 */
public class PurchaseSystemItemPacket {
    
    private final UUID itemId;
    private final int quantity;
    
    public PurchaseSystemItemPacket(UUID itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(PurchaseSystemItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.itemId);
        buffer.writeInt(packet.quantity);
    }
    
    /**
     * 解码数据包
     */
    public static PurchaseSystemItemPacket decode(FriendlyByteBuf buffer) {
        UUID itemId = buffer.readUUID();
        int quantity = buffer.readInt();
        return new PurchaseSystemItemPacket(itemId, quantity);
    }
    
    /**
     * 处理数据包
     */
    public static void handle(PurchaseSystemItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                handleServerSide(packet, player);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 服务端处理逻辑
     */
    private static void handleServerSide(PurchaseSystemItemPacket packet, ServerPlayer player) {
        try {
            SystemItemManager manager = SystemItemManager.getInstance();
            
            // 验证参数
            if (packet.itemId == null || packet.quantity <= 0) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.invalid_parameters"));
                return;
            }
            
            // 尝试购买系统商品
            boolean success = manager.purchaseSystemItem(player, packet.itemId, packet.quantity);
            
            if (success) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.purchase_success"));
                TradeMod.getLogger().info("玩家 {} 成功购买系统商品 {} x{}", 
                    player.getName().getString(), packet.itemId, packet.quantity);
                
                // 购买成功后，同步系统商品数据到所有客户端
                SystemItemSyncPacket syncPacket = new SystemItemSyncPacket(manager.getAllSystemItems());
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), syncPacket);
                TradeMod.getLogger().info("系统商品购买成功，已同步数据到所有客户端");
                
            } else {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.purchase_failed"));
                TradeMod.getLogger().warn("玩家 {} 购买系统商品失败 {} x{}", 
                    player.getName().getString(), packet.itemId, packet.quantity);
            }
            
        } catch (Exception e) {
            TradeMod.getLogger().error("处理购买系统商品请求时发生错误", e);
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.operation_failed"));
        }
    }
}