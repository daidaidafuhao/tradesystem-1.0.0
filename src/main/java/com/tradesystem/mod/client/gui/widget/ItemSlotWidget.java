package com.tradesystem.mod.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tradesystem.mod.data.TradeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

/**
 * 物品槽位组件
 * 用于在GUI中显示和交互物品
 */
public class ItemSlotWidget extends AbstractWidget {
    private ItemStack itemStack;
    private boolean selected;
    private boolean enabled;
    private Runnable onClickCallback;
    private java.util.function.Consumer<ItemSlotWidget> onSlotClickCallback;
    private TradeItem tradeItem;

    private static final ResourceLocation CONTAINER_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    
    public ItemSlotWidget(int x, int y, ItemStack itemStack) {
        super(x, y, 18, 18, Component.empty());
        this.itemStack = itemStack != null ? itemStack : ItemStack.EMPTY;
        this.selected = false;
        this.enabled = true;
    }
    
    public ItemSlotWidget(int x, int y, int size, Runnable onClickCallback) {
        super(x, y, size, size, Component.empty());
        this.itemStack = ItemStack.EMPTY;
        this.selected = false;
        this.enabled = true;
        this.onClickCallback = onClickCallback;
    }
    
    public ItemSlotWidget(int x, int y, int size, java.util.function.Consumer<ItemSlotWidget> onSlotClickCallback) {
        super(x, y, size, size, Component.empty());
        this.itemStack = ItemStack.EMPTY;
        this.selected = false;
        this.enabled = true;
        this.onSlotClickCallback = onSlotClickCallback;
    }
    
    /**
     * 设置物品
     */
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack != null ? itemStack : ItemStack.EMPTY;
    }
    
    /**
     * 获取物品
     */
    public ItemStack getItemStack() {
        return itemStack;
    }
    
    /**
     * 设置选中状态
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * 是否选中
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * 设置启用状态
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置点击回调
     */
    public void setOnClickCallback(Runnable callback) {
        this.onClickCallback = callback;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw slot background
        guiGraphics.blit(CONTAINER_TEXTURE, getX(), getY(), 7, 17, width, height);

        // 绘制物品
        if (!itemStack.isEmpty()) {
            RenderSystem.enableDepthTest();
            Minecraft minecraft = Minecraft.getInstance();

            // 计算物品图标居中位置
            int itemX = getX() + (width - 16) / 2;
            int itemY = getY() + (height - 16) / 2;

            // 渲染物品图标
            guiGraphics.renderItem(itemStack, itemX, itemY);

            // 渲染物品装饰（数量等）
            guiGraphics.renderItemDecorations(minecraft.font, itemStack, itemX, itemY);

            RenderSystem.disableDepthTest();
        }

        // 绘制选中高亮
        if (selected && enabled) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x8000FF00);
        }

        // 绘制悬停效果
        if (isHoveredOrFocused() && enabled) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x80FFFFFF);
        }
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (enabled) {
            if (onClickCallback != null) {
                onClickCallback.run();
            } else if (onSlotClickCallback != null) {
                onSlotClickCallback.accept(this);
            }
            playDownSound(Minecraft.getInstance().getSoundManager());
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (enabled && isValidClickButton(button)) {
            if (isMouseOver(mouseX, mouseY)) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                onClick(mouseX, mouseY);
                return true; // 只有在实际点击到槽位时才返回true
            }
        }
        return false; // 没有点击到槽位时返回false，允许事件传播
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // 简化实现，避免类型引用问题
    }
    
    /**
     * 检查是否为空槽位
     */
    public boolean isEmpty() {
        return itemStack.isEmpty();
    }
    
    /**
     * 检查是否有物品
     */
    public boolean hasItem() {
        return !itemStack.isEmpty();
    }
    
    /**
     * 检查是否激活
     */
    public boolean isActive() {
        return enabled;
    }
    
    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // TradeItem相关方法
    public TradeItem getTradeItem() {
        return tradeItem;
    }

    public void setTradeItem(TradeItem tradeItem) {
        this.tradeItem = tradeItem;
        if (tradeItem != null) {
            this.itemStack = tradeItem.getItemStack();
        } else {
            this.itemStack = ItemStack.EMPTY;
        }
    }

    /**
     * 清空槽位
     */
    public void clear() {
        this.itemStack = ItemStack.EMPTY;
    }
}