# 交易系统MOD设计文档

## 项目概述

本MOD旨在为Minecraft 1.20.1 Forge环境创建一个完整的交易系统，包含商品上架、定向交易、货币系统等功能。

## 核心功能设计

### 1. UI界面系统

#### 1.1 主交易界面
- **打开方式**：
  - 快捷键：默认为 `T` 键（可配置）
  - 命令：`/trade` 或 `/tradeshop`
- **界面布局**：
  - 左侧：商品浏览区域（分页显示）
  - 右侧：玩家背包和货币显示
  - 顶部：搜索栏和分类筛选
  - 底部：操作按钮区域

#### 1.2 商品上架界面
- **功能**：玩家可以将背包中的物品上架销售
- **设置项**：
  - 商品数量
  - 单价设定
  - 上架时长（可选）
  - 商品描述（可选）

#### 1.3 定向交易界面
- **功能**：向特定玩家发送商品
- **设置项**：
  - 目标玩家选择/输入
  - 商品选择
  - 接收条件（金额要求）
  - 交易备注

### 2. 货币系统

#### 2.1 货币类型
- **主货币**：金币（Coins）
- **获取方式**：
  - 出售商品
  - 系统回收物品
  - 管理员发放
  - 完成任务（扩展功能）

#### 2.2 货币管理
- **存储**：玩家数据文件中保存
- **显示**：UI界面实时显示当前余额
- **转账**：支持玩家间转账功能

### 3. 商品系统

#### 3.1 商品上架
- **上架流程**：
  1. 选择背包中的物品
  2. 设定价格和数量
  3. 确认上架
  4. 物品从背包移除，进入交易系统

#### 3.2 商品浏览
- **分类显示**：按物品类型分类
- **搜索功能**：支持物品名称搜索
- **排序功能**：按价格、时间等排序

#### 3.3 商品购买
- **购买流程**：
  1. 选择商品
  2. 确认购买数量
  3. 扣除货币
  4. 物品进入背包

### 4. 定向交易系统

#### 4.1 发送商品
- **发送流程**：
  1. 选择目标玩家
  2. 选择要发送的物品
  3. 设置接收条件（金额）
  4. 添加交易备注
  5. 发送交易请求

#### 4.2 接收商品
- **接收流程**：
  1. 查看待接收的交易
  2. 确认交易条件
  3. 支付所需金额
  4. 接收物品

#### 4.3 交易状态
- **待接收**：已发送，等待对方接收
- **已完成**：交易成功完成
- **已过期**：超时未接收（可配置过期时间）
- **已取消**：发送方主动取消

### 5. 系统回收功能

#### 5.1 回收设置
- **管理员权限**：只有管理员可以设置回收价格
- **回收列表**：可配置哪些物品可以被系统回收
- **价格设定**：为每种物品设定回收价格

#### 5.2 回收流程
- **快速回收**：在主界面直接回收背包物品
- **批量回收**：选择多个物品一次性回收
- **确认机制**：回收前显示总价值，需要确认

## 兼容性设计

### 1. 物品兼容性

#### 1.1 其他MOD物品支持
- **完全兼容**：交易系统支持所有MOD添加的物品
- **实现原理**：使用Minecraft原生的ItemStack系统，自动识别所有已注册的物品
- **NBT数据保持**：完整保存物品的NBT数据，包括MOD特有的属性
- **物品验证**：交易前验证物品是否仍然存在于游戏中（防止MOD卸载导致的问题）

#### 1.2 附魔物品支持
- **附魔完全保持**：支持所有原版和MOD附魔的交易
- **附魔显示**：在交易界面中完整显示物品的附魔信息
- **附魔验证**：确保附魔数据的完整性和有效性
- **特殊附魔**：支持诅咒、绑定等特殊附魔的物品交易

#### 1.3 特殊物品处理
- **容器物品**：支持潜影盒、背包等容器物品（保持内部物品）
- **工具耐久**：完整保存工具的耐久度信息
- **自定义名称**：保持物品的自定义名称和描述
- **皮肤物品**：支持马铠、旗帜等带有图案的物品

#### 1.4 限制物品配置
- **黑名单系统**：管理员可配置禁止交易的物品
- **创造模式物品**：可配置是否允许创造模式专有物品交易
- **危险物品**：可配置对TNT、岩浆等危险物品的交易限制

### 2. MOD兼容性测试

#### 2.1 常见MOD兼容性
- **JEI (Just Enough Items)**：完全兼容，支持在JEI中查看交易物品
- **Tinkers' Construct**：支持工匠构造的工具和武器交易
- **Applied Energistics 2**：支持AE2的存储设备和组件交易
- **Thermal Expansion**：支持热力膨胀的机器和物品交易
- **Botania**：支持植物魔法的花朵和魔法物品交易

#### 2.2 兼容性保障机制
- **动态物品检测**：运行时检测可用的物品和MOD
- **错误处理**：优雅处理MOD物品缺失的情况
- **数据迁移**：支持MOD更新后的物品数据迁移

## 功能实现原理

### 1. UI界面系统实现原理

#### 1.1 GUI框架设计
- **基础架构**：继承Minecraft的Screen类（纯客户端UI，无需Container同步）
- **渲染系统**：使用Minecraft原生的GuiGraphics渲染API
- **事件处理**：重写mouseClicked、keyPressed、mouseScrolled等方法
- **布局管理**：采用相对定位系统，支持不同分辨率自适应

#### 1.2 界面状态管理
```java
public class TradeScreen extends Screen {
    private TradeScreenState currentState;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final Minecraft minecraft;
    
    public TradeScreen() {
        super(Component.translatable("gui.tradingsystem.title"));
        this.minecraft = Minecraft.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        // 初始化UI组件
        initializeWidgets();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染自定义内容
    }
}
```

#### 1.3 数据绑定机制
- **双向绑定**：界面数据与后端数据实时同步
- **观察者模式**：数据变化时自动更新UI显示
- **缓存机制**：减少网络请求，提高响应速度

### 2. 货币系统实现原理

#### 2.1 货币存储机制
```java
public class CurrencyManager {
    // 使用Forge 1.20.1的正确Capability系统
    public static final Capability<ICurrencyHandler> CURRENCY_CAPABILITY = 
        Capability.create(ICurrencyHandler.class, () -> new DefaultCurrencyHandler());
    
    // 在ModCapabilities类中注册
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(CURRENCY_CAPABILITY, Player.class);
    }
    
    public static void addCoins(Player player, int amount) {
        player.getCapability(CURRENCY_CAPABILITY).ifPresent(handler -> {
            handler.addCoins(amount);
            // 同步到客户端
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendToPlayer(new SyncCurrencyPacket(handler.getCoins()), serverPlayer);
            }
        });
    }
    
    // 玩家数据附加 - 使用正确的Provider实现
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(TradingSystemMod.MODID, "currency"), 
                new CurrencyCapabilityProvider());
        }
    }
    
    // Capability Provider实现
    private static class CurrencyCapabilityProvider implements ICapabilityProvider {
        private final LazyOptional<ICurrencyHandler> instance = LazyOptional.of(DefaultCurrencyHandler::new);
        
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return CURRENCY_CAPABILITY.orEmpty(cap, instance);
        }
        
        public void invalidate() {
            instance.invalidate();
        }
    }
}
```

