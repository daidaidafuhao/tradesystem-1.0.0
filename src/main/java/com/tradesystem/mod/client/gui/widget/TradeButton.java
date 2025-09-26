package com.tradesystem.mod.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 交易系统自定义按钮
 * 提供统一的按钮样式和行为
 */
public class TradeButton extends Button {
    
    // 按钮颜色常量
    private static final int NORMAL_COLOR = 0xFF404040;
    private static final int HOVER_COLOR = 0xFF606060;
    private static final int PRESSED_COLOR = 0xFF202020;
    private static final int DISABLED_COLOR = 0xFF303030;
    private static final int BORDER_COLOR = 0xFF8B8B8B;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int DISABLED_TEXT_COLOR = 0xFF808080;
    
    public TradeButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int backgroundColor = getBackgroundColor();
        int textColor = this.active ? TEXT_COLOR : DISABLED_TEXT_COLOR;
        
        // 渲染按钮背景
        guiGraphics.fill(this.getX() - 1, this.getY() - 1, 
                this.getX() + this.width + 1, this.getY() + this.height + 1, BORDER_COLOR);
        guiGraphics.fill(this.getX(), this.getY(), 
                this.getX() + this.width, this.getY() + this.height, backgroundColor);
        
        // 渲染按钮文本
        int textX = this.getX() + (this.width - this.getFont().width(this.getMessage())) / 2;
        int textY = this.getY() + (this.height - 8) / 2;
        guiGraphics.drawString(this.getFont(), this.getMessage(), textX, textY, textColor, false);
    }
    
    /**
     * 获取按钮背景颜色
     */
    private int getBackgroundColor() {
        if (!this.active) {
            return DISABLED_COLOR;
        } else if (this.isPressed()) {
            return PRESSED_COLOR;
        } else if (this.isHovered()) {
            return HOVER_COLOR;
        } else {
            return NORMAL_COLOR;
        }
    }
    
    /**
     * 检查按钮是否被按下
     */
    public boolean isPressed() {
        return this.isHovered() && this.isHoveredOrFocused();
    }
    
    /**
     * 获取字体渲染器
     */
    private net.minecraft.client.gui.Font getFont() {
        return net.minecraft.client.Minecraft.getInstance().font;
    }
}