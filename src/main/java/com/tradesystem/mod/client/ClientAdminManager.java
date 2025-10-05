package com.tradesystem.mod.client;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.packet.CheckAdminPermissionPacket;
import net.minecraft.client.Minecraft;

/**
 * 客户端管理员状态管理器
 * 负责管理客户端的管理员权限状态
 */
public class ClientAdminManager {
    
    private static ClientAdminManager instance;
    private boolean hasAdminPermission = false;
    private boolean permissionChecked = false;
    
    private ClientAdminManager() {}
    
    public static ClientAdminManager getInstance() {
        if (instance == null) {
            instance = new ClientAdminManager();
        }
        return instance;
    }
    
    /**
     * 检查是否有管理员权限
     */
    public boolean hasAdminPermission() {
        return hasAdminPermission;
    }
    
    /**
     * 设置管理员权限状态
     */
    public void setAdminPermission(boolean hasPermission) {
        this.hasAdminPermission = hasPermission;
        this.permissionChecked = true;
        
        TradeMod.getLogger().info("Admin permission updated: {}", hasPermission);
    }
    
    /**
     * 权限是否已检查
     */
    public boolean isPermissionChecked() {
        return permissionChecked;
    }
    
    /**
     * 请求检查管理员权限
     */
    public void requestPermissionCheck() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            CheckAdminPermissionPacket packet = new CheckAdminPermissionPacket(minecraft.player.getUUID());
            NetworkHandler.sendToServer(packet);
            TradeMod.getLogger().info("Requesting admin permission check for player: {}", 
                    minecraft.player.getName().getString());
        }
    }
    
    /**
     * 重置权限状态（玩家登出时调用）
     */
    public void reset() {
        this.hasAdminPermission = false;
        this.permissionChecked = false;
        TradeMod.getLogger().info("Admin permission reset");
    }
    
    /**
     * 确保权限已检查
     */
    public void ensurePermissionChecked() {
        if (!permissionChecked) {
            requestPermissionCheck();
        }
    }
}