#### 2.2 货币同步机制
- **服务端权威**：所有货币操作在服务端验证和执行
- **客户端同步**：使用自定义数据包同步货币数据到客户端
- **持久化存储**：使用NBT数据保存到玩家数据文件

#### 2.3 交易验证系统
- **余额检查**：交易前验证玩家货币余额
- **原子操作**：确保货币转移的原子性，防止重复扣费
- **事务回滚**：交易失败时自动回滚货币状态

### 3. 商品系统实现原理

#### 3.1 商品数据结构
```java
public class TradeItem {
    private UUID id;
    private UUID sellerId;
    private ItemStack itemStack;
    private int price;
    private int quantity;
    private long timestamp;
    private String description;
    
    // NBT序列化方法
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", this.id);
        tag.putUUID("seller", this.sellerId);
        tag.put("item", this.itemStack.serializeNBT());
        tag.putInt("price", this.price);
        // ... 其他字段
        return tag;
    }
}
```

#### 3.2 商品索引系统
- **分类索引**：按物品类型建立索引，支持快速分类查询
- **价格索引**：按价格排序，支持价格区间查询
- **时间索引**：按上架时间排序，支持最新商品查询
- **搜索引擎**：基于物品名称和描述的全文搜索

#### 3.3 商品生命周期管理
```java
public class ItemLifecycleManager {
    public void listItem(Player seller, ItemStack item, int price) {
        // 1. 验证物品有效性
        if (!isValidItem(item)) return;
        
        // 2. 从玩家背包移除物品
        removeFromInventory(seller, item);
        
        // 3. 创建交易商品
        TradeItem tradeItem = new TradeItem(seller.getUUID(), item, price);
        
        // 4. 添加到交易系统
        TradeRegistry.getInstance().addItem(tradeItem);
        
        // 5. 同步到所有客户端
        syncToAllClients(tradeItem);
    }
}
```

### 4. 定向交易系统实现原理

#### 4.1 交易请求机制
```java
public class DirectTradeManager {
    private Map<UUID, List<TradeRequest>> pendingTrades;
    
    public void sendTradeRequest(UUID from, UUID to, ItemStack item, int requiredPayment) {
        TradeRequest request = new TradeRequest(from, to, item, requiredPayment);
        
        // 添加到待处理列表
        pendingTrades.computeIfAbsent(to, k -> new ArrayList<>()).add(request);
        
        // 通知目标玩家
        notifyPlayer(to, request);
        
        // 设置过期时间
        scheduleExpiration(request);
    }
}
```

#### 4.2 交易状态机
- **PENDING**：等待接收方响应
- **ACCEPTED**：接收方同意，等待支付
- **COMPLETED**：交易完成
- **EXPIRED**：超时过期
- **CANCELLED**：发送方取消

#### 4.3 异步处理机制
- **消息队列**：使用队列处理交易请求，避免阻塞
- **定时任务**：定期清理过期交易
- **事件驱动**：基于事件的交易状态更新

### 5. 系统回收功能实现原理

#### 5.1 回收价格管理
```java
public class RecycleManager {
    private Map<Item, Integer> recyclePrices;
    
    public void loadRecyclePrices() {
        // 从配置文件加载回收价格
        JsonObject config = loadConfig("recycle_prices.json");
        for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.getKey()));
            if (item != null) {
                recyclePrices.put(item, entry.getValue().getAsInt());
            }
        }
    }
    
    public int getRecyclePrice(ItemStack itemStack) {
        return recyclePrices.getOrDefault(itemStack.getItem(), 0) * itemStack.getCount();
    }
}
```

#### 5.2 批量回收算法
- **物品分组**：按类型分组计算总价值
- **价格计算**：考虑物品数量、耐久度、附魔等因素
- **确认机制**：显示详细的回收清单供玩家确认

### 6. 网络通信实现原理

#### 6.1 数据包设计
```java
// 网络处理器注册
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TradingSystemMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, C2SListItemPacket.class, 
            C2SListItemPacket::encode, C2SListItemPacket::decode, C2SListItemPacket::handle);
        INSTANCE.registerMessage(id++, S2CUpdateTradeListPacket.class,
            S2CUpdateTradeListPacket::encode, S2CUpdateTradeListPacket::decode, S2CUpdateTradeListPacket::handle);
    }
}

// 客户端到服务端数据包
public class C2SListItemPacket {
    private final int slotIndex;
    private final int price;
    private final int quantity;
    
    public C2SListItemPacket(int slotIndex, int price, int quantity) {
        this.slotIndex = slotIndex;
        this.price = price;
        this.quantity = quantity;
    }
    
    public static void encode(C2SListItemPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.slotIndex);
        buf.writeInt(packet.price);
        buf.writeInt(packet.quantity);
    }
    
    public static C2SListItemPacket decode(FriendlyByteBuf buf) {
        return new C2SListItemPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }
    
    public static void handle(C2SListItemPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack item = player.getInventory().getItem(packet.slotIndex);
                TradeManager.listItem(player, item, packet.price, packet.quantity);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

// 服务端到客户端数据包（优化的分页传输）
public class S2CUpdateTradeListPacket {
    private static final int MAX_PACKET_SIZE = 30720; // 30KB安全限制
    private static final int MAX_ITEMS_PER_PACKET = 25; // 限制每包物品数量
    
    private final List<TradeItem> items;
    private final int pageIndex;
    private final int totalPages;
    private final boolean isFragmented;
    private final int fragmentIndex;
    private final int totalFragments;
    
    public S2CUpdateTradeListPacket(List<TradeItem> items, int pageIndex, int totalPages) {
        this(items, pageIndex, totalPages, false, 0, 1);
    }
    
    public S2CUpdateTradeListPacket(List<TradeItem> items, int pageIndex, int totalPages,
                                   boolean isFragmented, int fragmentIndex, int totalFragments) {
        this.items = items;
        this.pageIndex = pageIndex;
        this.totalPages = totalPages;
        this.isFragmented = isFragmented;
        this.fragmentIndex = fragmentIndex;
        this.totalFragments = totalFragments;
    }
    
    // 安全的编码方法，确保不超过大小限制
    public static void encode(S2CUpdateTradeListPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.pageIndex);
        buf.writeInt(packet.totalPages);
        buf.writeBoolean(packet.isFragmented);
        
        if (packet.isFragmented) {
            buf.writeInt(packet.fragmentIndex);
            buf.writeInt(packet.totalFragments);
        }
        
        // 限制物品数量并检查大小
        int itemsToWrite = Math.min(packet.items.size(), MAX_ITEMS_PER_PACKET);
        buf.writeInt(itemsToWrite);
        
        int writtenItems = 0;
        for (TradeItem item : packet.items) {
            if (writtenItems >= itemsToWrite) break;
            
            // 预估大小检查（每个物品约1KB）
            if (buf.writerIndex() + 1024 > MAX_PACKET_SIZE) {
                break;
            }
            
            item.encode(buf);
            writtenItems++;
        }
        
        // 更新实际写入的物品数量
        int writerIndex = buf.writerIndex();
        buf.writerIndex(buf.writerIndex() - (itemsToWrite * 1024)); // 回到数量位置
        buf.writeInt(writtenItems);
        buf.writerIndex(writerIndex);
    }
    
    // 创建分片数据包的工厂方法
    public static List<S2CUpdateTradeListPacket> createSafePackets(List<TradeItem> allItems, 
                                                                   int pageIndex, int totalPages) {
        List<S2CUpdateTradeListPacket> packets = new ArrayList<>();
        
        if (allItems.size() <= MAX_ITEMS_PER_PACKET) {
            packets.add(new S2CUpdateTradeListPacket(allItems, pageIndex, totalPages));
            return packets;
        }
        
        // 分片处理
        int totalFragments = (int) Math.ceil((double) allItems.size() / MAX_ITEMS_PER_PACKET);
        for (int i = 0; i < totalFragments; i++) {
            int start = i * MAX_ITEMS_PER_PACKET;
            int end = Math.min(start + MAX_ITEMS_PER_PACKET, allItems.size());
            List<TradeItem> fragment = allItems.subList(start, end);
            
            packets.add(new S2CUpdateTradeListPacket(fragment, pageIndex, totalPages, 
                                                     true, i, totalFragments));
        }
        
        return packets;
    }
}
```

