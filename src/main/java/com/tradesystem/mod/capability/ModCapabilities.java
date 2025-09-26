package com.tradesystem.mod.capability;

import com.tradesystem.mod.TradeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * MOD Capability注册类
 * 负责注册所有的Capability
 */
@Mod.EventBusSubscriber(modid = TradeMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCapabilities {
    
    // 玩家货币Capability
    public static final Capability<IPlayerCurrency> PLAYER_CURRENCY = CapabilityManager.get(new CapabilityToken<>(){});
    
    // Capability资源位置
    public static final ResourceLocation PLAYER_CURRENCY_LOCATION = ResourceLocation.tryParse(TradeMod.MODID + ":player_currency");
    
    /**
     * 注册Capability
     */
    public static void register() {
        TradeMod.getLogger().info("注册Capability系统...");
    }
    
    /**
     * Capability注册事件
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerCurrency.class);
        TradeMod.getLogger().info("Capability注册完成");
    }
}