package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.client.gui.widget.ItemSlotWidget;
import com.tradesystem.mod.client.gui.widget.TradeButton;

import com.tradesystem.mod.manager.RecycleManager;
import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.ListItemPacket;
import com.tradesystem.mod.network.RecycleItemPacket;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 出售界面
 * 显示玩家背包物品，支持上架出售和系统回收
 */
public class SellScreen extends BaseTradeScreen {
    
    private static final int INVENTORY_SLOTS = 36; // 玩家背包槽位数

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 20;
    
    // 界面组件
    private EditBox priceBox;
    private EditBox quantityBox;
    private Button listItemButton;

    private Button backButton;
    private Button refreshButton;
    private Button selectAllButton;
    private Button clearSelectionButton;
    
    // 物品槽位
    private final List<ItemSlotWidget> inventorySlots = new ArrayList<>();
    
    // 选中的物品
    private final List<ItemSlotWidget> selectedSlots = new ArrayList<>();
    private ItemSlotWidget primarySelectedSlot = null;
    
    // 界面模式
    private SellMode currentMode = SellMode.LIST_ITEM;
    
    // 模态框状态
    private boolean showingModal = false;
    private String modalMessage = "";
    private long modalStartTime = 0;
    
    public SellScreen() {
        super(Component.translatable("gui.tradesystem.sell.title"), 400, 220);
    }
    