#### 6.2 数据同步策略
- **增量同步**：只同步变化的数据，减少网络开销
- **压缩传输**：对大量数据进行压缩传输
- **缓存机制**：客户端缓存常用数据，减少请求频率

### 7. 数据持久化实现原理

#### 7.1 存储架构
```java
public class TradeDataManager extends SavedData {
    private static final String DATA_NAME = "trading_system_data";
    // 使用普通HashMap，确保在主线程中访问
    private final Map<UUID, PlayerTradeData> playerTrades = new HashMap<>();
    private final Map<String, MarketItem> globalMarket = new HashMap<>();
    private final TradeIndexManager indexManager = new TradeIndexManager();
    
    // 分片存储管理 - 使用普通HashMap
    private final Map<String, TradeDataShard> shards = new HashMap<>();
    
    // 线程安全访问方法
    public void executeOnMainThread(Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(task);
        }
    }
    
    public static TradeDataManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            TradeDataManager::load,
            TradeDataManager::new,
            DATA_NAME
        );
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        // 保存索引信息
        tag.put("index", indexManager.serialize());
        
        // 保存分片引用
        ListTag shardList = new ListTag();
        for (String shardId : shards.keySet()) {
            CompoundTag shardTag = new CompoundTag();
            shardTag.putString("id", shardId);
            shardList.add(shardTag);
        }
        tag.put("shards", shardList);
        
        // 异步保存分片数据
        saveShards();
        return tag;
    }
    
    public static TradeDataManager load(CompoundTag tag) {
        TradeDataManager manager = new TradeDataManager();
        
        // 加载索引
        if (tag.contains("index")) {
            manager.indexManager.deserialize(tag.getCompound("index"));
        }
        
        // 延迟加载分片（按需加载）
        if (tag.contains("shards")) {
            ListTag shardList = tag.getList("shards", Tag.TAG_COMPOUND);
            for (Tag shardTag : shardList) {
                CompoundTag compound = (CompoundTag) shardTag;
                String shardId = compound.getString("id");
                manager.registerShard(shardId);
            }
        }
        
        return manager;
    }
    
    // 按需加载分片数据
    private TradeDataShard loadShard(String shardId) {
        return shards.computeIfAbsent(shardId, id -> {
            // 从文件系统加载分片数据
            return TradeDataShard.loadFromFile(id);
        });
    }
    
    // 高效查询接口
    public List<MarketItem> searchItems(ItemSearchCriteria criteria) {
        // 使用索引快速定位相关分片
        Set<String> relevantShards = indexManager.findRelevantShards(criteria);
        
        return relevantShards.parallelStream()
            .map(this::loadShard)
            .flatMap(shard -> shard.searchItems(criteria).stream())
            .sorted(criteria.getComparator())
            .limit(criteria.getLimit())
            .collect(Collectors.toList());
    }
}

// 分片数据结构 - 线程安全优化
public class TradeDataShard {
    private final String shardId;
    // 使用普通HashMap配合同步块
    private final Map<String, MarketItem> items = new HashMap<>();
    private volatile boolean dirty = false;
    private final Object lock = new Object();
    
    public void addItem(MarketItem item) {
        synchronized (lock) {
            items.put(item.getId(), item);
            dirty = true;
        }
        // 在主线程中触发保存
        scheduleMainThreadSave();
    }
    
    public MarketItem getItem(String itemId) {
        synchronized (lock) {
            return items.get(itemId);
        }
    }
    
    public List<MarketItem> getAllItems() {
        synchronized (lock) {
            return new ArrayList<>(items.values());
        }
    }
    
    private void scheduleMainThreadSave() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(this::saveToFile);
        }
    }
    
    public void saveToFile() {
        if (!dirty) return;
        
        try {
            // 序列化到文件
            Path shardFile = getShardFilePath(shardId);
            CompoundTag data = new CompoundTag();
            
            ListTag itemList = new ListTag();
            for (MarketItem item : items.values()) {
                itemList.add(item.serialize());
            }
            data.put("items", itemList);
            
            // 原子写入
            NbtIo.writeCompressed(data, shardFile.toFile());
            dirty = false;
        } catch (IOException e) {
            TradingSystemMod.LOGGER.error("Failed to save shard: " + shardId, e);
        }
    }
}

// 索引管理器 - 线程安全优化
public class TradeIndexManager {
    // 使用普通HashMap配合同步访问
    private final Map<String, Set<String>> itemTypeIndex = new HashMap<>();
    private final TreeMap<Integer, Set<String>> priceRangeIndex = new TreeMap<>();
    private final Map<UUID, Set<String>> sellerIndex = new HashMap<>();
    private final Object indexLock = new Object();
    
    // 线程安全的索引更新方法
    public void updateIndex(MarketItem item, String shardId) {
        synchronized (indexLock) {
            // 更新物品类型索引
            String itemType = item.getItemType();
            itemTypeIndex.computeIfAbsent(itemType, k -> new HashSet<>()).add(shardId);
            
            // 更新价格范围索引
            int priceRange = item.getPrice() / 100; // 按100为单位分组
            priceRangeIndex.computeIfAbsent(priceRange, k -> new HashSet<>()).add(shardId);
            
            // 更新卖家索引
            sellerIndex.computeIfAbsent(item.getSellerId(), k -> new HashSet<>()).add(shardId);
        }
    }
    
    public Set<String> getShardsByItemType(String itemType) {
        synchronized (indexLock) {
            return new HashSet<>(itemTypeIndex.getOrDefault(itemType, Collections.emptySet()));
        }
    }
    
    public void addItemToIndex(MarketItem item, String shardId) {
        // 按物品类型索引
        String itemType = item.getItemStack().getItem().toString();
        itemTypeIndex.computeIfAbsent(itemType, k -> ConcurrentHashMap.newKeySet()).add(shardId);
        
        // 按价格范围索引
        int priceRange = item.getPrice() / 100 * 100; // 按100为单位分组
        priceRangeIndex.computeIfAbsent(priceRange, k -> ConcurrentHashMap.newKeySet()).add(shardId);
        
        // 按卖家索引
        sellerIndex.computeIfAbsent(item.getSellerId(), k -> ConcurrentHashMap.newKeySet()).add(shardId);
    }
    
    public Set<String> findRelevantShards(ItemSearchCriteria criteria) {
        Set<String> result = new HashSet<>();
        
        // 根据搜索条件组合索引结果
        if (criteria.hasItemType()) {
            Set<String> typeShards = itemTypeIndex.get(criteria.getItemType());
            if (typeShards != null) result.addAll(typeShards);
        }
        
        if (criteria.hasPriceRange()) {
            int minPrice = criteria.getMinPrice() / 100 * 100;
            int maxPrice = criteria.getMaxPrice() / 100 * 100;
            
            priceRangeIndex.subMap(minPrice, true, maxPrice, true)
                .values().forEach(result::addAll);
        }
        
        return result.isEmpty() ? getAllShards() : result;
    }
}
```

