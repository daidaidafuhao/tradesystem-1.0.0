package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.packet.RequestCurrencySyncPacket;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 基础交易界面类
 * 所有交易相关界面的基类
 */
public abstract class BaseTradeScreen extends Screen {
    
    // 界面尺寸常量
    protected static final int DEFAULT_WIDTH = 256;
    protected static final int DEFAULT_HEIGHT = 166;
    
    // 颜色常量
    protected static final int BACKGROUND_COLOR = 0xC0101010;
    protected static final int BORDER_COLOR = 0xFF8B8B8B;
    protected static final int TEXT_COLOR = 0xFFFFFF;
    protected static final int MONEY_COLOR = 0xFFD700;
    
    // 界面位置
    protected int leftPos;
    protected int topPos;
    protected int imageWidth;
    protected int imageHeight;
    
    protected BaseTradeScreen(Component title) {
        super(title);
        this.imageWidth = DEFAULT_WIDTH;
        this.imageHeight = DEFAULT_HEIGHT;
    }
    
    protected BaseTradeScreen(Component title, int width, int height) {
        super(title);
        this.imageWidth = width;
        this.imageHeight = height;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 计算界面居中位置，确保界面不会超出屏幕边界
        this.leftPos = Math.max(0, (this.width - this.imageWidth) / 2);
        this.topPos = Math.max(0, (this.height - this.imageHeight) / 2);
        
        // 如果界面太大，调整到适合屏幕的大小
        if (this.imageWidth > this.width - 20) {
            this.imageWidth = this.width - 20;
            this.leftPos = 10;
        }
        if (this.imageHeight > this.height - 20) {
            this.imageHeight = this.height - 20;
            this.topPos = 10;
        }
        
        // 请求服务端同步最新的金币数据
        NetworkHandler.sendToServer(new RequestCurrencySyncPacket());
        
        // 初始化界面组件
        initComponents();
    }
    
    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        // 保存当前状态
        super.resize(minecraft, width, height);
        // 重新初始化以适应新的屏幕尺寸
        this.init();
    }
    
    /**
     * 初始化界面组件
     * 子类重写此方法来添加按钮、文本框等组件
     */
    protected abstract void initComponents();
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染界面内容
        renderContent(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染组件
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染工具提示
        renderTooltips(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * 渲染背景
     */
    protected void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染半透明背景
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
        
        // 渲染界面边框
        guiGraphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, BORDER_COLOR);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
    }
    
    /**
     * 渲染界面内容
     * 子类重写此方法来渲染自定义内容
     */
    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
    
    /**
     * 渲染工具提示
     * 子类重写此方法来渲染自定义工具提示
     */
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 默认实现为空，子类可以重写
    }
    
    /**
     * 渲染标题
     */
    protected void renderTitle(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, this.title, leftPos + 8, topPos + 6, TEXT_COLOR, false);
    }
    
    /**
     * 渲染玩家金币信息
     */
    protected void renderPlayerMoney(GuiGraphics guiGraphics) {
        if (this.minecraft != null && this.minecraft.player != null) {
            // 从ClientCurrencyManager获取同步的金币数量，确保实时更新
            int money = com.tradesystem.mod.client.ClientCurrencyManager.getInstance().getPlayerMoney();
            String moneyText = "金币: " + CurrencyUtil.formatMoney(money);
            int textWidth = this.font.width(moneyText);
            guiGraphics.drawString(this.font, moneyText, 
                    leftPos + imageWidth - textWidth - 8, topPos + 6, MONEY_COLOR, false);
        }
    }
    
    /**
     * 检查鼠标是否在指定区域内
     */
    protected boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    /**
     * 获取相对于界面的鼠标X坐标
     */
    protected int getRelativeMouseX(int mouseX) {
        return mouseX - leftPos;
    }
    
    /**
     * 获取相对于界面的鼠标Y坐标
     */
    protected int getRelativeMouseY(int mouseY) {
        return mouseY - topPos;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
}