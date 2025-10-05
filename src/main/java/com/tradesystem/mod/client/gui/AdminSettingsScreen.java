package com.tradesystem.mod.client.gui;

import com.tradesystem.mod.client.gui.widget.ItemSlotWidget;
import com.tradesystem.mod.client.gui.widget.TradeButton;
import com.tradesystem.mod.client.ClientSystemItemManager;
import com.tradesystem.mod.data.SystemItem;
import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.packet.SystemItemActionPacket;
import com.tradesystem.mod.util.CurrencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员设置界面
 * 允许管理员管理系统商品：上架、下架、价格设置、数量管理
 */
public class AdminSettingsScreen extends BaseTradeScreen {
    
    private static final int ITEMS_PER_PAGE = 12; // 4x3 网格
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 22;
    
    // 界面组件
    private EditBox searchBox;
    private EditBox priceBox;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button backButton;
    private Button refreshButton;
    private Button addItemButton;
    private Button removeItemButton;
    private Button updatePriceButton;
    private Button toggleStatusButton;
    
    // 物品槽位
    private final List<ItemSlotWidget> itemSlots = new ArrayList<>();
    
    // 数据
    private List<ItemStack> allGameItems = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private List<SystemItem> systemItems = new ArrayList<>();
    private int currentPage = 0;
    private String currentSearch = "";
    
    // 选中的物品
    private ItemStack selectedGameItem = null;
    private SystemItem selectedSystemItem = null;
    private ItemSlotWidget selectedSlot = null;
    
    public AdminSettingsScreen() {
        super(Component.translatable("gui.tradesystem.admin.title"), 320, 240);
    }
    
    @Override
    protected void initComponents() {
        // 搜索框
        searchBox = new EditBox(this.font, leftPos + 10, topPos + 20, 160, 18,
                Component.translatable("gui.tradesystem.admin.search"));
        searchBox.setHint(Component.translatable("gui.tradesystem.admin.search_hint"));
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);
        
        // 创建物品槽位 (修复Y坐标计算错误)
        itemSlots.clear();
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = leftPos + 10 + col * 32;
            int y = topPos + 45 + row * 32;  // 修复：使用topPos而不是leftPos
            
