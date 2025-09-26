package com.tradesystem.mod.client.gui.widget;

import com.tradesystem.mod.client.gui.MyTradesScreen;
import com.tradesystem.mod.data.TradeItem;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 交易历史组件
 * 用于显示单个交易项目或交易记录
 */
public class TradeHistoryWidget extends AbstractWidget {
    
    private static final int ITEM_SIZE = 16;
    private static final int PADDING = 4;
    
    // 数据
    private TradeItem tradeItem;
    private MyTradesScreen.TransactionRecord transactionRecord;
    
    // 状态
    private boolean selected = false;
    private boolean hovered = false;
    
    // 回调
    private final Consumer<TradeHistoryWidget> clickCallback;
    private boolean visible = true;
    
    public TradeHistoryWidget(int x, int y, int width, int height, Consumer<TradeHistoryWidget> clickCallback) {
        super(x, y, width, height, Component.empty());
        this.clickCallback = clickCallback;
    }
    
    /**
     * 设置交易项目数据
     */
    public void setTradeItem(TradeItem tradeItem) {
        this.tradeItem = tradeItem;
        this.transactionRecord = null;
    }
    
    /**
     * 设置交易记录数据
     */
    public void setTransactionRecord(MyTradesScreen.TransactionRecord record) {
        this.transactionRecord = record;
        this.tradeItem = null;
    }
    
    /**
     * 获取交易项目
     */
    public TradeItem getTradeItem() {
        return tradeItem;
    }
    
    /**
     * 获取交易记录
     */
    public MyTradesScreen.TransactionRecord getTransactionRecord() {
        return transactionRecord;
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
    
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        
        // 更新悬停状态
        this.hovered = mouseX >= this.getX() && mouseY >= this.getY() && 
                      mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        
        // 渲染背景
        renderBackground(guiGraphics);
        
        // 渲染内容
        if (tradeItem != null) {
            renderTradeItem(guiGraphics);
        } else if (transactionRecord != null) {
            renderTransactionRecord(guiGraphics);
        }
    }
    
    /**
     * 渲染背景
     */
    private void renderBackground(GuiGraphics guiGraphics) {
        int backgroundColor;
        
        if (selected) {
            backgroundColor = 0xFF4A90E2; // 蓝色选中
        } else if (hovered) {
            backgroundColor = 0xFF666666; // 灰色悬停
        } else {
            backgroundColor = 0xFF333333; // 深灰色默认
        }
        
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, backgroundColor);
        
