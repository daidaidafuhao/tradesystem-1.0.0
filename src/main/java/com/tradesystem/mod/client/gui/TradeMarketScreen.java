package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.client.gui.widget.TradeButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 交易市场主界面
 * 玩家进入交易系统的主要界面
 */
public class TradeMarketScreen extends BaseTradeScreen {
    
    // 按钮
    private Button buyButton;
    private Button sellButton;
    private Button myTradesButton;
    private Button itemManagementButton;
    private Button settingsButton;
    
    public TradeMarketScreen() {
        super(Component.translatable("gui.tradesystem.market.title"), 300, 240);
    }
    
    @Override
    protected void initComponents() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int buttonSpacing = 25;
        int startY = topPos + 40;
        
        // 购买按钮
        this.buyButton = new TradeButton(
                leftPos + (imageWidth - buttonWidth) / 2,
                startY,
                buttonWidth,
                buttonHeight,
                Component.translatable("gui.tradesystem.button.buy"),
                this::onBuyButtonPressed
        );
        this.addRenderableWidget(buyButton);
        
        // 出售按钮
        this.sellButton = new TradeButton(
                leftPos + (imageWidth - buttonWidth) / 2,
                startY + buttonSpacing,
                buttonWidth,
                buttonHeight,
                Component.translatable("gui.tradesystem.button.sell"),
                this::onSellButtonPressed
        );
        this.addRenderableWidget(sellButton);
        
        // 我的交易按钮
        this.myTradesButton = new TradeButton(
                leftPos + (imageWidth - buttonWidth) / 2,
                startY + buttonSpacing * 2,
                buttonWidth,
                buttonHeight,
                Component.translatable("gui.tradesystem.button.my_trades"),
                this::onMyTradesButtonPressed
        );
        this.addRenderableWidget(myTradesButton);
        
        // 商品管理按钮
        this.itemManagementButton = new TradeButton(
                leftPos + (imageWidth - buttonWidth) / 2,
                startY + buttonSpacing * 3,
                buttonWidth,
                buttonHeight,
                Component.translatable("gui.tradesystem.button.item_management"),
                this::onItemManagementButtonPressed
        );
        this.addRenderableWidget(itemManagementButton);
        
        // 设置按钮
        this.settingsButton = new TradeButton(
                leftPos + (imageWidth - buttonWidth) / 2,
                startY + buttonSpacing * 4,
                buttonWidth,
                buttonHeight,
                Component.translatable("gui.tradesystem.button.settings"),
                this::onSettingsButtonPressed
        );
        this.addRenderableWidget(settingsButton);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染标题
        renderTitle(guiGraphics);
        
        // 渲染玩家金币信息
        renderPlayerMoney(guiGraphics);
        
        // 渲染欢迎信息
        Component welcomeText = Component.translatable("gui.tradesystem.market.welcome");
        int textWidth = this.font.width(welcomeText);
        guiGraphics.drawString(this.font, welcomeText,
                leftPos + (imageWidth - textWidth) / 2, topPos + 25, TEXT_COLOR, false);
    }
    
    @Override
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 为按钮添加工具提示
        if (buyButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.tradesystem.tooltip.buy"), mouseX, mouseY);
        } else if (sellButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.tradesystem.tooltip.sell"), mouseX, mouseY);
        } else if (myTradesButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.tradesystem.tooltip.my_trades"), mouseX, mouseY);
        } else if (itemManagementButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.tradesystem.tooltip.item_management"), mouseX, mouseY);
        } else if (settingsButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.tradesystem.tooltip.settings"), mouseX, mouseY);
        }
    }
    
    /**
     * 购买按钮点击事件
     */
    private void onBuyButtonPressed(Button button) {
        GuiManager.openBuyScreen();
    }
    
    /**
     * 出售按钮点击事件
     */
    private void onSellButtonPressed(Button button) {
        GuiManager.openSellScreen();
    }
    
    /**
     * 我的交易按钮点击事件
     */
    private void onMyTradesButtonPressed(Button button) {
        GuiManager.openMyTradesScreen();
    }
    
    /**
     * 商品管理按钮点击事件
     */
    private void onItemManagementButtonPressed(Button button) {
        GuiManager.openItemManagementScreen();
    }
    
    /**
     * 设置按钮点击事件
     */
    private void onSettingsButtonPressed(Button button) {
        // TODO: 打开设置界面
        if (this.minecraft != null) {
            this.minecraft.player.sendSystemMessage(
                    Component.translatable("gui.tradesystem.message.settings_clicked"));
        }
    }
}