#### 7.2 数据备份机制
```java
public class BackupManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Path backupDir;
    private final TradeDataManager dataManager;
    
    public BackupManager(Path backupDir, TradeDataManager dataManager) {
        this.backupDir = backupDir;
        this.dataManager = dataManager;
        
        // 每小时执行增量备份
        scheduler.scheduleAtFixedRate(this::performIncrementalBackup, 1, 1, TimeUnit.HOURS);
        
        // 每天执行完整备份
        scheduler.scheduleAtFixedRate(this::performFullBackup, 24, 24, TimeUnit.HOURS);
    }
    
    private void performIncrementalBackup() {
        try {
            long timestamp = System.currentTimeMillis();
            Path backupFile = backupDir.resolve("incremental_" + timestamp + ".backup");
            
            // 获取自上次备份以来的变更数据
            List<DataChange> changes = dataManager.getChangesSince(lastBackupTime);
            
            CompoundTag backupData = new CompoundTag();
            backupData.putLong("timestamp", timestamp);
            backupData.putString("type", "incremental");
            
            ListTag changeList = new ListTag();
            for (DataChange change : changes) {
                changeList.add(change.serialize());
            }
            backupData.put("changes", changeList);
            
            // 计算校验和
            String checksum = calculateChecksum(backupData);
            backupData.putString("checksum", checksum);
            
            // 写入备份文件
            NbtIo.writeCompressed(backupData, backupFile.toFile());
            
            lastBackupTime = timestamp;
            LOGGER.info("Incremental backup completed: {}", backupFile);
            
        } catch (Exception e) {
            LOGGER.error("Failed to perform incremental backup", e);
        }
    }
    
    private void performFullBackup() {
        try {
            long timestamp = System.currentTimeMillis();
            Path backupFile = backupDir.resolve("full_" + timestamp + ".backup");
            
            // 获取完整数据快照
            CompoundTag fullData = dataManager.createSnapshot();
            fullData.putLong("timestamp", timestamp);
            fullData.putString("type", "full");
            
            // 计算校验和
            String checksum = calculateChecksum(fullData);
            fullData.putString("checksum", checksum);
            
            // 压缩写入
            NbtIo.writeCompressed(fullData, backupFile.toFile());
            
            // 清理旧备份
            cleanupOldBackups();
            
            LOGGER.info("Full backup completed: {}", backupFile);
            
        } catch (Exception e) {
            LOGGER.error("Failed to perform full backup", e);
        }
    }
    
    public boolean restoreFromBackup(Path backupFile) {
        try {
            CompoundTag backupData = NbtIo.readCompressed(backupFile.toFile());
            
            // 验证校验和
            String storedChecksum = backupData.getString("checksum");
            backupData.remove("checksum");
            String calculatedChecksum = calculateChecksum(backupData);
            
            if (!storedChecksum.equals(calculatedChecksum)) {
                LOGGER.error("Backup file corrupted: checksum mismatch");
                return false;
            }
            
            // 根据备份类型执行恢复
            String backupType = backupData.getString("type");
            if ("full".equals(backupType)) {
                dataManager.restoreFromSnapshot(backupData);
            } else if ("incremental".equals(backupType)) {
                dataManager.applyChanges(backupData.getList("changes", Tag.TAG_COMPOUND));
            }
            
            LOGGER.info("Successfully restored from backup: {}", backupFile);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to restore from backup", e);
            return false;
        }
    }
    
    private String calculateChecksum(CompoundTag data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(data, baos);
            byte[] hash = md.digest(baos.toByteArray());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }
}
```

### 8. 权限系统实现原理

#### 8.1 权限管理架构
```java
public class PermissionManager {
    private final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();
    private final Map<String, Integer> permissionLevels = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;
    
    public enum TradePermission {
        BASIC_TRADE(0, "trade.basic", "基础交易权限"),
        PREMIUM_TRADE(1, "trade.premium", "高级交易权限"),
        BULK_TRADE(2, "trade.bulk", "批量交易权限"),
        SYSTEM_RECYCLE(3, "system.recycle", "系统回收权限"),
        ADMIN_MANAGE(4, "admin.manage", "管理员权限"),
        ADMIN_RECYCLE_PRICE(4, "admin.recycle.price", "回收价格管理权限"),
        BYPASS_LIMITS(4, "admin.bypass.limits", "绕过限制权限");
        
        private final int level;
        private final String node;
        private final String description;
        
        TradePermission(int level, String node, String description) {
            this.level = level;
            this.node = node;
            this.description = description;
        }
        
        public boolean canAccess(int userLevel) {
            return userLevel >= this.level;
        }
        
        public String getNode() {
            return node;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    public PermissionManager() {
        // 初始化默认权限等级
        for (TradePermission perm : TradePermission.values()) {
            permissionLevels.put(perm.getNode(), perm.getLevel());
        }
    }
    
    public boolean hasPermission(Player player, String permission) {
        // 检查OP权限
        if (player.hasPermissions(4)) {
            return true;
        }
        
        // 检查玩家特定权限
        Set<String> playerPerms = playerPermissions.get(player.getUUID());
        if (playerPerms != null && playerPerms.contains(permission)) {
            return true;
        }
        
        // 检查权限等级
        int requiredLevel = permissionLevels.getOrDefault(permission, Integer.MAX_VALUE);
        return getPlayerPermissionLevel(player) >= requiredLevel;
    }
    
    public boolean hasPermission(Player player, TradePermission permission) {
        return hasPermission(player, permission.getNode());
    }
    
    public void grantPermission(UUID playerId, String permission) {
        playerPermissions.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
            .add(permission);
        setDirty();
    }
    
    public void revokePermission(UUID playerId, String permission) {
        Set<String> perms = playerPermissions.get(playerId);
        if (perms != null) {
            perms.remove(permission);
            if (perms.isEmpty()) {
                playerPermissions.remove(playerId);
            }
        }
        setDirty();
    }
    
    private int getPlayerPermissionLevel(Player player) {
        // 使用Minecraft原生权限系统
        if (player instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                // 检查OP权限等级
                int opLevel = server.getProfilePermissions(serverPlayer.getGameProfile());
                if (opLevel >= 4) return 4; // 超级管理员
                if (opLevel >= 2) return 2; // 管理员
                if (opLevel >= 1) return 1; // 助理管理员
            }
        }
        return 0; // 普通玩家
    }
    
    // 简化的权限检查方法
    public static boolean hasTradePermission(Player player, String permission) {
        int level = getInstance().getPlayerPermissionLevel(player);
        
        // 基于权限等级的简单检查
        switch (permission) {
            case "trade.basic" -> { return true; } // 所有玩家都可以基础交易
            case "trade.admin" -> { return level >= 2; } // 需要管理员权限
            case "trade.system" -> { return level >= 4; } // 需要超级管理员权限
            default -> { return level >= 1; } // 其他权限需要至少助理管理员
        }
    }
    
    private void setDirty() {
        this.dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void markClean() {
        this.dirty = false;
    }
}
```

