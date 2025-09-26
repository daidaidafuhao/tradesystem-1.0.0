package com.tradesystem.mod.network;

import com.tradesystem.mod.manager.RecycleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 回收物品数据包
 * 用于处理玩家回收物品的请求
 */
public class RecycleItemPacket {
    private final ItemStack itemStack;
    private final int count;
    private final java.util.List<Integer> slotIndices;
    
    public RecycleItemPacket(ItemStack itemStack, int count) {
        this.itemStack = itemStack;
        this.count = count;
        this.slotIndices = null;
    }
    
    public RecycleItemPacket(java.util.List<Integer> slotIndices) {
        this.itemStack = ItemStack.EMPTY;
        this.count = 0;
        this.slotIndices = slotIndices;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(RecycleItemPacket packet, FriendlyByteBuf buffer) {
        if (packet.slotIndices != null) {
            buffer.writeBoolean(true); // 标记为槽位索引模式
            buffer.writeInt(packet.slotIndices.size());
            for (Integer index : packet.slotIndices) {
                buffer.writeInt(index);
            }
        } else {
            buffer.writeBoolean(false); // 标记为物品模式
            buffer.writeItem(packet.itemStack);
            buffer.writeInt(packet.count);
        }
    }
    
    /**
     * 解码数据包
     */
    public static RecycleItemPacket decode(FriendlyByteBuf buffer) {
        boolean isSlotMode = buffer.readBoolean();
        if (isSlotMode) {
            int size = buffer.readInt();
            java.util.List<Integer> slotIndices = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                slotIndices.add(buffer.readInt());
            }
            return new RecycleItemPacket(slotIndices);
        } else {
            ItemStack itemStack = buffer.readItem();
            int count = buffer.readInt();
            return new RecycleItemPacket(itemStack, count);
        }
    }
    
    /**
     * 处理数据包
     */
    public static void handle(RecycleItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                try {
                    if (packet.slotIndices != null) {
                        // 槽位索引模式 - 回收指定槽位的物品
                        boolean success = true;
                        for (Integer slotIndex : packet.slotIndices) {
                            if (slotIndex >= 0 && slotIndex < player.getInventory().getContainerSize()) {
                                ItemStack slotItem = player.getInventory().getItem(slotIndex);
                                if (!slotItem.isEmpty()) {
                                    if (!RecycleManager.getInstance().recycleItem(player, slotItem.copy())) {
                                        success = false;
                                    } else {
                                        player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
                                    }
                                }
                            }
                        }
                        if (success) {
                            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.recycle_success"));
                        } else {
                            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.recycle_partial"));
                        }
                    } else {
                        // 物品模式 - 回收指定物品
                        ItemStack recycleItem = packet.itemStack.copy();
                        recycleItem.setCount(packet.count);
                        
                        if (RecycleManager.getInstance().recycleItem(player, recycleItem)) {
                            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.recycle_success"));
                        } else {
                            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.recycle_failed"));
                        }
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.translatable("gui.tradesystem.message.recycle_error"));
                }
            }
        });
        context.setPacketHandled(true);
    }
}