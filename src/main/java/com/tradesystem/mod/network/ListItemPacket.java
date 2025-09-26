package com.tradesystem.mod.network;

import com.tradesystem.mod.manager.ItemListingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 上架物品数据包
 * 用于处理玩家上架物品的请求
 */
public class ListItemPacket {
    private final int slotIndex;
    private final double price;
    private final int quantity;
    
    public ListItemPacket(int slotIndex, double price, int quantity) {
        this.slotIndex = slotIndex;
        this.price = price;
        this.quantity = quantity;
    }
    
    // 兼容旧版本的构造函数
    public ListItemPacket(int slotIndex, double price) {
        this(slotIndex, price, 1);
    }
    
    /**
     * 编码数据包
     */
    public static void encode(ListItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.slotIndex);
        buffer.writeDouble(packet.price);
        buffer.writeInt(packet.quantity);
    }
    
    /**
     * 解码数据包
     */
    public static ListItemPacket decode(FriendlyByteBuf buffer) {
        int slotIndex = buffer.readInt();
        double price = buffer.readDouble();
        int quantity = buffer.readInt();
        return new ListItemPacket(slotIndex, price, quantity);
    }
    
    /**
     * 处理数据包
     */
    public static void handle(ListItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                try {
                    // 获取指定槽位的物品
                    if (packet.slotIndex >= 0 && packet.slotIndex < player.getInventory().getContainerSize()) {
                        ItemStack itemStack = player.getInventory().getItem(packet.slotIndex);
                        
                        if (!itemStack.isEmpty() && packet.price > 0 && packet.quantity > 0) {
                            // 检查玩家是否有足够的物品
                            if (itemStack.getCount() < packet.quantity) {
                                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.insufficient_items"));
                                return;
                            }
                            
                            // 创建指定数量的物品副本
                            ItemStack itemCopy = itemStack.copy();
                            itemCopy.setCount(packet.quantity);
                            
                            // 尝试上架物品
                            if (ItemListingManager.getInstance().listItem(player, itemCopy, (int)packet.price)) {
                                // 上架成功，从玩家背包中移除指定数量的物品
                                itemStack.shrink(packet.quantity);
                                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.list_success"));
                            } else {
                                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.list_failed"));
                            }
                        } else {
                            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.invalid_item_or_price"));
                        }
                    } else {
                        player.sendSystemMessage(Component.translatable("gui.tradesystem.message.invalid_slot"));
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.translatable("gui.tradesystem.message.list_error"));
                }
            }
        });
        context.setPacketHandled(true);
    }
}