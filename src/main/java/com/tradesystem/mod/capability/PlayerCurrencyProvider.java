package com.tradesystem.mod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家货币Capability提供者
 * 负责为玩家实体提供货币Capability
 */
public class PlayerCurrencyProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    
    private PlayerCurrency currency = null;
    private final LazyOptional<IPlayerCurrency> optional = LazyOptional.of(this::createCurrency);
    
    private PlayerCurrency createCurrency() {
        if (this.currency == null) {
            this.currency = new PlayerCurrency();
        }
        return this.currency;
    }
    
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.PLAYER_CURRENCY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createCurrency().serializeNBT().getAllKeys().forEach(key -> {
            nbt.put(key, createCurrency().serializeNBT().get(key));
        });
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createCurrency().deserializeNBT(nbt);
    }
}