    @Override
    protected void initComponents() {
        // 第一行按钮
        // 模式切换按钮
        Button modeButton = new TradeButton(leftPos + 10, topPos + 25, 80, 20,
                getModeButtonText(),
                button -> switchMode());
        addRenderableWidget(modeButton);
        
        // 刷新按钮
        refreshButton = new TradeButton(leftPos + 100, topPos + 25, 60, 20,
                Component.translatable("gui.tradesystem.sell.refresh"),
                button -> refreshInventory());
        addRenderableWidget(refreshButton);
        
        // 选择所有按钮
        selectAllButton = new TradeButton(leftPos + 170, topPos + 25, 70, 20,
                Component.translatable("gui.tradesystem.sell.select_all"),
                button -> selectAllItems());
        addRenderableWidget(selectAllButton);
        
        // 清除选择按钮
        clearSelectionButton = new TradeButton(leftPos + 250, topPos + 25, 70, 20,
                Component.translatable("gui.tradesystem.sell.clear_selection"),
                button -> clearSelection());
        addRenderableWidget(clearSelectionButton);
        
        // 第二行：价格、数量和操作按钮
        // 价格输入框（仅在上架模式显示）
        priceBox = new EditBox(this.font, leftPos + 10, topPos + 50, 80, 15,
                Component.translatable("gui.tradesystem.sell.price"));
        priceBox.setHint(Component.translatable("gui.tradesystem.sell.price.hint"));
        priceBox.setFilter(this::isValidPriceInput);
        addRenderableWidget(priceBox);
        
        // 数量输入框（仅在上架模式显示）
        quantityBox = new EditBox(this.font, leftPos + 100, topPos + 50, 60, 15,
                Component.translatable("gui.tradesystem.sell.quantity"));
        quantityBox.setHint(Component.translatable("gui.tradesystem.sell.quantity.hint"));
        quantityBox.setFilter(this::isValidQuantityInput);
        quantityBox.setValue("1"); // 默认数量为1
        addRenderableWidget(quantityBox);
        
        // 上架/回收按钮
        listItemButton = new TradeButton(leftPos + 170, topPos + 50, 80, 20,
                getActionButtonText(),
                button -> performAction());
        addRenderableWidget(listItemButton);
        
        // 创建背包槽位 (9x4 网格)
        inventorySlots.clear();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int x = leftPos + 10 + col * SLOT_SPACING;
                int y = topPos + 80 + row * SLOT_SPACING;
                ItemSlotWidget slot = new ItemSlotWidget(x, y, SLOT_SIZE, this::onInventorySlotClicked);
                inventorySlots.add(slot);
                addRenderableWidget(slot);
            }
        }
        
        // 返回按钮
        backButton = new TradeButton(leftPos + 320, topPos + 165, 60, 20,
                Component.translatable("gui.tradesystem.button.back"),
                button -> GuiManager.openTradeMarket());
        addRenderableWidget(backButton);
        
        // 加载背包数据
        refreshInventory();
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染标题
        renderTitle(guiGraphics);
        
        // 渲染玩家金币信息
        renderPlayerMoney(guiGraphics);
        
        // 渲染选中物品信息
        renderSelectedItemsInfo(guiGraphics);
        
        // 渲染预估价值信息
        renderEstimatedValue(guiGraphics);
        
        // 更新组件可见性
        updateComponentVisibility();
        
        // 渲染模态框
        if (showingModal) {
            renderModal(guiGraphics);
        }
    }
    
    @Override
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染背包槽位的工具提示
        for (ItemSlotWidget slot : inventorySlots) {
            if (slot.isMouseOver(mouseX, mouseY) && slot.hasItem()) {
                ItemStack itemStack = slot.getItemStack();
                if (!itemStack.isEmpty()) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(itemStack.getHoverName());
                    tooltip.add(Component.translatable("gui.tradesystem.sell.quantity", 
                            itemStack.getCount()));
                    
                    if (currentMode == SellMode.RECYCLE) {
                        int recyclePrice = RecycleManager.getInstance().getRecyclePricePreview(itemStack);
                        if (recyclePrice > 0) {
                            tooltip.add(Component.translatable("gui.tradesystem.sell.recycle_value",
                                    CurrencyUtil.formatMoney(recyclePrice)));
                        } else {
                            tooltip.add(Component.translatable("gui.tradesystem.sell.not_recyclable"));
                        }
                    }
                    
                    guiGraphics.renderTooltip(this.font, tooltip.stream().map(Component::getVisualOrderText).collect(java.util.stream.Collectors.toList()), mouseX, mouseY);
                }
            }
        }
    }
    
    /**
     * 背包槽位点击处理
     */
    private void onInventorySlotClicked(ItemSlotWidget slot) {
        if (!slot.hasItem()) {
            return;
        }
        
        // 检查是否按住Ctrl键进行多选
        boolean isCtrlPressed = hasControlDown();
        
        if (isCtrlPressed) {
            // 多选模式
            if (selectedSlots.contains(slot)) {
                selectedSlots.remove(slot);
                slot.setSelected(false);
            } else {
                selectedSlots.add(slot);
                slot.setSelected(true);
            }
        } else {
            // 单选模式
            clearSelection();
            selectedSlots.add(slot);
            slot.setSelected(true);
            primarySelectedSlot = slot;
        }
    }
    
    /**
     * 刷新背包显示
     */
    private void refreshInventory() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        
        Inventory inventory = minecraft.player.getInventory();
        
        for (int i = 0; i < Math.min(INVENTORY_SLOTS, inventorySlots.size()); i++) {
            ItemSlotWidget slot = inventorySlots.get(i);
            ItemStack itemStack = inventory.getItem(i);
            
            slot.setItemStack(itemStack);
            slot.setVisible(!itemStack.isEmpty());
            
            // 在回收模式下，标记不可回收的物品
            if (currentMode == SellMode.RECYCLE && !itemStack.isEmpty()) {
                boolean recyclable = RecycleManager.getInstance().isItemRecyclable(itemStack);
                slot.setEnabled(recyclable);
            } else {
                slot.setEnabled(true);
            }
        }
    }
    
    /**
     * 切换模式
     */
    private void switchMode() {
        currentMode = currentMode == SellMode.LIST_ITEM ? SellMode.RECYCLE : SellMode.LIST_ITEM;
        clearSelection();
        refreshInventory();
        
        // 更新按钮文本
        for (net.minecraft.client.gui.components.Renderable widget : renderables) {
            if (widget instanceof Button button) {
                if (button.getMessage().getString().contains("上架") || 
                    button.getMessage().getString().contains("回收") ||
                    button.getMessage().getString().contains("List") ||
                    button.getMessage().getString().contains("Recycle")) {
                    button.setMessage(getModeButtonText());
                    break;
                }
            }
        }
        
        listItemButton.setMessage(getActionButtonText());
    }
    
    /**
     * 执行操作（上架或回收）
     */
    private void performAction() {
        if (selectedSlots.isEmpty()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.sell.no_items_selected"));
            }
            return;
        }
        
        if (currentMode == SellMode.LIST_ITEM) {
            listSelectedItems();
        } else {
            recycleSelectedItems();
        }
    }
    
    /**
     * 上架选中的物品
     */
    private void listSelectedItems() {
        String priceText = priceBox.getValue().trim();
        String quantityText = quantityBox.getValue().trim();
        
        if (priceText.isEmpty()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.sell.enter_price"));
            }
            return;
        }
        
        if (quantityText.isEmpty()) {
            quantityBox.setValue("1");
            quantityText = "1";
        }
        
        int price;
        int quantity;
        try {
            price = Integer.parseInt(priceText);
            if (price <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.sell.invalid_price"));
            }
            return;
        }
        
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(
                        Component.translatable("gui.tradesystem.sell.invalid_quantity"));
            }
            return;
        }
        
        // 发送上架请求到服务器
        for (ItemSlotWidget slot : selectedSlots) {
            if (slot.hasItem()) {
                int slotIndex = inventorySlots.indexOf(slot);
                NetworkHandler.sendToServer(new ListItemPacket(slotIndex, price, quantity));
            }
        }
        
        // 显示模态框
        showModal(Component.translatable("gui.tradesystem.sell.items_listing",
                selectedSlots.size()).getString());
        
        // 清除选择和价格
        clearSelection();
        priceBox.setValue("");
        quantityBox.setValue("1");
        
        // 延迟关闭模态框并刷新，等待服务器处理完成
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待1秒
                if (minecraft != null) {
                    minecraft.execute(() -> {
                        hideModal();
                        refreshInventory();
                        // 通知购买界面刷新（如果打开的话）
                        if (minecraft.screen instanceof BuyScreen buyScreen) {
                            buyScreen.refreshItems();
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 回收选中的物品
     */
    private void recycleSelectedItems() {
        List<Integer> slotIndices = new ArrayList<>();
        int totalValue = 0;
        
        for (ItemSlotWidget slot : selectedSlots) {
            if (slot.hasItem()) {
                int slotIndex = inventorySlots.indexOf(slot);
                slotIndices.add(slotIndex);
                
                ItemStack itemStack = slot.getItemStack();
                totalValue += RecycleManager.getInstance().getRecyclePricePreview(itemStack);
            }
        }
        
        if (slotIndices.isEmpty()) {
            return;
        }
        
        // 发送回收请求到服务器
        NetworkHandler.sendToServer(new RecycleItemPacket(slotIndices));
        
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(
                    Component.translatable("gui.tradesystem.sell.items_recycled",
                            slotIndices.size(), CurrencyUtil.formatMoney(totalValue)));
        }
        
        // 清除选择
        clearSelection();
        refreshInventory();
    }
    
    /**
     * 选择所有物品
     */
    private void selectAllItems() {
        clearSelection();
        
        for (ItemSlotWidget slot : inventorySlots) {
            if (slot.hasItem() && slot.isActive()) {
                selectedSlots.add(slot);
                slot.setSelected(true);
            }
        }
    }
    
    /**
     * 清除选择
     */
    private void clearSelection() {
        for (ItemSlotWidget slot : selectedSlots) {
            slot.setSelected(false);
        }
        selectedSlots.clear();
        primarySelectedSlot = null;
    }
    
    /**
     * 验证价格输入
     */
    private boolean isValidPriceInput(String input) {
        if (input.isEmpty()) {
            return true;
        }
        
        try {
            int price = Integer.parseInt(input);
            return price > 0 && price <= 999999;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 验证数量输入
     */
    private boolean isValidQuantityInput(String input) {
        if (input.isEmpty()) {
            return true;
        }
        try {
            int quantity = Integer.parseInt(input);
            return quantity > 0 && quantity <= 64; // 限制最大数量为64
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 获取模式按钮文本
     */
    private Component getModeButtonText() {
        return Component.translatable("gui.tradesystem.sell.mode." + 
                currentMode.name().toLowerCase());
    }
    
    /**
     * 获取操作按钮文本
     */
    private Component getActionButtonText() {
        return currentMode == SellMode.LIST_ITEM ?
                Component.translatable("gui.tradesystem.sell.list_item") :
                Component.translatable("gui.tradesystem.sell.recycle");
    }
    
    /**
     * 渲染选中物品信息
     */
    private void renderSelectedItemsInfo(GuiGraphics guiGraphics) {
        if (!selectedSlots.isEmpty()) {
            Component selectedText = Component.translatable("gui.tradesystem.sell.selected_count",
                    selectedSlots.size());
            
            guiGraphics.drawString(this.font, selectedText,
                    leftPos + 10, topPos + 310, 0xFFFFAA, false);
        }
    }
    
    /**
     * 渲染预估价值信息
     */
    private void renderEstimatedValue(GuiGraphics guiGraphics) {
        if (!selectedSlots.isEmpty() && currentMode == SellMode.RECYCLE) {
            int totalValue = 0;
            for (ItemSlotWidget slot : selectedSlots) {
                if (slot.hasItem()) {
                    totalValue += RecycleManager.getInstance().getRecyclePricePreview(slot.getItemStack());
                }
            }
            
            if (totalValue > 0) {
                Component valueText = Component.translatable("gui.tradesystem.sell.estimated_value",
                        CurrencyUtil.formatMoney(totalValue));
                
                guiGraphics.drawString(this.font, valueText,
                        leftPos + 10, topPos + 330, 0x00FF00, false);
            }
        }
    }
    
    /**
     * 更新组件可见性
     */
    private void updateComponentVisibility() {
        boolean isListMode = currentMode == SellMode.LIST_ITEM;
        priceBox.setVisible(isListMode);
        quantityBox.setVisible(isListMode);
        listItemButton.active = !selectedSlots.isEmpty() && 
                (currentMode == SellMode.RECYCLE || !priceBox.getValue().trim().isEmpty());
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
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 如果显示模态框，阻止所有按键输入（除了ESC）
        if (showingModal && keyCode != 256) {
            return true;
        }
        
        // 处理ESC键返回主界面
        if (keyCode == 256) { // ESC key
            if (showingModal) {
                // 如果显示模态框，ESC键隐藏模态框
                hideModal();
                return true;
            }
            GuiManager.openTradeMarket();
            return true;
        }
        
        // 处理Ctrl+A全选
        if (keyCode == 65 && hasControlDown()) { // Ctrl+A
            selectAllItems();
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
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * 出售模式枚举
     */
    public enum SellMode {
        LIST_ITEM,  // 上架物品
        RECYCLE     // 系统回收
    }
}