        // 渲染边框
        int borderColor = selected ? 0xFF87CEEB : 0xFF555555;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
    }
    
    /**
     * 渲染交易项目
     */
    private void renderTradeItem(GuiGraphics guiGraphics) {
        Font font = Minecraft.getInstance().font;
        
        // 渲染物品图标
        ItemStack itemStack = tradeItem.getItemStack();
        guiGraphics.renderItem(itemStack, getX() + PADDING, getY() + (height - ITEM_SIZE) / 2);
        
        // 渲染物品数量
        if (itemStack.getCount() > 1) {
            String countText = String.valueOf(itemStack.getCount());
            guiGraphics.drawString(font, countText, 
                    getX() + PADDING + ITEM_SIZE - font.width(countText), 
                    getY() + (height - ITEM_SIZE) / 2 + ITEM_SIZE - 8, 
                    0xFFFFFF, true);
        }
        
        // 渲染物品名称
        Component itemName = Component.literal(tradeItem.getDisplayName());
        int nameX = getX() + PADDING + ITEM_SIZE + PADDING;
        int nameY = getY() + (height - font.lineHeight) / 2 - 2;
        guiGraphics.drawString(font, itemName, nameX, nameY, 0xFFFFFF, false);
        
        // 渲染价格
        String priceText = CurrencyUtil.formatMoney(tradeItem.getPrice());
        int priceWidth = font.width(priceText);
        int priceX = getX() + width - PADDING - priceWidth;
        int priceY = getY() + (height - font.lineHeight) / 2 - 2;
        guiGraphics.drawString(font, priceText, priceX, priceY, 0xFFD700, false);
        
        // 渲染状态信息
        String statusText = getTradeItemStatusText();
        int statusY = getY() + (height - font.lineHeight) / 2 + 6;
        guiGraphics.drawString(font, statusText, nameX, statusY, 0xAAAAAA, false);
    }
    
    /**
     * 渲染交易记录
     */
    private void renderTransactionRecord(GuiGraphics guiGraphics) {
        Font font = Minecraft.getInstance().font;
        
        // 渲染物品图标
        ItemStack itemStack = transactionRecord.getItemStack();
        guiGraphics.renderItem(itemStack, getX() + PADDING, getY() + (height - ITEM_SIZE) / 2);
        
        // 渲染物品数量
        if (itemStack.getCount() > 1) {
            String countText = String.valueOf(itemStack.getCount());
            guiGraphics.drawString(font, countText, 
                    getX() + PADDING + ITEM_SIZE - font.width(countText), 
                    getY() + (height - ITEM_SIZE) / 2 + ITEM_SIZE - 8, 
                    0xFFFFFF, true);
        }
        
        // 渲染物品名称
        Component itemName = itemStack.getDisplayName();
        int nameX = getX() + PADDING + ITEM_SIZE + PADDING;
        int nameY = getY() + (height - font.lineHeight) / 2 - 2;
        guiGraphics.drawString(font, itemName, nameX, nameY, 0xFFFFFF, false);
        
        // 渲染价格
        String priceText = CurrencyUtil.formatMoney(transactionRecord.getPrice());
        int priceWidth = font.width(priceText);
        int priceX = getX() + width - PADDING - priceWidth;
        int priceY = getY() + (height - font.lineHeight) / 2 - 2;
        
        // 根据交易类型设置价格颜色
        int priceColor = switch (transactionRecord.getType()) {
            case SELL -> 0xFF00FF00; // 绿色 - 收入
            case BUY -> 0xFFFF6B6B; // 红色 - 支出
            case RECYCLE -> 0xFFFFD700; // 金色 - 回收
        };
        
        guiGraphics.drawString(font, priceText, priceX, priceY, priceColor, false);
        
        // 渲染交易信息
        String infoText = getTransactionInfoText();
        int infoY = getY() + (height - font.lineHeight) / 2 + 6;
        guiGraphics.drawString(font, infoText, nameX, infoY, 0xAAAAAA, false);
    }
    
    /**
     * 获取交易项目状态文本
     */
    private String getTradeItemStatusText() {
        if (tradeItem == null) {
            return "";
        }
        
        // 简化实现，返回固定状态
        return "活跃";
    }
    
    /**
     * 获取交易记录信息文本
     */
    private String getTransactionInfoText() {
        if (transactionRecord == null) {
            return "";
        }
        
        String typeText = switch (transactionRecord.getType()) {
            case SELL -> "出售给 " + transactionRecord.getBuyerName();
            case BUY -> "购买自 " + transactionRecord.getSellerName();
            case RECYCLE -> "系统回收";
        };
        
        return typeText + " - " + transactionRecord.getFormattedTime();
    }
    public List<Component> getTooltipComponents() {
        List<Component> tooltip = new ArrayList<>();
        
        if (tradeItem != null) {
            // 交易项目工具提示
            tooltip.add(Component.literal(tradeItem.getDisplayName()));
            tooltip.add(Component.literal("价格: " + CurrencyUtil.formatMoney(tradeItem.getPrice())));
            tooltip.add(Component.literal("卖家: " + tradeItem.getSellerName()));
            
            if (tradeItem.getDescription() != null && !tradeItem.getDescription().isEmpty()) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("描述:"));
                tooltip.add(Component.literal(tradeItem.getDescription()).withStyle(style -> style.withColor(0xAAAAAA)));
            }
            
        } else if (transactionRecord != null) {
            // 交易记录工具提示
            tooltip.add(transactionRecord.getItemStack().getDisplayName());
            tooltip.add(Component.literal("价格: " + CurrencyUtil.formatMoney(transactionRecord.getPrice())));
            
            switch (transactionRecord.getType()) {
                case SELL -> {
                    tooltip.add(Component.literal("类型: 出售").withStyle(style -> style.withColor(0x00FF00)));
                    tooltip.add(Component.literal("买家: " + transactionRecord.getBuyerName()));
                }
                case BUY -> {
                    tooltip.add(Component.literal("类型: 购买").withStyle(style -> style.withColor(0xFF6B6B)));
                    tooltip.add(Component.literal("卖家: " + transactionRecord.getSellerName()));
                }
                case RECYCLE -> {
                    tooltip.add(Component.literal("类型: 系统回收").withStyle(style -> style.withColor(0xFFD700)));
                }
            }
            
            tooltip.add(Component.literal("时间: " + transactionRecord.getFormattedTime()));
            tooltip.add(Component.literal("交易ID: " + transactionRecord.getTransactionId().toString().substring(0, 8) + "..."));
        }
        
        return tooltip;
    }

    /**
     * 检查是否可见
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.visible && this.active && clickCallback != null) {
            clickCallback.accept(this);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
            this.onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // 简化实现，避免类型引用问题
    }
}