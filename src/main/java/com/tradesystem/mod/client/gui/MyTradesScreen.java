package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.client.gui.widget.TradeButton;
import com.tradesystem.mod.client.gui.widget.TradeHistoryWidget;
import com.tradesystem.mod.data.TradeItem;

import com.tradesystem.mod.data.TradeManager;
import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 我的交易界面
 * 显示玩家的活跃交易和交易历史
 */
public class MyTradesScreen extends BaseTradeScreen {
    
    private static final int ITEMS_PER_PAGE = 6;
    
    // 界面组件
    private Button activeTradesTabButton;
    private Button historyTabButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button backButton;
    private Button refreshButton;
    private Button unlistButton;
    
    // 交易历史组件
    private final List<TradeHistoryWidget> historyWidgets = new ArrayList<>();
    
    // 数据
    private List<TradeItem> activeListings = new ArrayList<>();
    private List<TransactionRecord> transactionHistory = new ArrayList<>();
    private int currentPage = 0;
    private TabType currentTab = TabType.ACTIVE_TRADES;
    
    // 选中的项目
    private TradeItem selectedActiveTrade = null;
    private TransactionRecord selectedTransaction = null;
    private TradeHistoryWidget selectedWidget = null;
    
    public MyTradesScreen() {
        super(Component.translatable("gui.tradesystem.my_trades.title"), 300, 200);
    }
    
    @Override
    protected void initComponents() {
        // 标签页按钮
        activeTradesTabButton = new TradeButton(leftPos + 10, topPos + 25, 60, 15,
                Component.translatable("gui.tradesystem.my_trades.active_trades"),
                button -> switchToActiveTab());
        addRenderableWidget(activeTradesTabButton);
        
        historyTabButton = new TradeButton(leftPos + 80, topPos + 25, 60, 15,
                Component.translatable("gui.tradesystem.my_trades.history"),
                button -> switchToHistoryTab());
        addRenderableWidget(historyTabButton);
        
        // 刷新按钮
        refreshButton = new TradeButton(leftPos + 150, topPos + 25, 40, 15,
                Component.translatable("gui.tradesystem.sell.refresh"),
                button -> refreshData());
        addRenderableWidget(refreshButton);
        
        // 下架按钮
        unlistButton = new TradeButton(leftPos + 200, topPos + 25, 40, 15,
                Component.translatable("gui.tradesystem.my_trades.unlist"),
                button -> unlistSelectedItem());
        addRenderableWidget(unlistButton);
        
        // 创建交易历史组件 (紧凑显示)
        historyWidgets.clear();
        for (int i = 0; i < 6; i++) {
            int y = topPos + 50 + i * 18;
            TradeHistoryWidget widget = new TradeHistoryWidget(
                    leftPos + 10, y, imageWidth - 20, 16, this::onHistoryWidgetClicked);
            historyWidgets.add(widget);
            addRenderableWidget(widget);
        }
        
        // 分页按钮
        prevPageButton = new TradeButton(leftPos + 10, topPos + 160, 40, 15,
                Component.translatable("gui.tradesystem.my_trades.prev_page"),
                button -> previousPage());
        addRenderableWidget(prevPageButton);
        
        nextPageButton = new TradeButton(leftPos + 60, topPos + 160, 40, 15,
                Component.translatable("gui.tradesystem.my_trades.next_page"),
                button -> nextPage());
        addRenderableWidget(nextPageButton);
        
        // 返回按钮
        backButton = new TradeButton(leftPos + 200, topPos + 160, 40, 15,
                Component.translatable("gui.tradesystem.button.back"),
                button -> GuiManager.openTradeMarket());
        addRenderableWidget(backButton);
        
        // 加载数据
        refreshData();
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染标题
        renderTitle(guiGraphics);
        
        // 渲染玩家金币信息
        renderPlayerMoney(guiGraphics);
        
        // 渲染标签页指示器
        renderTabIndicator(guiGraphics);
        
        // 渲染分页信息
        renderPageInfo(guiGraphics);
        
        // 渲染统计信息
        renderStatistics(guiGraphics);
        
        // 更新按钮状态
        updateButtonStates();
    }
    
