package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.client.gui.widget.ItemSlotWidget;
import com.tradesystem.mod.client.gui.widget.TradeButton;
import com.tradesystem.mod.data.TradeItem;
import com.tradesystem.mod.manager.ItemListingManager;
import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.packet.UnlistItemPacket;
import com.tradesystem.mod.network.UpdatePricePacket;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;


import java.util.ArrayList;
import java.util.List;


/**
 * 商品管理界面
 * 允许玩家管理自己上架的商品，包括修改价格和下架
 */
public class ItemManagementScreen extends BaseTradeScreen {
    
    private static final int ITEMS_PER_PAGE = 15; // 5x3 网格，与BuyScreen保持一致
    private static final int SLOT_SIZE = 16; // 更小的槽位大小
    private static final int SLOT_SPACING = 18; // 槽位间距
    
    // 界面组件
    private Button prevPageButton;
    private Button nextPageButton;
    private Button backButton;
    private Button refreshButton;
    private Button updatePriceButton;
    private Button unlistButton;
    private EditBox priceEditBox;
    
    // 物品槽组件
    private final List<ItemSlotWidget> itemSlots = new ArrayList<>();
    
    // 数据
    private List<TradeItem> myListings = new ArrayList<>();
    private int currentPage = 0;
    private TradeItem selectedItem = null;
    private ItemSlotWidget selectedSlot = null;
    
    public ItemManagementScreen() {
        super(Component.translatable("gui.tradesystem.item_management.title"), 300, 220);
    }
    