#### 8.2 权限检查机制
- **分层权限**：普通玩家、VIP玩家、管理员等不同权限级别
- **功能权限**：细分到具体功能的权限控制
- **动态权限**：支持运行时权限修改和刷新

#### 8.3 权限配置系统
```json
{
  "default_permissions": ["trade.buy", "trade.sell"],
  "vip_permissions": ["trade.buy", "trade.sell", "trade.bulk"],
  "admin_permissions": ["*"],
  "permission_groups": {
    "trader": ["trade.buy", "trade.sell"],
    "merchant": ["trade.buy", "trade.sell", "trade.bulk", "trade.priority"]
  }
}
```

### 9. 安全机制实现原理

#### 9.1 输入验证与安全机制
```java
public class SecurityValidator {
    private static final int MAX_ITEM_NAME_LENGTH = 64;
    private static final int MAX_DESCRIPTION_LENGTH = 256;
    private static final int MAX_PRICE = 1000000;
    private static final int MAX_QUANTITY = 64;
    
    // 防止恶意输入的正则表达式
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\s\\u4e00-\\u9fa5]+$");
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(".*[<>\"'&;]+.*");
    
    public static ValidationResult validateTradeRequest(TradeRequest request, Player player) {
        // 验证玩家权限
        if (!PermissionManager.getInstance().hasPermission(player, TradePermission.BASIC_TRADE)) {
            return ValidationResult.error("insufficient_permissions");
        }
        
        // 验证物品数量
        if (request.getQuantity() <= 0 || request.getQuantity() > MAX_QUANTITY) {
            return ValidationResult.error("invalid_quantity");
        }
        
        // 验证价格范围
        if (request.getPrice() < 0 || request.getPrice() > MAX_PRICE) {
            return ValidationResult.error("invalid_price");
        }
        
        // 验证物品合法性
        ItemStack itemStack = request.getItemStack();
        if (itemStack.isEmpty() || isBlacklistedItem(itemStack)) {
            return ValidationResult.error("invalid_item");
        }
        
        // 验证玩家库存
        if (!hasEnoughItems(player, itemStack, request.getQuantity())) {
            return ValidationResult.error("insufficient_items");
        }
        
        // 验证描述文本安全性
        String description = request.getDescription();
        if (description != null) {
            if (description.length() > MAX_DESCRIPTION_LENGTH) {
                return ValidationResult.error("description_too_long");
            }
            if (DANGEROUS_PATTERN.matcher(description).matches()) {
                return ValidationResult.error("dangerous_content");
            }
        }
        
        return ValidationResult.success();
    }
    
    // 物品黑名单检查
    private static boolean isBlacklistedItem(ItemStack itemStack) {
        Set<String> blacklist = TradingSystemConfig.getBlacklistedItems();
        String itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
        
        if (blacklist.contains(itemId)) {
            return true;
        }
        
        // 检查特殊NBT标签
        CompoundTag nbt = itemStack.getTag();
        if (nbt != null) {
            if (nbt.contains("creative_only") || nbt.contains("admin_item")) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean hasEnoughItems(Player player, ItemStack required, int quantity) {
        int totalCount = 0;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, required)) {
                totalCount += stack.getCount();
                if (totalCount >= quantity) {
                    return true;
                }
            }
        }
        
        return false;
    }
}

// 防刷机制
public class AntiSpamManager {
    private final Map<UUID, ActionTracker> playerTrackers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
    
    public AntiSpamManager() {
        // 定期清理过期数据
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredTrackers, 5, 5, TimeUnit.MINUTES);
    }
    
    public boolean canPerformAction(Player player, ActionType actionType) {
        UUID playerId = player.getUUID();
        ActionTracker tracker = playerTrackers.computeIfAbsent(playerId, k -> new ActionTracker());
        
        return tracker.canPerformAction(actionType);
    }
    
    public void detectSuspiciousActivity(UUID playerId, TradeAction action) {
        ActionTracker tracker = playerTrackers.get(playerId);
        if (tracker != null && tracker.isPatternSuspicious(action)) {
            flagPlayer(playerId, "Suspicious trading pattern detected");
        }
    }
    
    private void flagPlayer(UUID playerId, String reason) {
        TradingSystemMod.LOGGER.warn("Flagged player {} for: {}", playerId, reason);
        // 可以添加更多处理逻辑，如临时禁用交易功能
    }
    
    private void cleanupExpiredTrackers() {
        long currentTime = System.currentTimeMillis();
        playerTrackers.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getLastActionTime() > TimeUnit.HOURS.toMillis(1));
    }
    
    private static class ActionTracker {
        private final Map<ActionType, Long> lastActionTimes = new ConcurrentHashMap<>();
        private final Map<ActionType, Integer> actionCounts = new ConcurrentHashMap<>();
        private long lastResetTime = System.currentTimeMillis();
        
        public boolean canPerformAction(ActionType actionType) {
            long currentTime = System.currentTimeMillis();
            
            // 重置计数器（每分钟）
            if (currentTime - lastResetTime > TimeUnit.MINUTES.toMillis(1)) {
                actionCounts.clear();
                lastResetTime = currentTime;
            }
            
            // 检查最小间隔
            Long lastTime = lastActionTimes.get(actionType);
            if (lastTime != null && (currentTime - lastTime) < actionType.getMinInterval()) {
                return false;
            }
            
            // 检查频率限制
            int count = actionCounts.getOrDefault(actionType, 0);
            if (count >= actionType.getMaxActionsPerMinute()) {
                return false;
            }
            
            // 记录操作
            lastActionTimes.put(actionType, currentTime);
            actionCounts.put(actionType, count + 1);
            
            return true;
        }
        
        public boolean isPatternSuspicious(TradeAction action) {
            // 实现可疑模式检测逻辑
            return false;
        }
        
        public long getLastActionTime() {
            return lastActionTimes.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        }
    }
    
    public enum ActionType {
        LIST_ITEM(500, 20),
        BUY_ITEM(200, 30),
        SEND_TRADE(1000, 10),
        RECYCLE_ITEM(100, 50);
        
        private final long minInterval;
        private final int maxActionsPerMinute;
        
        ActionType(long minInterval, int maxActionsPerMinute) {
            this.minInterval = minInterval;
            this.maxActionsPerMinute = maxActionsPerMinute;
        }
        
        public long getMinInterval() {
            return minInterval;
        }
        
        public int getMaxActionsPerMinute() {
            return maxActionsPerMinute;
        }
    }
}
```

