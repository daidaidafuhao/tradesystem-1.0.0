package com.tradesystem.mod.client;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.client.gui.GuiManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端事件处理器
 * 处理客户端相关事件，如按键输入
 */
@Mod.EventBusSubscriber(modid = TradeMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    
    /**
     * 处理按键输入事件
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // 确保玩家在游戏中且没有打开其他界面
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        
        // 检查是否按下了打开交易界面的快捷键
        if (KeyBindings.OPEN_TRADE_GUI.consumeClick()) {
            TradeMod.getLogger().info("玩家 {} 按下G键，打开交易界面", minecraft.player.getName().getString());
            GuiManager.openTradeMarket();
        }
    }
}