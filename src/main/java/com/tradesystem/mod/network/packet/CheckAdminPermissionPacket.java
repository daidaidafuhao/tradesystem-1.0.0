package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.manager.SystemItemManager;
import com.tradesystem.mod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 检查管理员权限数据包
 * 客户端发送给服务器，请求检查当前玩家是否有管理员权限
 */
public class CheckAdminPermissionPacket {
    
    private final UUID playerUUID;
    
    public CheckAdminPermissionPacket(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }
    
    public CheckAdminPermissionPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getUUID().equals(playerUUID)) {
                // 检查管理员权限
                boolean hasPermission = SystemItemManager.getInstance().hasAdminPermission(player);
                
                // 发送权限结果回客户端
                AdminPermissionResponsePacket response = new AdminPermissionResponsePacket(hasPermission);
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), response);
            }
        });
        return true;
    }
}