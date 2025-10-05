package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.client.ClientSystemItemManager;
import com.tradesystem.mod.data.SystemItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 系统商品同步数据包
 * 用于将系统商品数据从服务端同步到客户端
 */
public class SystemItemSyncPacket {
    
    private final List<SystemItem> systemItems;
    
    public SystemItemSyncPacket(List<SystemItem> systemItems) {
        this.systemItems = systemItems != null ? systemItems : new ArrayList<>();
    }
    
    /**
     * 编码数据包
     */
    public static void encode(SystemItemSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.systemItems.size());
        
        for (SystemItem item : packet.systemItems) {
            CompoundTag itemTag = item.serializeNBT();
            buffer.writeNbt(itemTag);
        }
    }
    
    /**
     * 解码数据包
     */
    public static SystemItemSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<SystemItem> items = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            CompoundTag itemTag = buffer.readNbt();
            if (itemTag != null) {
                SystemItem item = new SystemItem();
                item.deserializeNBT(itemTag);
                items.add(item);
            }
        }
        
        return new SystemItemSyncPacket(items);
    }
    
    /**
     * 处理数据包
     */
    public static void handle(SystemItemSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    handleClientSide(packet);
                });
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 客户端处理逻辑
     */
    private static void handleClientSide(SystemItemSyncPacket packet) {
        try {
            ClientSystemItemManager.getInstance().updateSystemItems(packet.systemItems);
            TradeMod.getLogger().info("已同步 {} 个系统商品到客户端", packet.systemItems.size());
        } catch (Exception e) {
            TradeMod.getLogger().error("同步系统商品到客户端时发生错误", e);
        }
    }
}