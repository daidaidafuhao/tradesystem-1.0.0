package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.packet.RequestTradeHistorySyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

/**
 * GUI管理器
 * 负责管理和打开各种交易界面
 */
public class GuiManager {
    
    private static final Minecraft minecraft = Minecraft.getInstance();
    
    /**
     * 打开交易市场主界面
     */
    public static void openTradeMarket() {
        openScreen(new TradeMarketScreen());
    }
    
    /**
     * 打开购买界面
     */
    public static void openBuyScreen() {
        openScreen(new BuyScreen());
    }
    
    /**
     * 打开出售界面
     */
    public static void openSellScreen() {
        openScreen(new SellScreen());
    }
    
    /**
     * 打开我的交易界面
     */
    public static void openMyTradesScreen() {
        // 请求服务器同步交易历史
        if (minecraft.player != null) {
            NetworkHandler.sendToServer(new RequestTradeHistorySyncPacket(minecraft.player.getUUID()));
        }
        
        openScreen(new MyTradesScreen());
    }
    
    /**
     * 打开商品管理界面
     */
    public static void openItemManagementScreen() {
        openScreen(new ItemManagementScreen());
    }
    
    /**
     * 打开设置界面
     */
    public static void openSettingsScreen() {
        // TODO: 实现设置界面
        // openScreen(new SettingsScreen());
    }
    
    /**
     * 通用的打开界面方法
     */
    private static void openScreen(Screen screen) {
        if (minecraft.player != null) {
            minecraft.setScreen(screen);
        }
    }
    
    /**
     * 关闭当前界面
     */
    public static void closeCurrentScreen() {
        if (minecraft.screen != null) {
            minecraft.setScreen(null);
        }
    }
    
    /**
     * 检查是否有交易界面打开
     */
    public static boolean isTradeScreenOpen() {
        return minecraft.screen instanceof BaseTradeScreen;
    }
    
    /**
     * 获取当前玩家
     */
    public static Player getCurrentPlayer() {
        return minecraft.player;
    }
}