#### 9.2 数据验证机制
- **输入验证**：所有用户输入进行严格验证
- **物品验证**：检查物品的合法性和完整性
- **价格验证**：防止负数价格和超大数值
- **权限验证**：每个操作都进行权限检查

#### 9.3 审计日志系统
```java
public class AuditLogger {
    private final Logger logger = LogManager.getLogger("TradeAudit");
    
    public void logTrade(UUID buyer, UUID seller, ItemStack item, int price) {
        AuditEvent event = new AuditEvent()
            .setType("TRADE_COMPLETED")
            .setBuyer(buyer)
            .setSeller(seller)
            .setItem(item.getDescriptionId())
            .setPrice(price)
            .setTimestamp(System.currentTimeMillis());
            
        logger.info("Trade: {} bought {} from {} for {} coins", 
            buyer, item.getDescriptionId(), seller, price);
        
        // 保存到数据库
        saveAuditEvent(event);
    }
}
```

### 10. 性能优化实现原理

#### 10.1 缓存策略
```java
public class TradeCache {
    private final LoadingCache<String, MarketItem> hotItemCache;
    private final Cache<String, List<MarketItem>> searchResultCache;
    private final TradeDataManager dataManager;
    
    public TradeCache(TradeDataManager dataManager) {
        this.dataManager = dataManager;
        
        // 热点物品缓存（最近访问的物品）
        this.hotItemCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .refreshAfterWrite(Duration.ofMinutes(5))
            .build(this::loadItemFromStorage);
            
        // 搜索结果缓存（避免重复查询）
        this.searchResultCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(2))
            .build();
    }
    
    public MarketItem getItem(String itemId) {
        return hotItemCache.get(itemId);
    }
    
    public List<MarketItem> searchItems(ItemSearchCriteria criteria) {
        String cacheKey = criteria.toCacheKey();
        
        return searchResultCache.get(cacheKey, key -> {
            return dataManager.searchItems(criteria);
        });
    }
    
    private MarketItem loadItemFromStorage(String itemId) {
        return dataManager.getItemById(itemId);
    }
    
    // 缓存失效通知
    public void invalidateItem(String itemId) {
        hotItemCache.invalidate(itemId);
        // 清除相关搜索结果缓存
        searchResultCache.asMap().keySet().removeIf(key -> 
            key.contains(itemId.substring(0, Math.min(8, itemId.length()))));
    }
}
```

#### 10.2 异步处理机制
- **异步数据库操作**：使用线程池处理数据库读写
- **批量操作**：合并多个小操作为批量操作
- **延迟写入**：非关键数据延迟写入数据库

#### 10.3 内存管理
- **对象池**：重用频繁创建的对象
- **弱引用**：对大对象使用弱引用避免内存泄漏
- **定期清理**：定期清理过期数据和缓存

### 11. 错误处理实现原理

#### 11.1 异常分类处理
```java
public class TradeExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private final Map<String, Integer> retryCounters = new HashMap<>();
    
    public void handleTradeException(Exception e, TradeContext context) {
        try {
            // 空值检查
            if (context == null) {
                LOGGER.error("TradeContext is null", e);
                return;
            }
            
            if (e instanceof NetworkException) {
                handleNetworkException((NetworkException) e, context);
            } else if (e instanceof ValidationException) {
                handleValidationException((ValidationException) e, context);
            } else if (e instanceof DataException) {
                handleDataException((DataException) e, context);
            } else if (e instanceof NullPointerException) {
                handleNullPointerException((NullPointerException) e, context);
            } else if (e instanceof IllegalArgumentException) {
                handleIllegalArgumentException((IllegalArgumentException) e, context);
            } else {
                handleUnknownException(e, context);
            }
        } catch (Exception handlerException) {
            // 异常处理器本身出错时的兜底处理
            LOGGER.fatal("Exception handler failed", handlerException);
            emergencyShutdown(context);
        }
    }
    
    private void handleNetworkException(NetworkException e, TradeContext context) {
        String contextId = context.getId();
        int retryCount = retryCounters.getOrDefault(contextId, 0);
        
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            retryCounters.put(contextId, retryCount + 1);
            LOGGER.warn("Network error, retrying ({}/{}): {}", retryCount + 1, MAX_RETRY_ATTEMPTS, e.getMessage());
            scheduleRetry(context, calculateBackoffDelay(retryCount));
        } else {
            LOGGER.error("Network error exceeded max retries", e);
            retryCounters.remove(contextId);
            notifyPlayerOfFailure(context.getPlayer(), "network.error.max_retries");
        }
    }
    
    private void handleValidationException(ValidationException e, TradeContext context) {
        LOGGER.warn("Validation error: {}", e.getMessage());
        if (context.getPlayer() != null) {
            sendLocalizedErrorMessage(context.getPlayer(), "validation.error", e.getMessage());
        }
        // 清理无效状态
        cleanupInvalidState(context);
    }
    
    private void handleDataException(DataException e, TradeContext context) {
        LOGGER.error("Data error in trade", e);
        
        // 尝试数据恢复
        if (attemptDataRecovery(context)) {
            LOGGER.info("Data recovery successful for context: {}", context.getId());
        } else {
            LOGGER.error("Data recovery failed, rolling back trade");
            rollbackTrade(context);
        }
    }
    
    private void handleNullPointerException(NullPointerException e, TradeContext context) {
        LOGGER.error("Null pointer exception in trade system", e);
        
        // 检查关键对象是否为空
        if (context.getPlayer() == null) {
            LOGGER.error("Player is null in trade context");
            return;
        }
        
        // 尝试重新初始化
        if (reinitializeContext(context)) {
            LOGGER.info("Context reinitialization successful");
        } else {
            emergencyShutdown(context);
        }
    }
    
    private void handleIllegalArgumentException(IllegalArgumentException e, TradeContext context) {
        LOGGER.warn("Illegal argument in trade operation: {}", e.getMessage());
        sendLocalizedErrorMessage(context.getPlayer(), "argument.error", e.getMessage());
        resetContextToSafeState(context);
    }
    
    private void handleUnknownException(Exception e, TradeContext context) {
        LOGGER.error("Unexpected error in trade system", e);
        
        // 生成错误报告
        String errorReport = generateErrorReport(e, context);
        saveErrorReport(errorReport);
        
        // 通知管理员
        notifyAdministrators(e, context);
        
        // 安全关闭
        emergencyShutdown(context);
    }
    
    private long calculateBackoffDelay(int retryCount) {
        return Math.min(1000L * (1L << retryCount), 30000L); // 指数退避，最大30秒
    }
    
    private boolean attemptDataRecovery(TradeContext context) {
        try {
            // 尝试从备份恢复数据
            TradeDataManager dataManager = TradeDataManager.get(context.getLevel());
            return dataManager.recoverFromBackup(context.getId());
        } catch (Exception e) {
            LOGGER.error("Data recovery attempt failed", e);
            return false;
        }
    }
    
    private boolean reinitializeContext(TradeContext context) {
        try {
            context.reset();
            context.initialize();
            return true;
        } catch (Exception e) {
            LOGGER.error("Context reinitialization failed", e);
            return false;
        }
    }
    
    private void emergencyShutdown(TradeContext context) {
        try {
            if (context != null && context.getPlayer() != null) {
                sendLocalizedErrorMessage(context.getPlayer(), "system.emergency_shutdown", "");
            }
            // 强制清理资源
            forceCleanup(context);
        } catch (Exception e) {
            LOGGER.fatal("Emergency shutdown failed", e);
        }
    }
}
```