    @Override
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染交易历史组件的工具提示
        for (TradeHistoryWidget widget : historyWidgets) {
            if (widget.isMouseOver(mouseX, mouseY) && widget.isVisible()) {
                List<Component> tooltip = widget.getTooltipComponents();
                if (!tooltip.isEmpty()) {
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                }
            }
        }
    }
    
    /**
     * 交易历史组件点击处理
     */
    private void onHistoryWidgetClicked(TradeHistoryWidget widget) {
        if (selectedWidget != null) {
            selectedWidget.setSelected(false);
        }
        
        selectedWidget = widget;
        widget.setSelected(true);
        
        if (currentTab == TabType.ACTIVE_TRADES) {
            selectedActiveTrade = widget.getTradeItem();
            selectedTransaction = null;
        } else {
            selectedTransaction = widget.getTransactionRecord();
            selectedActiveTrade = null;
        }
    }
    
    /**
     * 切换到活跃交易标签
     */
    private void switchToActiveTab() {
        if (currentTab != TabType.ACTIVE_TRADES) {
            currentTab = TabType.ACTIVE_TRADES;
            currentPage = 0;
            clearSelection();
            updateDisplay();
        }
    }
    
    /**
     * 切换到历史记录标签
     */
    private void switchToHistoryTab() {
        if (currentTab != TabType.HISTORY) {
            currentTab = TabType.HISTORY;
            currentPage = 0;
            clearSelection();
            updateDisplay();
        }
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        
        UUID playerId = minecraft.player.getUUID();
        
        // 获取玩家的活跃交易
        activeListings = TradeManager.getInstance().getActiveTradeItems().stream()
                .filter(item -> item.getSellerId().equals(playerId))
                .collect(java.util.stream.Collectors.toList());
        
        // 获取交易历史（这里应该从服务器获取，暂时使用模拟数据）
        transactionHistory = getTransactionHistory(playerId);
        
        updateDisplay();
    }
    
    /**
     * 更新显示
     */
    private void updateDisplay() {
        List<?> currentData = getCurrentData();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        
        for (int i = 0; i < historyWidgets.size(); i++) {
            TradeHistoryWidget widget = historyWidgets.get(i);
            int dataIndex = startIndex + i;
            
            if (dataIndex < currentData.size()) {
                Object data = currentData.get(dataIndex);
                
                if (currentTab == TabType.ACTIVE_TRADES && data instanceof TradeItem tradeItem) {
                    widget.setTradeItem(tradeItem);
                    widget.setVisible(true);
                } else if (currentTab == TabType.HISTORY && data instanceof TransactionRecord record) {
                    widget.setTransactionRecord(record);
                    widget.setVisible(true);
                } else {
                    widget.setVisible(false);
                }
            } else {
                widget.setVisible(false);
            }
        }
    }
    
    /**
     * 获取当前标签的数据
     */
    private List<?> getCurrentData() {
        return currentTab == TabType.ACTIVE_TRADES ? activeListings : transactionHistory;
    }
    
    /**
     * 上一页
     */
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateDisplay();
        }
    }
    
    /**
     * 下一页
     */
    private void nextPage() {
        List<?> currentData = getCurrentData();
        int maxPage = (currentData.size() - 1) / ITEMS_PER_PAGE;
        if (currentPage < maxPage) {
            currentPage++;
            updateDisplay();
        }
    }
    
    /**
     * 下架选中的物品
     */
    private void unlistSelectedItem() {
        if (selectedActiveTrade == null) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.my_trades.no_item_selected"));
            }
            return;
        }
        
        // 发送下架请求到服务器
        NetworkHandler.sendToServer(new com.tradesystem.mod.network.UnlistItemPacket(selectedActiveTrade.getId()));
        
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(
                    Component.translatable("gui.tradesystem.my_trades.item_unlisted",
                            selectedActiveTrade.getDisplayName()));
        }
        
        // 刷新数据
        refreshData();
        clearSelection();
    }
    
    /**
     * 清除选择
     */
    private void clearSelection() {
        if (selectedWidget != null) {
            selectedWidget.setSelected(false);
            selectedWidget = null;
        }
        selectedActiveTrade = null;
        selectedTransaction = null;
    }
    
    /**
     * 渲染标签页指示器
     */
    private void renderTabIndicator(GuiGraphics guiGraphics) {
        // 高亮当前标签
        if (currentTab == TabType.ACTIVE_TRADES) {
            guiGraphics.fill(leftPos + 10, topPos + 45, leftPos + 110, topPos + 47, 0xFF00AA00);
        } else {
            guiGraphics.fill(leftPos + 120, topPos + 45, leftPos + 220, topPos + 47, 0xFF00AA00);
        }
    }
    
    /**
     * 渲染分页信息
     */
    private void renderPageInfo(GuiGraphics guiGraphics) {
        List<?> currentData = getCurrentData();
        int totalPages = Math.max(1, (currentData.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        
        Component pageText = Component.translatable("gui.tradesystem.my_trades.page_info",
                currentPage + 1, totalPages, currentData.size());
        
        int textWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText,
                leftPos + (imageWidth - textWidth) / 2, topPos + 290, TEXT_COLOR, false);
    }
    
    /**
     * 渲染统计信息
     */
    private void renderStatistics(GuiGraphics guiGraphics) {
        if (currentTab == TabType.ACTIVE_TRADES) {
            // 显示活跃交易统计
            int totalValue = activeListings.stream()
                    .mapToInt(TradeItem::getPrice)
                    .sum();
            
            Component statsText = Component.translatable("gui.tradesystem.my_trades.active_stats",
                    activeListings.size(), CurrencyUtil.formatMoney(totalValue));
            
            guiGraphics.drawString(this.font, statsText,
                    leftPos + 150, topPos + 310, 0xAAAAAAA, false);
        } else {
            // 显示历史交易统计
            int totalTransactions = transactionHistory.size();
            int totalEarnings = transactionHistory.stream()
                    .mapToInt(TransactionRecord::getPrice)
                    .sum();
            
            Component statsText = Component.translatable("gui.tradesystem.my_trades.history_stats",
                    totalTransactions, CurrencyUtil.formatMoney(totalEarnings));
            
            guiGraphics.drawString(this.font, statsText,
                    leftPos + 150, topPos + 310, 0xAAAAAAA, false);
        }
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        List<?> currentData = getCurrentData();
        int totalPages = Math.max(1, (currentData.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = currentPage < totalPages - 1;
        
        // 下架按钮只在活跃交易标签且有选中项目时可用
        unlistButton.active = currentTab == TabType.ACTIVE_TRADES && selectedActiveTrade != null;
        unlistButton.visible = currentTab == TabType.ACTIVE_TRADES;
        
        // 标签按钮状态
        activeTradesTabButton.active = currentTab != TabType.ACTIVE_TRADES;
        historyTabButton.active = currentTab != TabType.HISTORY;
    }
    
    /**
     * 获取交易历史（模拟数据）
     */
    private List<TransactionRecord> getTransactionHistory(UUID playerId) {
        // 这里应该从服务器获取真实数据
        List<TransactionRecord> history = new ArrayList<>();
        
        // 添加一些模拟数据用于测试
        long now = System.currentTimeMillis();
        
        history.add(new TransactionRecord(
                UUID.randomUUID(),
                playerId,
                "TestPlayer",
                UUID.randomUUID(),
                "Buyer1",
                new ItemStack(net.minecraft.world.item.Items.DIAMOND, 5),
                100,
                now - 3600000, // 1小时前
                TransactionRecord.Type.SELL
        ));
        
        history.add(new TransactionRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Seller1",
                playerId,
                "TestPlayer",
                new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 32),
                50,
                now - 7200000, // 2小时前
                TransactionRecord.Type.BUY
        ));
        
        return history;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理ESC键返回主界面
        if (keyCode == 256) { // ESC key
            GuiManager.openTradeMarket();
            return true;
        }
        
        // 处理Tab键切换标签
        if (keyCode == 258) { // Tab key
            if (currentTab == TabType.ACTIVE_TRADES) {
                switchToHistoryTab();
            } else {
                switchToActiveTab();
            }
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * 标签类型枚举
     */
    public enum TabType {
        ACTIVE_TRADES,
        HISTORY
    }
    
    /**
     * 交易记录类（临时定义，应该移到单独的文件中）
     */
    public static class TransactionRecord {
        private final UUID transactionId;
        private final UUID sellerId;
        private final String sellerName;
        private final UUID buyerId;
        private final String buyerName;
        private final ItemStack itemStack;
        private final int price;
        private final long timestamp;
        private final Type type;
        
        public TransactionRecord(UUID transactionId, UUID sellerId, String sellerName,
                               UUID buyerId, String buyerName, ItemStack itemStack,
                               int price, long timestamp, Type type) {
            this.transactionId = transactionId;
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
            this.itemStack = itemStack;
            this.price = price;
            this.timestamp = timestamp;
            this.type = type;
        }
        
        // Getters
        public UUID getTransactionId() { return transactionId; }
        public UUID getSellerId() { return sellerId; }
        public String getSellerName() { return sellerName; }
        public UUID getBuyerId() { return buyerId; }
        public String getBuyerName() { return buyerName; }
        public ItemStack getItemStack() { return itemStack; }
        public int getPrice() { return price; }
        public long getTimestamp() { return timestamp; }
        public Type getType() { return type; }
        
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
            return sdf.format(new Date(timestamp));
        }
        
        public enum Type {
            BUY, SELL, RECYCLE
        }
    }
}