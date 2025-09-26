package com.tradesystem.mod.task;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.data.JsonDataManager;
import com.tradesystem.mod.data.TradeDataManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据保存任务
 * 负责定期保存交易系统的所有数据
 */
public class DataSaveTask {
    
    private static DataSaveTask instance;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    // 保存间隔（秒）
    private static final int SAVE_INTERVAL = 300; // 5分钟
    
    private DataSaveTask() {
        // 私有构造函数，确保单例
    }
    
    /**
     * 获取单例实例
     */
    public static DataSaveTask getInstance() {
        if (instance == null) {
            instance = new DataSaveTask();
        }
        return instance;
    }
    
    /**
     * 启动定期保存任务
     */
    public void start() {
        if (isRunning) {
            TradeMod.getLogger().warn("数据保存任务已经在运行中");
            return;
        }
        
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "TradeSystem-DataSave");
            thread.setDaemon(true);
            return thread;
        });
        
        // 启动定期保存任务
        scheduler.scheduleAtFixedRate(this::performSave, SAVE_INTERVAL, SAVE_INTERVAL, TimeUnit.SECONDS);
        
        isRunning = true;
        TradeMod.getLogger().info("数据保存任务已启动，保存间隔: {} 秒", SAVE_INTERVAL);
    }
    
    /**
     * 停止定期保存任务
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        if (scheduler != null) {
            // 执行最后一次保存
            performSave();
            
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        isRunning = false;
        TradeMod.getLogger().info("数据保存任务已停止");
    }
    
    /**
     * 执行数据保存
     */
    private void performSave() {
        try {
            // 保存JSON数据
            JsonDataManager jsonManager = JsonDataManager.getInstance();
            if (jsonManager != null) {
                jsonManager.saveAllData();
            }
            
            // 保存传统数据管理器数据
            TradeDataManager dataManager = TradeDataManager.getInstance();
            if (dataManager != null) {
                dataManager.saveAllData();
            }
            
            TradeMod.getLogger().debug("定期数据保存完成");
            
        } catch (Exception e) {
            TradeMod.getLogger().error("定期数据保存时发生错误", e);
        }
    }
    
    /**
     * 立即执行一次保存
     */
    public void saveNow() {
        performSave();
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
}