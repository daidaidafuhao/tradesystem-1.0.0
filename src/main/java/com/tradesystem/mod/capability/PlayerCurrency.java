package com.tradesystem.mod.capability;

import com.tradesystem.mod.config.TradeConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * 玩家货币Capability实现类
 */
public class PlayerCurrency implements IPlayerCurrency {
    
    private int money;
    
    public PlayerCurrency() {
        this.money = TradeConfig.initialPlayerMoney;
    }
    
    @Override
    public int getMoney() {
        return money;
    }
    
    @Override
    public void setMoney(int money) {
        this.money = Math.max(0, Math.min(money, getMaxMoney()));
    }
    
    @Override
    public boolean addMoney(int amount) {
        if (amount < 0) {
            return false;
        }
        
        long newMoney = (long) this.money + amount;
        if (newMoney > getMaxMoney()) {
            this.money = getMaxMoney();
            return false; // 达到上限
        }
        
        this.money = (int) newMoney;
        return true;
    }
    
    @Override
    public boolean removeMoney(int amount) {
        if (amount < 0 || !hasMoney(amount)) {
            return false;
        }
        
        this.money -= amount;
        return true;
    }
    
    @Override
    public boolean hasMoney(int amount) {
        return this.money >= amount && amount >= 0;
    }
    
    @Override
    public void reset() {
        this.money = TradeConfig.initialPlayerMoney;
    }
    
    @Override
    public int getMaxMoney() {
        return TradeConfig.maxPlayerMoney;
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("money", this.money);
        return tag;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // 如果NBT中没有金币数据，使用初始金币而不是0
        if (nbt.contains("money")) {
            this.money = nbt.getInt("money");
        } else {
            this.money = TradeConfig.initialPlayerMoney;
            com.tradesystem.mod.TradeMod.getLogger().warn("NBT中没有找到金币数据，使用初始金币: {}", this.money);
        }
        
        // 确保金币数量在有效范围内
        this.money = Math.max(0, Math.min(this.money, getMaxMoney()));
        
        com.tradesystem.mod.TradeMod.getLogger().debug("反序列化金币数据: {}", this.money);
    }
}