#### 11.2 事务回滚机制
- **状态快照**：操作前保存状态快照
- **补偿操作**：定义每个操作的补偿操作
- **自动回滚**：异常时自动执行回滚操作

#### 11.3 用户友好的错误提示
- **本地化错误信息**：支持多语言错误提示
- **详细错误描述**：提供具体的错误原因和解决建议
- **错误代码系统**：使用错误代码便于问题定位

### 12. 扩展性设计实现原理

#### 12.1 插件系统架构
```java
public interface TradeExtension {
    void onTradeCompleted(TradeEvent event);
    void onItemListed(ListItemEvent event);
    boolean canTradeItem(ItemStack item, Player player);
    void modifyTradePrice(PriceModificationEvent event);
}

public class ExtensionManager {
    private List<TradeExtension> extensions = new ArrayList<>();
    
    public void registerExtension(TradeExtension extension) {
        extensions.add(extension);
        logger.info("Registered trade extension: {}", extension.getClass().getSimpleName());
    }
    
    public void fireTradeEvent(TradeEvent event) {
        extensions.forEach(ext -> {
            try {
                ext.onTradeCompleted(event);
            } catch (Exception e) {
                logger.error("Extension error", e);
            }
        });
    }
}
```

#### 12.2 配置系统扩展
- **模块化配置**：每个功能模块独立配置文件
- **热重载**：支持运行时重新加载配置
- **配置验证**：配置文件格式和内容验证

#### 12.3 API接口设计
- **RESTful API**：提供HTTP API供外部系统调用
- **事件API**：允许其他MOD监听交易事件
- **数据API**：提供数据查询和统计接口

## 技术实现方案

### 1. 数据存储

#### 1.1 玩家数据
```json
{
  "uuid": "玩家UUID",
  "coins": 1000,
  "listedItems": [
    {
      "id": "商品ID",
      "item": "物品NBT数据",
      "price": 100,
      "quantity": 64,
      "timestamp": "上架时间"
    }
  ],
  "pendingTrades": [
    {
      "id": "交易ID",
      "from": "发送者UUID",
      "item": "物品NBT数据",
      "requiredPayment": 50,
      "message": "交易备注",
      "timestamp": "发送时间"
    }
  ]
}
```

#### 1.2 系统配置
```json
{
  "recycleItems": {
    "minecraft:diamond": 100,
    "minecraft:iron_ingot": 10,
    "minecraft:gold_ingot": 20
  },
  "tradeExpireTime": 604800,
  "maxListedItems": 50
}
```

### 2. 网络通信

#### 2.1 数据包设计
- **OpenTradeGUI**：打开交易界面
- **ListItem**：上架商品
- **BuyItem**：购买商品
- **SendTrade**：发送定向交易
- **AcceptTrade**：接受交易
- **RecycleItem**：回收物品
- **UpdateCoins**：更新货币显示

### 3. 权限系统

#### 3.1 权限等级
- **普通玩家**：基础交易功能
- **管理员**：设置回收价格、管理交易系统
- **超级管理员**：完整系统控制权限

## 配置文件设计

### 1. 主配置文件 (config/trading-system-common.toml)
```toml
[general]
# 是否启用交易系统
enabled = true
# 最大同时上架物品数量
max_listings_per_player = 10
# 交易手续费百分比 (0-100)
transaction_fee_percent = 5.0
# 是否启用系统回收功能
enable_system_recycle = true
# 数据自动保存间隔（秒）
auto_save_interval = 300
# 交易过期时间（秒）
trade_expire_time = 604800

[currency]
# 货币名称
currency_name = "金币"
# 货币符号
currency_symbol = "¤"
# 初始货币数量
starting_currency = 100
# 最大货币持有量
max_currency = 1000000
# 是否启用货币掉落（死亡时）
currency_drop_on_death = false
# 货币掉落百分比
currency_drop_percent = 10.0

[security]
# 启用防刷机制
enable_anti_spam = true
# 每秒最大操作次数
max_actions_per_second = 2.0
# 操作最小间隔（毫秒）
min_action_interval = 500
# 启用输入验证
enable_input_validation = true
# 最大物品描述长度
max_description_length = 256

[performance]
# 缓存大小
cache_size = 1000
# 缓存过期时间（分钟）
cache_expire_minutes = 30
# 启用数据分片
enable_data_sharding = true
# 分片大小限制
shard_size_limit = 10000
# 启用异步保存
enable_async_save = true

[backup]
# 启用自动备份
enable_auto_backup = true
# 增量备份间隔（小时）
incremental_backup_hours = 1
# 完整备份间隔（小时）
full_backup_hours = 24
# 保留备份数量
max_backup_files = 30

# 物品黑名单
[blacklist]
items = [
    "minecraft:bedrock",
    "minecraft:command_block",
    "minecraft:structure_block",
    "minecraft:barrier"
]

# 系统回收价格配置
[recycle_prices]
"minecraft:diamond" = 100
"minecraft:emerald" = 50
"minecraft:gold_ingot" = 25
"minecraft:iron_ingot" = 10
"minecraft:coal" = 1
```

### 2. 客户端配置文件 (config/trading-system-client.toml)
```toml
[ui]
# GUI缩放比例
ui_scale = 1.0
# 是否显示动画
show_animations = true
# 界面主题
theme = "default"
# 是否启用音效
enable_sounds = true
# 音效音量 (0.0-1.0)
sound_volume = 0.5
# 打开交易界面的快捷键
open_key = "T"
# 界面标题
gui_title = "交易系统"

[display]
# 每页显示物品数量
items_per_page = 20
# 是否显示物品工具提示
show_item_tooltips = true
# 是否显示价格历史
show_price_history = false
# 货币显示格式
currency_format = "#,##0"

[keybinds]
# 打开交易界面快捷键
open_trade_gui = "key.keyboard.t"
# 快速搜索快捷键
quick_search = "key.keyboard.f"
```

