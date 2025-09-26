package com.tradesystem.mod;

import com.mojang.logging.LogUtils;
import com.tradesystem.mod.capability.ModCapabilities;
import com.tradesystem.mod.command.TradeCommand;
import com.tradesystem.mod.config.TradeConfig;
import com.tradesystem.mod.data.DataService;
import com.tradesystem.mod.data.TradeDataManager;
import com.tradesystem.mod.network.NetworkHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * 交易系统MOD主类
 * 负责MOD的初始化、注册和生命周期管理
 */
@Mod(TradeMod.MODID)
public class TradeMod {
    // MOD ID
    public static final String MODID = "tradesystem";
    
    // 日志记录器
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 物品注册器
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    
    // 创造模式标签页注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    
    // 数据管理器实例
    private static TradeDataManager dataManager;
    
    public TradeMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        
        // 注册通用设置事件
        modEventBus.addListener(this::commonSetup);
        
        // 注册延迟注册器
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        
        // 注册网络包
        NetworkHandler.registerPackets();
        
        // 注册到Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
        
        // 注册配置
        context.registerConfig(ModConfig.Type.COMMON, TradeConfig.SPEC);
        
        LOGGER.info("交易系统MOD初始化完成");
    }
    
    /**
     * 通用设置阶段
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 初始化Capability系统
            ModCapabilities.register();
            
            // 初始化网络处理器
            NetworkHandler.registerPackets();
            
            LOGGER.info("交易系统通用设置完成");
        });
    }
    
    /**
     * 注册命令事件
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TradeCommand.register(event.getDispatcher());
        LOGGER.info("交易系统命令注册完成");
    }
    
    /**
     * 服务器启动事件
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("交易系统MOD服务器启动");
        
        // 初始化JSON数据管理器
        com.tradesystem.mod.data.JsonDataManager.getInstance().initialize(event.getServer());
        
        // 启动定期数据保存任务
        com.tradesystem.mod.task.DataSaveTask.getInstance().start();
        
        // 初始化数据管理器（保留兼容性）
        dataManager = new TradeDataManager(event.getServer());
        if (dataManager != null) {
            LOGGER.info("数据管理器初始化完成");
        }
        
        // 初始化数据服务
        DataService.getInstance().initialize(event.getServer());
        
        // 加载商品数据
        com.tradesystem.mod.manager.ItemListingManager.getInstance().loadData();
        
        LOGGER.info("交易系统服务器启动完成");
    }
    
    /**
     * 服务器关闭事件
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("交易系统MOD服务器关闭中...");
        
        // 保存JSON数据
        com.tradesystem.mod.data.JsonDataManager.getInstance().shutdown();
        
        // 保存商品数据
        com.tradesystem.mod.manager.ItemListingManager.getInstance().saveData();
        
        // 关闭数据管理器
        if (dataManager != null) {
            dataManager.shutdown();
        }
        
        // 关闭数据服务
        DataService.getInstance().shutdown();
        
        LOGGER.info("交易系统MOD服务器关闭完成");
    }
    
    /**
     * 获取数据管理器实例
     */
    public static TradeDataManager getDataManager() {
        return dataManager;
    }
    
    /**
     * 获取日志记录器
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}