    @Override
    protected void initComponents() {
        // 刷新按钮
        refreshButton = new TradeButton(leftPos + 10, topPos + 25, 50, 20,
                Component.translatable("gui.tradesystem.item_management.refresh"),
                button -> refreshData());
        addRenderableWidget(refreshButton);
        
        // 价格输入框
        priceEditBox = new EditBox(this.font, leftPos + 70, topPos + 25, 60, 20,
                Component.translatable("gui.tradesystem.item_management.new_price"));
        priceEditBox.setMaxLength(10);
        priceEditBox.setValue("");
        addRenderableWidget(priceEditBox);
        
        // 更新价格按钮
        updatePriceButton = new TradeButton(leftPos + 140, topPos + 25, 60, 20,
                Component.translatable("gui.tradesystem.item_management.update_price"),
                button -> updateSelectedItemPrice());
        addRenderableWidget(updatePriceButton);
        
        // 下架按钮
        unlistButton = new TradeButton(leftPos + 210, topPos + 25, 50, 20,
                Component.translatable("gui.tradesystem.item_management.unlist"),
                button -> unlistSelectedItem());
        addRenderableWidget(unlistButton);
        
        // 创建物品槽 (5x3 网格，与BuyScreen保持一致)
        itemSlots.clear();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                int x = leftPos + 10 + col * SLOT_SPACING;
                int y = topPos + 60 + row * SLOT_SPACING;
                
                ItemSlotWidget slot = new ItemSlotWidget(x, y, SLOT_SIZE, this::onItemSlotClicked);
                itemSlots.add(slot);
                addRenderableWidget(slot);
            }
        }
        
        // 分页按钮 (调整位置以适应新的布局)
        prevPageButton = new TradeButton(leftPos + 10, topPos + 170, 50, 20,
                Component.translatable("gui.tradesystem.item_management.prev_page"),
                button -> previousPage());
        addRenderableWidget(prevPageButton);
        
        nextPageButton = new TradeButton(leftPos + 70, topPos + 170, 50, 20,
                Component.translatable("gui.tradesystem.item_management.next_page"),
                button -> nextPage());
        addRenderableWidget(nextPageButton);
        
        // 返回按钮
        backButton = new TradeButton(leftPos + 200, topPos + 170, 50, 20,
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
        
        // 渲染分页信息
        renderPageInfo(guiGraphics);
        
        // 渲染选中物品信息
        renderSelectedItemInfo(guiGraphics);
        
        // 更新按钮状态
        updateButtonStates();
    }
    
    @Override
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染物品槽工具提示
        for (ItemSlotWidget slot : itemSlots) {
            if (slot.isMouseOver(mouseX, mouseY) && slot.visible && slot.getTradeItem() != null) {
                TradeItem item = slot.getTradeItem();
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(item.getItemStack().getHoverName());
                tooltip.add(Component.translatable("gui.tradesystem.tooltip.price", 
                        CurrencyUtil.formatMoney(item.getPrice())));
                tooltip.add(Component.translatable("gui.tradesystem.tooltip.quantity", 
                        item.getItemStack().getCount()));
                tooltip.add(Component.translatable("gui.tradesystem.item_management.tooltip.click_to_select"));
                
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
        
        // 渲染按钮工具提示
        if (updatePriceButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.tradesystem.item_management.tooltip.update_price"), 
                    mouseX, mouseY);
        } else if (unlistButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.tradesystem.item_management.tooltip.unlist"), 
                    mouseX, mouseY);
        }
    }
    
    /**
     * 物品槽点击事件
     */
    private void onItemSlotClicked(ItemSlotWidget slot) {
        // 清除之前的选择
        if (selectedSlot != null) {
            selectedSlot.setSelected(false);
        }
        
        // 设置新的选择
        selectedSlot = slot;
        selectedItem = slot.getTradeItem();
        
        if (selectedItem != null) {
            slot.setSelected(true);
            priceEditBox.setValue(String.valueOf(selectedItem.getPrice()));
        } else {
            priceEditBox.setValue("");
        }
    }
    
    /**
     * 更新选中物品的价格
     */
    private void updateSelectedItemPrice() {
        if (selectedItem == null) {
            return;
        }
        
        try {
            int newPrice = Integer.parseInt(priceEditBox.getValue());
            if (newPrice <= 0) {
                // 显示错误消息
                return;
            }
            
            // 发送更新价格的网络包
            NetworkHandler.sendToServer(new UpdatePricePacket(selectedItem.getId(), newPrice));
            
            // 刷新数据
            refreshData();
            
        } catch (NumberFormatException e) {
            // 价格格式错误
            priceEditBox.setValue(String.valueOf(selectedItem.getPrice()));
        }
    }
    
    /**
     * 下架选中的物品
     */
    private void unlistSelectedItem() {
        if (selectedItem != null) {
            // 发送下架请求到服务器
            NetworkHandler.sendToServer(new UnlistItemPacket(selectedItem.getId()));
            
            // 添加调试日志
            com.tradesystem.mod.TradeMod.getLogger().debug("发送下架请求: 物品ID={}, 物品名称={}", 
                    selectedItem.getId(), selectedItem.getDisplayName());
            
            // 不在这里立即刷新，等待服务器响应后再刷新
        }
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        if (minecraft.player != null) {
            // 在客户端环境下，从客户端缓存获取玩家自己的商品
            myListings = ItemListingManager.getInstance()
                    .getPlayerListings(minecraft.player.getUUID());
            
            // 添加调试日志
            com.tradesystem.mod.TradeMod.getLogger().debug("ItemManagementScreen刷新数据: 玩家={}, 商品数量={}", 
                    minecraft.player.getName().getString(), myListings.size());
            
            updateDisplay();
        }
    }
    
    /**
     * 更新显示
     */
    private void updateDisplay() {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        
        for (int i = 0; i < itemSlots.size(); i++) {
            ItemSlotWidget slot = itemSlots.get(i);
            int itemIndex = startIndex + i;
            
            if (itemIndex < myListings.size()) {
                TradeItem item = myListings.get(itemIndex);
                slot.setTradeItem(item);
                slot.setVisible(true);
            } else {
                slot.setTradeItem(null);
                slot.setVisible(false);
            }
        }
    }
    
    /**
     * 上一页
     */
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            clearSelection();
            updateDisplay();
        }
    }
    
    /**
     * 下一页
     */
    private void nextPage() {
        int maxPage = (myListings.size() - 1) / ITEMS_PER_PAGE;
        if (currentPage < maxPage) {
            currentPage++;
            clearSelection();
            updateDisplay();
        }
    }
    
    /**
     * 清除选择
     */
    public void clearSelection() {
        if (selectedSlot != null) {
            selectedSlot.setSelected(false);
            selectedSlot = null;
        }
        selectedItem = null;
        priceEditBox.setValue("");
    }
    
    /**
     * 渲染分页信息
     */
    private void renderPageInfo(GuiGraphics guiGraphics) {
        int totalPages = Math.max(1, (myListings.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        Component pageText = Component.translatable("gui.tradesystem.page_info", 
                currentPage + 1, totalPages);
        
        int textWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText,
                leftPos + (imageWidth - textWidth) / 2, topPos + 150, TEXT_COLOR, false);
    }
    
    /**
     * 渲染选中物品信息
     */
    private void renderSelectedItemInfo(GuiGraphics guiGraphics) {
        if (selectedItem != null) {
            Component selectedText = Component.translatable("gui.tradesystem.item_management.selected_item",
                    selectedItem.getDisplayName());
            guiGraphics.drawString(this.font, selectedText,
                    leftPos + 10, topPos + 50, 0xFFFFAA, false);
        } else {
            Component noSelectionText = Component.translatable("gui.tradesystem.item_management.no_selection");
            guiGraphics.drawString(this.font, noSelectionText,
                    leftPos + 10, topPos + 50, 0xAAAAAA, false);
        }
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = selectedItem != null;
        boolean hasValidPrice = false;
        
        try {
            int price = Integer.parseInt(priceEditBox.getValue());
            hasValidPrice = price > 0;
        } catch (NumberFormatException e) {
            hasValidPrice = false;
        }
        
        updatePriceButton.active = hasSelection && hasValidPrice;
        unlistButton.active = hasSelection;
        
        int totalPages = Math.max(1, (myListings.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = currentPage < totalPages - 1;
    }
}