### 3. 配置管理器实现
```java
public class TradingSystemConfig {
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    
    // 通用配置
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue MAX_LISTINGS_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue TRANSACTION_FEE_PERCENT;
    public static final ModConfigSpec.BooleanValue ENABLE_SYSTEM_RECYCLE;
    public static final ModConfigSpec.IntValue AUTO_SAVE_INTERVAL;
    
    // 货币配置
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_NAME;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_SYMBOL;
    public static final ModConfigSpec.IntValue STARTING_CURRENCY;
    public static final ModConfigSpec.IntValue MAX_CURRENCY;
    
    // 安全配置
    public static final ModConfigSpec.BooleanValue ENABLE_ANTI_SPAM;
    public static final ModConfigSpec.DoubleValue MAX_ACTIONS_PER_SECOND;
    public static final ModConfigSpec.IntValue MIN_ACTION_INTERVAL;
    public static final ModConfigSpec.BooleanValue ENABLE_INPUT_VALIDATION;
    
    // 物品黑名单
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ITEMS;
    
    // 客户端配置
    public static final ModConfigSpec.DoubleValue UI_SCALE;
    public static final ModConfigSpec.BooleanValue SHOW_ANIMATIONS;
    public static final ModConfigSpec.ConfigValue<String> THEME;
    public static final ModConfigSpec.BooleanValue ENABLE_SOUNDS;
    
    static {
        // 初始化通用配置
        COMMON_BUILDER.comment("Trading System Configuration").push("general");
        ENABLED = COMMON_BUILDER.comment("Enable trading system").define("enabled", true);
        MAX_LISTINGS_PER_PLAYER = COMMON_BUILDER.comment("Maximum listings per player")
            .defineInRange("max_listings_per_player", 10, 1, 100);
        TRANSACTION_FEE_PERCENT = COMMON_BUILDER.comment("Transaction fee percentage")
            .defineInRange("transaction_fee_percent", 5.0, 0.0, 50.0);
        ENABLE_SYSTEM_RECYCLE = COMMON_BUILDER.comment("Enable system recycle feature")
            .define("enable_system_recycle", true);
        AUTO_SAVE_INTERVAL = COMMON_BUILDER.comment("Auto save interval in seconds")
            .defineInRange("auto_save_interval", 300, 60, 3600);
        COMMON_BUILDER.pop();
        
        COMMON_BUILDER.comment("Currency settings").push("currency");
        CURRENCY_NAME = COMMON_BUILDER.comment("Currency name")
            .define("currency_name", "金币");
        CURRENCY_SYMBOL = COMMON_BUILDER.comment("Currency symbol")
            .define("currency_symbol", "¤");
        STARTING_CURRENCY = COMMON_BUILDER.comment("Starting currency amount")
            .defineInRange("starting_currency", 100, 0, 10000);
        MAX_CURRENCY = COMMON_BUILDER.comment("Maximum currency amount")
            .defineInRange("max_currency", 1000000, 1000, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();
        
        COMMON_BUILDER.comment("Security settings").push("security");
        ENABLE_ANTI_SPAM = COMMON_BUILDER.comment("Enable anti-spam protection")
            .define("enable_anti_spam", true);
        MAX_ACTIONS_PER_SECOND = COMMON_BUILDER.comment("Maximum actions per second")
            .defineInRange("max_actions_per_second", 2.0, 0.1, 10.0);
        MIN_ACTION_INTERVAL = COMMON_BUILDER.comment("Minimum action interval in milliseconds")
            .defineInRange("min_action_interval", 500, 100, 5000);
        ENABLE_INPUT_VALIDATION = COMMON_BUILDER.comment("Enable input validation")
            .define("enable_input_validation", true);
        COMMON_BUILDER.pop();
        
        COMMON_BUILDER.comment("Item blacklist").push("blacklist");
        BLACKLISTED_ITEMS = COMMON_BUILDER.comment("Blacklisted items")
            .defineList("items", Arrays.asList(
                "minecraft:bedrock",
                "minecraft:command_block",
                "minecraft:structure_block",
                "minecraft:barrier"
            ), obj -> obj instanceof String);
        COMMON_BUILDER.pop();
        
        // 初始化客户端配置
        CLIENT_BUILDER.comment("UI settings").push("ui");
        UI_SCALE = CLIENT_BUILDER.comment("UI scale factor")
            .defineInRange("ui_scale", 1.0, 0.5, 3.0);
        SHOW_ANIMATIONS = CLIENT_BUILDER.comment("Show UI animations")
            .define("show_animations", true);
        THEME = CLIENT_BUILDER.comment("UI theme")
            .define("theme", "default");
        ENABLE_SOUNDS = CLIENT_BUILDER.comment("Enable UI sounds")
            .define("enable_sounds", true);
        CLIENT_BUILDER.pop();
    }
    
    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
    
    // 便捷访问方法
    public static boolean isEnabled() {
        return ENABLED.get();
    }
    
    public static int getMaxListingsPerPlayer() {
        return MAX_LISTINGS_PER_PLAYER.get();
    }
    
    public static double getTransactionFeePercent() {
        return TRANSACTION_FEE_PERCENT.get();
    }
    
    public static Set<String> getBlacklistedItems() {
        return new HashSet<>(BLACKLISTED_ITEMS.get());
    }
    
    public static String getCurrencyName() {
        return CURRENCY_NAME.get();
    }
    
    public static String formatCurrency(int amount) {
        return String.format("%s%,d", CURRENCY_SYMBOL.get(), amount);
    }
    
    public static boolean isAntiSpamEnabled() {
        return ENABLE_ANTI_SPAM.get();
    }
    
    public static boolean isInputValidationEnabled() {
        return ENABLE_INPUT_VALIDATION.get();
    }
}
```

## 开发计划

### 阶段一：基础框架
1. 创建基础MOD结构
2. 实现货币系统
3. 创建基础GUI框架
4. 实现数据存储系统

### 阶段二：核心功能
1. 实现商品上架功能
2. 实现商品购买功能
3. 实现系统回收功能
4. 完善UI界面

### 阶段三：高级功能
1. 实现定向交易系统
2. 添加搜索和筛选功能
3. 实现权限管理
4. 优化用户体验

### 阶段四：测试和优化
1. 功能测试
2. 性能优化
3. 错误处理
4. 文档完善

## 用户体验设计

### 1. 界面设计原则
- **简洁明了**：界面布局清晰，操作直观
- **响应迅速**：操作反馈及时
- **信息完整**：显示必要的交易信息
- **错误提示**：友好的错误提示信息

### 2. 操作流程优化
- **快捷操作**：支持快捷键和右键菜单
- **批量操作**：支持批量上架和回收
- **确认机制**：重要操作需要二次确认
- **撤销功能**：支持取消上架等操作

## 安全性考虑

### 1. 数据验证
- **物品验证**：确保物品数据的完整性
- **价格验证**：防止异常价格设定
- **权限验证**：确保操作权限正确

### 2. 防作弊机制
- **交易记录**：记录所有交易操作
- **异常检测**：检测异常的交易行为
- **回滚机制**：支持交易回滚功能

---

**请确认以上设计方案是否符合您的需求，确认后我将开始代码开发工作。**