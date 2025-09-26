package com.tradesystem.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tradesystem.mod.TradeMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 键盘绑定管理类
 * 管理交易系统的所有快捷键
 */
@Mod.EventBusSubscriber(modid = TradeMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    
    // 打开交易界面的快捷键 - G键
    public static final KeyMapping OPEN_TRADE_GUI = new KeyMapping(
            "key.tradesystem.open_trade_gui", // 翻译键
            InputConstants.Type.KEYSYM, // 键盘类型
            InputConstants.KEY_G, // G键
            "key.categories.tradesystem" // 分类
    );
    
    /**
     * 注册键盘绑定
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TRADE_GUI);
        TradeMod.getLogger().info("交易系统键盘绑定已注册: G键 -> 打开交易界面");
    }
}