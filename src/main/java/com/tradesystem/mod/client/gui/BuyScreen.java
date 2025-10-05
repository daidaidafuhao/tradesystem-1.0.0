package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.client.gui.widget.ItemSlotWidget;
import com.tradesystem.mod.client.gui.widget.TradeButton;
import com.tradesystem.mod.client.ClientSystemItemManager;
import com.tradesystem.mod.data.TradeItem;
import com.tradesystem.mod.data.SystemItem;
import com.tradesystem.mod.manager.ItemListingManager;

import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.PurchaseItemPacket;
import com.tradesystem.mod.network.packet.PurchaseSystemItemPacket;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

/**
 * 购买界面
 * 显示所有可购买的物品，支持搜索和分页
 */
public class BuyScreen extends BaseTradeScreen {
    
    private static final int ITEMS_PER_PAGE = 15; // 5x3 网格
    
    // 界面组件
    private EditBox searchBox;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button backButton;
    private Button refreshButton;
    private Button sortButton;
    
    // 物品槽位
    private final List<ItemSlotWidget> itemSlots = new ArrayList<>();
    
    // 数据
    private List<TradeItem> allItems = new ArrayList<>();
    private List<SystemItem> systemItems = new ArrayList<>();
    private List<Object> allDisplayItems = new ArrayList<>(); // 包含TradeItem和SystemItem
    private List<Object> filteredItems = new ArrayList<>();
    private int currentPage = 0;
    private String currentSearch = "";
    private SortType currentSort = SortType.NEWEST;
    
    // 选中的物品
    private Object selectedItem = null; // 可能是TradeItem或SystemItem
    private ItemSlotWidget selectedSlot = null;
    
    // 模态框状态
    private boolean showingModal = false;
    private String modalMessage = "";
    private long modalStartTime = 0;
    
    public BuyScreen() {
        super(Component.translatable("gui.tradesystem.buy.title"), 300, 200);
    }
    
    @Override
    protected void initComponents() {
        // 搜索框
        searchBox = new EditBox(this.font, leftPos + 10, topPos + 25, 120, 15, 
                Component.translatable("gui.tradesystem.buy.search"));
        searchBox.setHint(Component.translatable("gui.tradesystem.buy.search.hint"));
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);
        
        // 刷新按钮（移动到数量输入框的位置）
        refreshButton = new TradeButton(leftPos + 140, topPos + 25, 40, 15,
                Component.translatable("gui.tradesystem.buy.refresh"),
                button -> refreshItems());
        addRenderableWidget(refreshButton);
        
        // 排序按钮
        sortButton = new TradeButton(leftPos + 190, topPos + 25, 60, 15,
                getSortButtonText(),
                button -> cycleSortType());
        addRenderableWidget(sortButton);
        
