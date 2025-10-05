package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.client.ClientAdminManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 管理员权限响应数据包
 * 服务器发送给客户端，告知玩家是否有管理员权限
 */
public class AdminPermissionResponsePacket {
    
    private final boolean hasPermission;
    
    public AdminPermissionResponsePacket(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }
    
    public AdminPermissionResponsePacket(FriendlyByteBuf buf) {
        this.hasPermission = buf.readBoolean();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(hasPermission);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 更新客户端管理员状态
            ClientAdminManager.getInstance().setAdminPermission(hasPermission);
        });
        return true;
    }
}