package com.tradesystem.mod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * 玩家货币Capability接口
 * 定义玩家货币相关的操作
 */
public interface IPlayerCurrency extends INBTSerializable<CompoundTag> {
    
    /**
     * 获取玩家当前金币数量
     * @return 金币数量
     */
    int getMoney();
    
    /**
     * 设置玩家金币数量
     * @param money 金币数量
     */
    void setMoney(int money);
    
    /**
     * 增加金币
     * @param amount 增加的数量
     * @return 是否成功
     */
    boolean addMoney(int amount);
    
    /**
     * 减少金币
     * @param amount 减少的数量
     * @return 是否成功（金币足够）
     */
    boolean removeMoney(int amount);
    
    /**
     * 检查是否有足够的金币
     * @param amount 需要的金币数量
     * @return 是否足够
     */
    boolean hasMoney(int amount);
    
    /**
     * 重置金币到初始值
     */
    void reset();
    
    /**
     * 获取最大金币限制
     * @return 最大金币数量
     */
    int getMaxMoney();
}