            ItemSlotWidget slot = new ItemSlotWidget(x, y, 16, this::onItemSlotClicked);
            itemSlots.add(slot);
            addRenderableWidget(slot);
        }
        
        // 分页按钮
        prevPageButton = new TradeButton(leftPos + 10, topPos + 145, 35, 18,
                Component.translatable("gui.tradesystem.admin.prev_page"),
                button -> previousPage());
        addRenderableWidget(prevPageButton);
        
        nextPageButton = new TradeButton(leftPos + 50, topPos + 145, 35, 18,
                Component.translatable("gui.tradesystem.admin.next_page"),
                button -> nextPage());
        addRenderableWidget(nextPageButton);
        
        // 价格输入框 (居中显示)
        priceBox = new EditBox(this.font, leftPos + 130, topPos + 145, 80, 18,
                Component.translatable("gui.tradesystem.admin.price"));
        priceBox.setHint(Component.translatable("gui.tradesystem.admin.price_hint"));
        addRenderableWidget(priceBox);
        
        // 操作按钮 (重新排列以避免重叠)
        addItemButton = new TradeButton(leftPos + 10, topPos + 170, 50, 18,
                Component.translatable("gui.tradesystem.admin.add_item"),
                button -> addSystemItem());
        addRenderableWidget(addItemButton);
        
        removeItemButton = new TradeButton(leftPos + 65, topPos + 170, 50, 18,
                Component.translatable("gui.tradesystem.admin.remove_item"),
                button -> removeSystemItem());
        addRenderableWidget(removeItemButton);
        
        updatePriceButton = new TradeButton(leftPos + 120, topPos + 170, 60, 18,
                Component.translatable("gui.tradesystem.admin.update_price"),
                button -> updateItemPrice());
        addRenderableWidget(updatePriceButton);
        
        toggleStatusButton = new TradeButton(leftPos + 185, topPos + 170, 60, 18,
                Component.translatable("gui.tradesystem.admin.toggle_status"),
                button -> toggleItemStatus());
        addRenderableWidget(toggleStatusButton);
        
        // 返回按钮
        backButton = new TradeButton(leftPos + 250, topPos + 170, 50, 18,
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
        
        // 渲染价格标签 (居中显示)
        Component priceLabel = Component.translatable("gui.tradesystem.admin.price");
        guiGraphics.drawString(this.font, priceLabel, leftPos + 130, topPos + 135, TEXT_COLOR, false);
        
        // 渲染页面信息 (调整位置避免与按钮重叠)
        if (!filteredItems.isEmpty()) {
            int totalPages = (filteredItems.size() - 1) / ITEMS_PER_PAGE + 1;
            Component pageInfo = Component.literal(String.format("第 %d/%d 页", currentPage + 1, totalPages));
            guiGraphics.drawString(this.font, pageInfo, leftPos + 10, topPos + 195, TEXT_COLOR, false);
        }
        
        // 渲染选中物品信息
        renderSelectedItemInfo(guiGraphics);
        
        // 更新按钮状态
        updateButtonStates();
    }
    
    @Override
    public void tick() {
        super.tick();
        searchBox.tick();
        priceBox.tick();
        updateButtonStates();
    }
    
    /**
     * 渲染选中物品信息
     * 在界面中间区域显示当前选中物品的详细信息，包括图标、名称、价格和状态
     */
    private void renderSelectedItemInfo(GuiGraphics guiGraphics) {
        // 只有当有物品被选中时才渲染信息
        if (selectedGameItem != null) {
            // 在界面中间位置渲染选中物品的图标
            guiGraphics.renderItem(selectedGameItem, leftPos + 120, topPos + 100);
            
            // 在图标右侧显示物品名称
            Component itemName = selectedGameItem.getHoverName();
            guiGraphics.drawString(this.font, itemName, leftPos + 140, topPos + 105, TEXT_COLOR, false);
            
            // 检查该物品是否为系统商品，如果是则显示额外信息
            SystemItem systemItem = ClientSystemItemManager.getInstance().getSystemItem(selectedGameItem);
            if (systemItem != null) {
                // 显示系统商品的价格信息
                Component priceText = Component.literal("价格: " + CurrencyUtil.formatMoney(systemItem.getPrice()));
                guiGraphics.drawString(this.font, priceText, leftPos + 120, topPos + 120, TEXT_COLOR, false);
                
                // 显示系统商品的启用/禁用状态
                Component statusText = Component.literal("状态: " + (systemItem.isActive() ? "启用" : "禁用"));
                guiGraphics.drawString(this.font, statusText, leftPos + 120, topPos + 130, TEXT_COLOR, false);
            }
        }
        // 注意：如果没有选中任何物品，中间区域保持空白
    }
    
    /**
     * 搜索框内容变化事件
     */
    private void onSearchChanged(String search) {
        this.currentSearch = search.toLowerCase();
        this.currentPage = 0;
        filterItems();
        updateItemSlots();
    }
    
    /**
     * 物品槽位点击事件
     * 当用户点击某个物品槽位时，会选中该物品并在界面中间显示其详细信息
     */
    private void onItemSlotClicked(ItemSlotWidget slot) {
        // 取消之前选中槽位的高亮状态
        if (selectedSlot != null) {
            selectedSlot.setSelected(false);
        }
        
        // 设置新选中的槽位并高亮显示
        selectedSlot = slot;
        slot.setSelected(true);
        
        // 根据槽位索引计算对应的物品索引
        int slotIndex = itemSlots.indexOf(slot);
        int itemIndex = currentPage * ITEMS_PER_PAGE + slotIndex;
        
        // 检查是否点击了有效的物品槽位（不是空槽位）
        if (itemIndex < filteredItems.size()) {
            // 获取选中的物品，这个物品会在界面中间显示
            selectedGameItem = filteredItems.get(itemIndex);
            // 检查该物品是否已经是系统商品
            selectedSystemItem = ClientSystemItemManager.getInstance().getSystemItem(selectedGameItem);
            
            // 根据物品状态更新价格输入框
            if (selectedSystemItem != null) {
                // 如果是系统商品，显示当前价格
                priceBox.setValue(String.valueOf(selectedSystemItem.getPrice()));
            } else {
                // 如果不是系统商品，清空价格框
                priceBox.setValue("");
            }
        } else {
            // 点击了空槽位，清空选择
            selectedGameItem = null;
            selectedSystemItem = null;
            priceBox.setValue("");
        }
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        // 加载所有游戏物品
        allGameItems.clear();
        
        System.out.println("=== refreshData 开始加载物品 ===");
        
        // 只加载原版Minecraft物品
        ForgeRegistries.ITEMS.getValues().forEach(item -> {
            if (item != Items.AIR) {
                // 获取物品的注册名称
                String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
                
                // 只添加原版minecraft物品
                if (itemId.startsWith("minecraft:")) {
                    ItemStack itemStack = new ItemStack(item, 1);
                    allGameItems.add(itemStack);
                    System.out.println("加载原版物品: " + itemId + " -> " + itemStack.getHoverName().getString());
                } else {
                    System.out.println("跳过模组物品: " + itemId);
                }
            }
        });
        
        System.out.println("总共加载了 " + allGameItems.size() + " 个原版物品");
        
        // 加载系统商品
        systemItems = ClientSystemItemManager.getInstance().getAllSystemItems();
        
        // 过滤物品
        filterItems();
        updateItemSlots();
        
        System.out.println("=== refreshData 完成 ===");
    }
    
    /**
     * 过滤物品
     */
    private void filterItems() {
        filteredItems.clear();
        
        if (currentSearch.isEmpty()) {
            filteredItems.addAll(allGameItems);
        } else {
            for (ItemStack item : allGameItems) {
                String itemName = item.getHoverName().getString().toLowerCase();
                if (itemName.contains(currentSearch)) {
                    filteredItems.add(item);
                }
            }
        }
    }
    
    /**
     * 更新物品槽位显示
     * 根据当前页面和过滤后的物品列表，更新每个槽位的显示内容
     */
    private void updateItemSlots() {
        System.out.println("=== updateItemSlots 调试信息 ===");
        System.out.println("filteredItems.size(): " + filteredItems.size());
        System.out.println("currentPage: " + currentPage);
        System.out.println("ITEMS_PER_PAGE: " + ITEMS_PER_PAGE);
        
        for (int i = 0; i < itemSlots.size(); i++) {
            ItemSlotWidget slot = itemSlots.get(i);
            // 计算当前槽位对应的物品索引
            int itemIndex = currentPage * ITEMS_PER_PAGE + i;
            
            System.out.println("槽位 " + i + ", 物品索引: " + itemIndex);
            
            // 检查是否有对应的物品
            if (itemIndex < filteredItems.size()) {
                // 有物品：设置物品并显示槽位
                ItemStack item = filteredItems.get(itemIndex);
                System.out.println("  设置物品: " + item.getHoverName().getString());
                slot.setItemStack(item);
                slot.setVisible(true);  // 显示有物品的槽位
                slot.setEnabled(true);  // 启用点击功能
                
                // 检查是否是系统商品
                SystemItem systemItem = ClientSystemItemManager.getInstance().getSystemItem(item);
                if (systemItem != null) {
                    slot.setSystemItem(systemItem);
                } else {
                    // 清空系统物品信息，但保留ItemStack
                    slot.setSystemItem(null);
                }
            } else {
                // 没有物品：清空槽位内容但保持槽位可见
                System.out.println("  空槽位");
                slot.setItemStack(ItemStack.EMPTY);
                slot.setVisible(true);  // 空槽位也要显示（显示为空的灰色框）
                slot.setEnabled(false); // 禁用空槽位的点击功能
                // 只清空系统物品信息，不清空ItemStack
                slot.setSystemItem(null);
            }
        }
        System.out.println("=== updateItemSlots 结束 ===");
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
        int maxPage = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_PAGE);
        if (currentPage < maxPage) {
            currentPage++;
            updateItemSlots();
        }
    }
    
    /**
     * 添加系统商品
     */
    private void addSystemItem() {
        if (selectedGameItem == null) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.no_item_selected"));
            return;
        }
        
        try {
            int price = Integer.parseInt(priceBox.getValue());
            // 系统商品默认为无限数量，设置为1作为显示值
            int quantity = 1;
            
            if (price <= 0) {
                showMessage(Component.translatable("gui.tradesystem.admin.error.invalid_price"));
                return;
            }
            
            // 发送添加系统商品的网络包
            NetworkHandler.sendToServer(new SystemItemActionPacket(selectedGameItem, price, quantity));
            
            showMessage(Component.translatable("gui.tradesystem.admin.success.item_added"));
            
        } catch (NumberFormatException e) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.invalid_number"));
        }
    }
    
    /**
     * 移除系统商品
     */
    private void removeSystemItem() {
        if (selectedGameItem == null) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.no_item_selected"));
            return;
        }
        
        if (selectedSystemItem == null) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.not_system_item"));
            return;
        }
        
        // 发送移除系统商品的网络包
        NetworkHandler.sendToServer(new SystemItemActionPacket(
                SystemItemActionPacket.Action.REMOVE, selectedSystemItem.getId()));
        
        showMessage(Component.translatable("gui.tradesystem.admin.success.item_removed"));
    }
    
    /**
     * 更新物品价格
     */
    private void updateItemPrice() {
        if (selectedGameItem == null) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.no_item_selected"));
            return;
        }
        
        if (selectedSystemItem == null) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.not_system_item"));
            return;
        }
        
        try {
            int price = Integer.parseInt(priceBox.getValue());
            
            if (price <= 0) {
                showMessage(Component.translatable("gui.tradesystem.admin.error.invalid_price"));
                return;
            }
            
            // 发送更新价格的网络包
            NetworkHandler.sendToServer(new SystemItemActionPacket(selectedSystemItem.getId(), price));
            
            showMessage(Component.translatable("gui.tradesystem.admin.success.price_updated"));
            
        } catch (NumberFormatException e) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.invalid_number"));
        }
    }
    
    /**
     * 切换物品状态（上架/下架）
     */
    private void toggleItemStatus() {
        if (selectedGameItem == null) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.no_item_selected"));
            return;
        }
        
        if (selectedSystemItem == null) {
            showMessage(Component.translatable("gui.tradesystem.admin.error.not_system_item"));
            return;
        }
        
        // 发送切换状态的网络包
        NetworkHandler.sendToServer(new SystemItemActionPacket(
                SystemItemActionPacket.Action.TOGGLE_STATUS, selectedSystemItem.getId()));
        
        String statusKey = selectedSystemItem.isActive() ? 
                "gui.tradesystem.admin.success.item_disabled" :
                "gui.tradesystem.admin.success.item_enabled";
        showMessage(Component.translatable(statusKey));
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = selectedGameItem != null;
        boolean isSystemItem = selectedSystemItem != null;
        
        addItemButton.active = hasSelection && !isSystemItem;
        removeItemButton.active = hasSelection && isSystemItem;
        updatePriceButton.active = hasSelection && isSystemItem;
        toggleStatusButton.active = hasSelection && isSystemItem;
        
        int maxPage = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_PAGE);
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = currentPage < maxPage;
    }
    
    /**
     * 显示消息
     */
    private void showMessage(Component message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(message);
        }
    }
}