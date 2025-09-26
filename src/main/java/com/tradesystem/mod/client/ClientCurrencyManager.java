package com.tradesystem.mod.client;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.config.TradeConfig;

/**
 * 客户端货币管理器
 * 负责管理客户端的货币数据显示
 */
public class ClientCurrencyManager {
    
    private static ClientCurrencyManager instance;
    private int playerMoney = TradeConfig.initialPlayerMoney; // 使用配置文件中的初始金币
    
    private ClientCurrencyManager() {}
    
    public static ClientCurrencyManager getInstance() {
        if (instance == null) {
            instance = new ClientCurrencyManager();
        }
        return instance;
    }
    
    /**
     * 获取玩家当前金币数量
     */
    public int getPlayerMoney() {
        return playerMoney;
    }
    
    /**
     * 设置玩家金币数量（由服务器同步）
     */
    public void setPlayerMoney(int money) {
        int oldMoney = this.playerMoney;
        this.playerMoney = Math.max(0, money);
        
        if (oldMoney != this.playerMoney) {
            TradeMod.getLogger().debug("客户端金币更新: {} -> {}", oldMoney, this.playerMoney);
        }
    }
    
    /**
     * 重置金币到初始值
     */
    public void reset() {
        this.playerMoney = TradeConfig.initialPlayerMoney;
        TradeMod.getLogger().debug("客户端金币已重置为: {}", this.playerMoney);
    }
}