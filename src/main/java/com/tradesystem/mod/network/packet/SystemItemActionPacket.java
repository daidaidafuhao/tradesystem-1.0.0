package com.tradesystem.mod.network.packet;

import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.manager.SystemItemManager;
import com.tradesystem.mod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 系统商品操作数据包
 * 用于处理管理员对系统商品的各种操作
 */
public class SystemItemActionPacket {
    
    public enum Action {
        ADD,           // 添加系统商品
        REMOVE,        // 移除系统商品
        UPDATE_PRICE,  // 更新价格
        UPDATE_QUANTITY, // 更新数量
        TOGGLE_STATUS  // 切换状态
    }
    
    private Action action;
    private UUID itemId;
    private ItemStack itemStack;
    private int price;
    private int quantity;
    
    // 添加商品构造函数
    public SystemItemActionPacket(ItemStack itemStack, int price, int quantity) {
        this.action = Action.ADD;
        this.itemId = null;
        this.itemStack = itemStack;
        this.price = price;
        this.quantity = quantity;
    }
    
    // 移除商品构造函数
    public SystemItemActionPacket(Action action, UUID itemId) {
        this.action = action;
        this.itemId = itemId;
        this.itemStack = ItemStack.EMPTY;
        this.price = 0;
        this.quantity = 0;
    }
    
    // 更新价格构造函数
    public SystemItemActionPacket(UUID itemId, int newPrice) {
        this.action = Action.UPDATE_PRICE;
        this.itemId = itemId;
        this.itemStack = ItemStack.EMPTY;
        this.price = newPrice;
        this.quantity = 0;
    }
    
    // 更新数量构造函数
    public SystemItemActionPacket(UUID itemId, int newQuantity, boolean isQuantityUpdate) {
        this.action = Action.UPDATE_QUANTITY;
        this.itemId = itemId;
        this.itemStack = ItemStack.EMPTY;
        this.price = 0;
        this.quantity = newQuantity;
    }
    
    /**
     * 编码数据包
     */
    public static void encode(SystemItemActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        
        if (packet.itemId != null) {
            buffer.writeBoolean(true);
            buffer.writeUUID(packet.itemId);
        } else {
            buffer.writeBoolean(false);
        }
        
        buffer.writeItem(packet.itemStack);
        buffer.writeInt(packet.price);
        buffer.writeInt(packet.quantity);
    }
    
    /**
     * 解码数据包
     */
    public static SystemItemActionPacket decode(FriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        UUID itemId = buffer.readBoolean() ? buffer.readUUID() : null;
        ItemStack itemStack = buffer.readItem();
        int price = buffer.readInt();
        int quantity = buffer.readInt();
        
        // 创建一个临时对象并设置字段
        SystemItemActionPacket packet = new SystemItemActionPacket(ItemStack.EMPTY, 0, 0);
        packet.action = action;
        packet.itemId = itemId;
        packet.itemStack = itemStack;
        packet.price = price;
        packet.quantity = quantity;
        
        return packet;
    }
    
    /**
     * 处理数据包
     */
    public static void handle(SystemItemActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                handleServerSide(packet, player);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 服务端处理逻辑
     */
    private static void handleServerSide(SystemItemActionPacket packet, ServerPlayer player) {
        try {
            SystemItemManager manager = SystemItemManager.getInstance();
            
            // 检查管理员权限
            if (!manager.isAdmin(player)) {
                player.sendSystemMessage(Component.translatable("gui.tradesystem.message.no_permission"));
                return;
            }
            
            boolean success = false;
            String messageKey = "";
            
            switch (packet.action) {
                case ADD:
                    success = manager.addSystemItem(packet.itemStack, packet.price, packet.quantity, player.getName().getString());
                    messageKey = success ? "gui.tradesystem.message.system_item_added" : "gui.tradesystem.message.system_item_add_failed";
                    break;
                    
                case REMOVE:
                    success = manager.removeSystemItem(packet.itemId);
                    messageKey = success ? "gui.tradesystem.message.system_item_removed" : "gui.tradesystem.message.system_item_remove_failed";
                    break;
                    
                case UPDATE_PRICE:
                    success = manager.updateSystemItemPrice(packet.itemId, packet.price);
                    messageKey = success ? "gui.tradesystem.message.system_item_price_updated" : "gui.tradesystem.message.system_item_price_update_failed";
                    break;
                    
                case UPDATE_QUANTITY:
                    success = manager.updateSystemItemQuantity(packet.itemId, packet.quantity);
                    messageKey = success ? "gui.tradesystem.message.system_item_quantity_updated" : "gui.tradesystem.message.system_item_quantity_update_failed";
                    break;
                    
                case TOGGLE_STATUS:
                    success = manager.toggleSystemItemStatus(packet.itemId);
                    messageKey = success ? "gui.tradesystem.message.system_item_status_toggled" : "gui.tradesystem.message.system_item_status_toggle_failed";
                    break;
            }
            
            // 发送结果消息
            player.sendSystemMessage(Component.translatable(messageKey));
            
            // 如果操作成功，同步数据到所有客户端
            if (success) {
                // 同步系统商品数据到所有客户端
                SystemItemSyncPacket syncPacket = new SystemItemSyncPacket(manager.getAllSystemItems());
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), syncPacket);
                TradeMod.getLogger().info("系统商品操作成功: {} by {}，已同步到所有客户端", packet.action, player.getName().getString());
            }
            
        } catch (Exception e) {
            TradeMod.getLogger().error("处理系统商品操作时发生错误", e);
            player.sendSystemMessage(Component.translatable("gui.tradesystem.message.operation_failed"));
        }
    }
}