        // 创建物品槽位 (5x3 网格，更紧凑)
        itemSlots.clear();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                int x = leftPos + 10 + col * 16;
                int y = topPos + 50 + row * 16;
                ItemSlotWidget slot = new ItemSlotWidget(x, y, 14, this::onItemSlotClicked);
                itemSlots.add(slot);
                addRenderableWidget(slot);
            }
        }
        
        // 分页按钮
        prevPageButton = new TradeButton(leftPos + 10, topPos + 160, 40, 15,
                Component.translatable("gui.tradesystem.buy.prev_page"),
                button -> previousPage());
        addRenderableWidget(prevPageButton);
        
        nextPageButton = new TradeButton(leftPos + 60, topPos + 160, 40, 15,
                Component.translatable("gui.tradesystem.buy.next_page"),
                button -> nextPage());
        addRenderableWidget(nextPageButton);
        
        // 购买按钮
        Button buyButton = new TradeButton(leftPos + 120, topPos + 160, 50, 15,
                Component.translatable("gui.tradesystem.buy.purchase"),
                button -> purchaseSelectedItem());
        addRenderableWidget(buyButton);
        
        // 返回按钮
        backButton = new TradeButton(leftPos + 180, topPos + 160, 40, 15,
                Component.translatable("gui.tradesystem.button.back"),
                button -> GuiManager.openTradeMarket());
        addRenderableWidget(backButton);
        
        // 加载物品数据
        refreshItems();
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
        
        // 渲染模态框
        if (showingModal) {
            renderModal(guiGraphics);
        }
    }
    
    @Override
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染物品槽位的工具提示
        for (ItemSlotWidget slot : itemSlots) {
            if (slot.isMouseOver(mouseX, mouseY) && slot.hasItem()) {
                TradeItem item = slot.getTradeItem();
                if (item != null) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(item.getItemStack().getHoverName());
                    
                    // 显示总价格（单价 x 数量）
                    int totalPrice = item.getPrice() * item.getItemStack().getCount();
                    tooltip.add(Component.translatable("gui.tradesystem.buy.total_price", 
                            CurrencyUtil.formatMoney(totalPrice)));
                    tooltip.add(Component.translatable("gui.tradesystem.buy.unit_price", 
                            CurrencyUtil.formatMoney(item.getPrice())));
                    tooltip.add(Component.translatable("gui.tradesystem.buy.quantity", 
                            item.getItemStack().getCount()));
                    tooltip.add(Component.translatable("gui.tradesystem.buy.seller", 
                            item.getSellerName()));
                    
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                }
            }
        }
    }
    
    /**
     * 搜索框内容变化处理
     */
    private void onSearchChanged(String search) {
        currentSearch = search.toLowerCase();
        currentPage = 0;
        filterAndSortItems();
        updateItemSlots();
    }
    
    /**
     * 物品槽位点击处理
     */
    private void onItemSlotClicked(ItemSlotWidget slot) {
        if (selectedSlot != null) {
            selectedSlot.setSelected(false);
        }
        
        selectedSlot = slot;
        
        // 根据槽位类型设置选中的物品
        if (slot.isSystemItem()) {
            selectedItem = slot.getSystemItem();
        } else {
            selectedItem = slot.getTradeItem();
        }
        
        if (selectedSlot != null) {
            selectedSlot.setSelected(true);
        }
    }
    
    /**
     * 刷新物品列表（公共方法）
     */
    public void refreshItems() {
        // 从ItemListingManager获取玩家交易物品
        allItems = ItemListingManager.getInstance().getAllActiveListings();
        
        // 从ClientSystemItemManager获取系统商品
        systemItems = ClientSystemItemManager.getInstance().getActiveSystemItems();
        
        // 合并所有物品
        allDisplayItems.clear();
        allDisplayItems.addAll(allItems);
        allDisplayItems.addAll(systemItems);
        
        filterAndSortItems();
        updateItemSlots();
    }
    
    /**
     * 过滤和排序物品
     */
    private void filterAndSortItems() {
        filteredItems = allDisplayItems.stream()
                .filter(item -> {
                    if (currentSearch.isEmpty()) {
                        return true;
                    }
                    String displayName = getItemDisplayName(item);
                    String sellerName = getItemSellerName(item);
                    return displayName.toLowerCase().contains(currentSearch) ||
                           sellerName.toLowerCase().contains(currentSearch);
                })
                .sorted((a, b) -> {
                    switch (currentSort) {
                        case PRICE_LOW_TO_HIGH:
                            return Integer.compare(getItemPrice(a), getItemPrice(b));
                        case PRICE_HIGH_TO_LOW:
                            return Integer.compare(getItemPrice(b), getItemPrice(a));
                        case NAME_A_TO_Z:
                            return getItemDisplayName(a).compareToIgnoreCase(getItemDisplayName(b));
                        case NAME_Z_TO_A:
                            return getItemDisplayName(b).compareToIgnoreCase(getItemDisplayName(a));
                        case NEWEST:
                        default:
                            return Long.compare(getItemListTime(b), getItemListTime(a));
                    }
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取物品显示名称
     */
    private String getItemDisplayName(Object item) {
        if (item instanceof TradeItem) {
            return ((TradeItem) item).getDisplayName();
        } else if (item instanceof SystemItem) {
            return ((SystemItem) item).getItemStack().getHoverName().getString();
        }
        return "";
    }
    
    /**
     * 获取物品卖家名称
     */
    private String getItemSellerName(Object item) {
        if (item instanceof TradeItem) {
            return ((TradeItem) item).getSellerName();
        } else if (item instanceof SystemItem) {
            return "System"; // 系统商品
        }
        return "";
    }
    
    /**
     * 获取物品价格
     */
    private int getItemPrice(Object item) {
        if (item instanceof TradeItem) {
            return ((TradeItem) item).getPrice();
        } else if (item instanceof SystemItem) {
            return ((SystemItem) item).getPrice();
        }
        return 0;
    }
    
    /**
     * 获取物品上架时间
     */
    private long getItemListTime(Object item) {
        if (item instanceof TradeItem) {
            return ((TradeItem) item).getListTime();
        } else if (item instanceof SystemItem) {
            return 0; // 系统商品没有上架时间，设为0
        }
        return 0;
    }
    
    /**
     * 更新物品槽位显示
     */
    private void updateItemSlots() {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        
        for (int i = 0; i < itemSlots.size(); i++) {
            ItemSlotWidget slot = itemSlots.get(i);
            int itemIndex = startIndex + i;
            
            if (itemIndex < filteredItems.size()) {
                Object item = filteredItems.get(itemIndex);
                if (item instanceof TradeItem) {
                    slot.setTradeItem((TradeItem) item);
                } else if (item instanceof SystemItem) {
                    slot.setSystemItem((SystemItem) item);
                }
                slot.setVisible(true);
            } else {
                slot.setTradeItem(null);
                slot.setSystemItem(null);
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
            updateItemSlots();
        }
    }
    
    /**
     * 下一页
     */
    private void nextPage() {
        int maxPage = (filteredItems.size() - 1) / ITEMS_PER_PAGE;
        if (currentPage < maxPage) {
            currentPage++;
            updateItemSlots();
        }
    }
    
    /**
     * 切换排序类型
     */
    private void cycleSortType() {
        SortType[] values = SortType.values();
        int currentIndex = currentSort.ordinal();
        currentSort = values[(currentIndex + 1) % values.length];
        sortButton.setMessage(getSortButtonText());
        
        filterAndSortItems();
        updateItemSlots();
    }
    
    /**
     * 获取排序按钮文本
     */
    private Component getSortButtonText() {
        return Component.translatable("gui.tradesystem.buy.sort." + currentSort.name().toLowerCase());
    }
    
    /**
     * 购买选中的物品（一次性购买全部数量）
     */
    private void purchaseSelectedItem() {
        if (selectedItem == null) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.buy.no_item_selected"));
            }
            return;
        }
        
        if (selectedItem instanceof TradeItem) {
            purchaseTradeItem((TradeItem) selectedItem);
        } else if (selectedItem instanceof SystemItem) {
            purchaseSystemItem((SystemItem) selectedItem);
        }
    }
    
    /**
     * 购买玩家交易物品
     */
    private void purchaseTradeItem(TradeItem tradeItem) {
        // 获取商品的全部数量
        int quantity = tradeItem.getItemStack().getCount();
        
        // 检查玩家是否有足够的金币
        if (minecraft != null && minecraft.player != null) {
            int playerMoney = com.tradesystem.mod.client.ClientCurrencyManager.getInstance().getPlayerMoney();
            int totalPrice = tradeItem.getPrice() * quantity;
            if (playerMoney < totalPrice) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.buy.insufficient_money",
                                CurrencyUtil.formatMoney(totalPrice)));
                return;
            }
        }
        
        // 发送购买请求到服务器（购买全部数量）
        NetworkHandler.sendToServer(new PurchaseItemPacket(tradeItem.getId(), quantity));
        
        // 显示模态框
        showModal(Component.translatable("gui.tradesystem.buy.purchasing_all",
                tradeItem.getDisplayName(), quantity, 
                CurrencyUtil.formatMoney(tradeItem.getPrice() * quantity)).getString());
        
        // 清除选择并延迟刷新
        clearSelectionAndRefresh();
    }
    
    /**
     * 购买系统商品
     */
    private void purchaseSystemItem(SystemItem systemItem) {
        // 系统商品默认购买1个
        int quantity = 1;
        
        // 检查玩家是否有足够的金币
        if (minecraft != null && minecraft.player != null) {
            int playerMoney = com.tradesystem.mod.client.ClientCurrencyManager.getInstance().getPlayerMoney();
            int totalPrice = systemItem.getPrice() * quantity;
            if (playerMoney < totalPrice) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.buy.insufficient_money",
                                CurrencyUtil.formatMoney(totalPrice)));
                return;
            }
        }
        
        // 发送购买系统商品请求到服务器
        NetworkHandler.sendToServer(new PurchaseSystemItemPacket(systemItem.getId(), quantity));
        
        // 显示模态框
        showModal(Component.translatable("gui.tradesystem.buy.purchasing_system",
                systemItem.getItemStack().getHoverName().getString(), quantity, 
                CurrencyUtil.formatMoney(systemItem.getPrice() * quantity)).getString());
        
        // 清除选择并延迟刷新
        clearSelectionAndRefresh();
    }
    
    /**
     * 清除选择并延迟刷新
     */
    private void clearSelectionAndRefresh() {
        // 清除选择
        selectedItem = null;
        if (selectedSlot != null) {
            selectedSlot.setSelected(false);
            selectedSlot = null;
        }
        
        // 延迟关闭模态框并刷新，等待服务器处理完成
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待1秒
                if (minecraft != null) {
                    minecraft.execute(() -> {
                        hideModal();
                        refreshItems();
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 渲染分页信息
     */
    private void renderPageInfo(GuiGraphics guiGraphics) {
        int totalPages = Math.max(1, (filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        Component pageText = Component.translatable("gui.tradesystem.buy.page_info",
                currentPage + 1, totalPages, filteredItems.size());
        
        int textWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText,
                leftPos + (imageWidth - textWidth) / 2, topPos + 140, TEXT_COLOR, false);
    }
    
    /**
     * 渲染选中物品信息
     */
    private void renderSelectedItemInfo(GuiGraphics guiGraphics) {
        if (selectedItem != null) {
            String displayName;
            int price;
            int quantity;
            
            if (selectedItem instanceof TradeItem) {
                TradeItem tradeItem = (TradeItem) selectedItem;
                displayName = tradeItem.getDisplayName();
                price = tradeItem.getPrice();
                quantity = tradeItem.getItemStack().getCount();
            } else if (selectedItem instanceof SystemItem) {
                SystemItem systemItem = (SystemItem) selectedItem;
                displayName = systemItem.getItemStack().getHoverName().getString();
                price = systemItem.getPrice();
                quantity = 1; // 系统商品默认购买1个
            } else {
                return;
            }
            
            int totalPrice = price * quantity;
            Component selectedText = Component.translatable("gui.tradesystem.buy.selected_total",
                    displayName, quantity, CurrencyUtil.formatMoney(totalPrice));
            
            guiGraphics.drawString(this.font, selectedText,
                    leftPos + 10, topPos + 125, 0xFFFFAA, false);
        }
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        int totalPages = Math.max(1, (filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = currentPage < totalPages - 1;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 如果显示模态框，阻止所有按键输入
        if (showingModal) {
            return true;
        }
        
        // 处理ESC键返回主界面
        if (keyCode == 256) { // ESC key
            GuiManager.openTradeMarket();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 如果显示模态框，阻止所有鼠标点击
        if (showingModal) {
            return true;
        }
        
        // 首先让父类处理按钮点击
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // 然后处理物品槽位点击
        for (ItemSlotWidget slot : itemSlots) {
            if (slot.isMouseOver(mouseX, mouseY) && slot.hasItem()) {
                onItemSlotClicked(slot);
                return true;
            }
        }
        
        return false;
     }
     
     /**
      * 显示模态框
      */
     private void showModal(String message) {
         showingModal = true;
         modalMessage = message;
         modalStartTime = System.currentTimeMillis();
     }
     
     /**
      * 隐藏模态框
      */
     private void hideModal() {
         showingModal = false;
         modalMessage = "";
         modalStartTime = 0;
     }
     
     /**
      * 渲染模态框
      */
     private void renderModal(GuiGraphics guiGraphics) {
         // 渲染半透明背景
         guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
         
         // 计算模态框尺寸和位置
         int modalWidth = 200;
         int modalHeight = 60;
         int modalX = (this.width - modalWidth) / 2;
         int modalY = (this.height - modalHeight) / 2;
         
         // 渲染模态框背景
         guiGraphics.fill(modalX - 1, modalY - 1, modalX + modalWidth + 1, modalY + modalHeight + 1, 0xFF8B8B8B);
         guiGraphics.fill(modalX, modalY, modalX + modalWidth, modalY + modalHeight, 0xFF000000);
         
         // 渲染消息文本
         Component messageComponent = Component.literal(modalMessage);
         int textWidth = this.font.width(messageComponent);
         int textX = modalX + (modalWidth - textWidth) / 2;
         int textY = modalY + 20;
         guiGraphics.drawString(this.font, messageComponent, textX, textY, 0xFFFFFF);
         
         // 渲染加载动画（简单的点点点）
         long elapsed = System.currentTimeMillis() - modalStartTime;
         int dots = (int) ((elapsed / 500) % 4); // 每500ms切换一次
         String loadingText = "请稍候" + ".".repeat(dots);
         Component loadingComponent = Component.literal(loadingText);
         int loadingWidth = this.font.width(loadingComponent);
         int loadingX = modalX + (modalWidth - loadingWidth) / 2;
         int loadingY = modalY + 35;
         guiGraphics.drawString(this.font, loadingComponent, loadingX, loadingY, 0xFFD700);
     }
     
     /**
      * 排序类型枚举
      */
     public enum SortType {
         NEWEST,
         PRICE_LOW_TO_HIGH,
         PRICE_HIGH_TO_LOW,
         NAME_A_TO_Z,
         NAME_Z_TO_A
     }
}