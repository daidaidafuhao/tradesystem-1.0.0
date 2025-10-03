package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.TradeMod;
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
    private TradeButton historyTabButton;
    private TradeButton refreshButton;
    private TradeButton backButton;
    private TradeButton prevPageButton;
    private TradeButton nextPageButton;
    
    // 交易历史组件
    private List<TradeHistoryWidget> historyWidgets = new ArrayList<>();
    
    // 数据
    private List<TransactionRecord> transactionHistory = new ArrayList<>();
    private int currentPage = 0;
    private TabType currentTab = TabType.HISTORY;
    
    // 选中的项目
    private TransactionRecord selectedTransaction = null;
    private TradeHistoryWidget selectedWidget = null;
    
    public MyTradesScreen() {
        super(Component.translatable("gui.tradesystem.my_trades.title"), 300, 240);
        // 默认显示历史记录标签页
        this.currentTab = TabType.HISTORY;
    }
    
    @Override
    protected void initComponents() {
        // 只保留历史记录标签按钮，移除活跃交易标签
        historyTabButton = new TradeButton(leftPos + 10, topPos + 25, 80, 15,
                Component.translatable("gui.tradesystem.my_trades.history"),
                button -> switchToHistoryTab());
        addRenderableWidget(historyTabButton);
        
        // 刷新按钮
        refreshButton = new TradeButton(leftPos + 100, topPos + 25, 40, 15,
                Component.translatable("gui.tradesystem.sell.refresh"),
                button -> refreshData());
        addRenderableWidget(refreshButton);
        
        // 移除下架按钮，因为只显示历史记录
        
        // 创建交易历史组件 (紧凑显示)
        historyWidgets.clear();
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int y = topPos + 50 + i * 26;
            TradeHistoryWidget widget = new TradeHistoryWidget(
                    leftPos + 10, y, imageWidth - 20, 24, this::onHistoryWidgetClicked);
            historyWidgets.add(widget);
            addRenderableWidget(widget);
        }
        
        // 分页按钮
        prevPageButton = new TradeButton(leftPos + 10, topPos + 210, 40, 15,
                Component.translatable("gui.tradesystem.my_trades.prev_page"),
                button -> previousPage());
        addRenderableWidget(prevPageButton);
        
        nextPageButton = new TradeButton(leftPos + 60, topPos + 210, 40, 15,
                Component.translatable("gui.tradesystem.my_trades.next_page"),
                button -> nextPage());
        addRenderableWidget(nextPageButton);
        
        // 返回按钮
        backButton = new TradeButton(leftPos + 200, topPos + 210, 40, 15,
                Component.translatable("gui.tradesystem.button.back"),
                button -> GuiManager.openTradeMarket());
        addRenderableWidget(backButton);
        
        // 加载数据
        TradeMod.getLogger().info("MyTradesScreen 初始化完成，开始刷新数据");
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
        
        selectedTransaction = widget.getTransactionRecord();
    }
    
    /**
     * 切换到历史记录标签
     */
    private void switchToHistoryTab() {
        this.currentTab = TabType.HISTORY;
        updateDisplay();
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        TradeMod.getLogger().info("refreshData 被调用");
        
        if (minecraft == null || minecraft.player == null) {
            TradeMod.getLogger().warn("minecraft 或 player 为 null，无法刷新数据");
            return;
        }
        
        UUID playerId = minecraft.player.getUUID();
        TradeMod.getLogger().info("发送交易历史同步请求，玩家ID: {}", playerId);
        
        // 请求服务器同步交易历史
        com.tradesystem.mod.network.packet.RequestTradeHistorySyncPacket packet = 
            new com.tradesystem.mod.network.packet.RequestTradeHistorySyncPacket(playerId);
        NetworkHandler.INSTANCE.sendToServer(packet);
        
        TradeMod.getLogger().info("交易历史同步请求已发送");
        
        updateDisplay();
    }
    
    /**
     * 更新交易历史数据（由网络包调用）
     */
    public void updateTransactionHistory(List<TransactionRecord> newHistory) {
        TradeMod.getLogger().info("MyTradesScreen.updateTransactionHistory 被调用，接收到 {} 条记录", newHistory.size());
        
        this.transactionHistory = new ArrayList<>(newHistory);
        this.currentPage = 0; // 重置到第一页
        
        TradeMod.getLogger().info("交易历史已更新，当前共有 {} 条记录", this.transactionHistory.size());
        
        // 刷新显示
        updateDisplay();
        
        TradeMod.getLogger().info("界面显示已刷新");
    }
    
    /**
     * 更新显示
     */
    private void updateDisplay() {
        TradeMod.getLogger().info("updateDisplay 被调用，当前交易历史记录数: {}", transactionHistory.size());
        
        updateHistoryDisplay();
        
        // 更新按钮状态
        historyTabButton.active = true;
        
        // 更新分页按钮状态
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = (currentPage + 1) * ITEMS_PER_PAGE < transactionHistory.size();
        
        TradeMod.getLogger().info("updateDisplay 完成，当前页: {}, 分页按钮状态 - 上一页: {}, 下一页: {}", 
            currentPage, prevPageButton.active, nextPageButton.active);
    }
    
    /**
     * 更新历史记录显示
     */
    private void updateHistoryDisplay() {
        TradeMod.getLogger().info("updateHistoryDisplay 被调用，当前页: {}, 每页显示: {}", currentPage, ITEMS_PER_PAGE);
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        
        TradeMod.getLogger().info("开始索引: {}, 历史记录总数: {}, 历史组件数: {}", 
            startIndex, transactionHistory.size(), historyWidgets.size());
        
        for (int i = 0; i < historyWidgets.size(); i++) {
            TradeHistoryWidget widget = historyWidgets.get(i);
            int dataIndex = startIndex + i;
            
            if (dataIndex < transactionHistory.size()) {
                TransactionRecord record = transactionHistory.get(dataIndex);
                TradeMod.getLogger().info("设置组件 {} 显示记录: {} (卖家: {}, 买家: {})", 
                    i, record.getTransactionId(), record.getSellerName(), record.getBuyerName());
                widget.setTransactionRecord(record);
                widget.setVisible(true);
            } else {
                TradeMod.getLogger().info("隐藏组件 {} (索引 {} 超出范围)", i, dataIndex);
                widget.setVisible(false);
            }
        }
        
        TradeMod.getLogger().info("updateHistoryDisplay 完成");
    }
    
    /**
     * 获取当前标签的数据
     */
    private List<?> getCurrentData() {
        return transactionHistory;
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
     * 清除选择
     */
    private void clearSelection() {
        if (selectedWidget != null) {
            selectedWidget.setSelected(false);
            selectedWidget = null;
        }
        selectedTransaction = null;
    }
    
    /**
     * 渲染标签页指示器
     */
    private void renderTabIndicator(GuiGraphics guiGraphics) {
        // 高亮当前标签（只有历史标签）
        guiGraphics.fill(leftPos + 120, topPos + 45, leftPos + 220, topPos + 47, 0xFF00AA00);
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
    
    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        List<?> currentData = getCurrentData();
        int totalPages = Math.max(1, (currentData.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = currentPage < totalPages - 1;
        
        // 标签按钮状态
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
            switchToHistoryTab();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * 标签页类型枚举
     */
    private enum TabType {
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
            this.itemStack = itemStack.copy(); // 修复：添加copy()调用
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
        public ItemStack getItemStack() { return itemStack.copy(); }
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