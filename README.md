# Trade System MOD

一个功能完整的Minecraft交易系统MOD，支持玩家间的物品交易和虚拟货币系统。

## 项目结构

```
src/main/java/com/tradesystem/mod/
├── TradeMod.java                    # 主MOD类
├── capability/                      # 玩家能力系统
│   ├── IPlayerCurrency.java        # 货币接口
│   ├── PlayerCurrency.java         # 货币实现
│   ├── PlayerCurrencyProvider.java # 货币提供者
│   └── ModCapabilities.java        # 能力注册
├── client/                          # 客户端代码
│   ├── ClientCurrencyManager.java  # 客户端货币管理
│   └── gui/                         # GUI界面
├── command/                         # 命令系统
│   ├── CurrencyCommand.java        # 货币命令
│   └── TradeCommand.java           # 交易命令
├── data/                           # 数据管理
│   ├── DataService.java            # 数据服务
│   ├── TradeDataManager.java       # 交易数据管理
│   └── TradeSavedData.java         # 数据持久化
├── event/                          # 事件处理
│   └── ModEventHandler.java        # 事件处理器
├── item/                           # 物品系统
│   └── TradeItem.java              # 交易物品
├── manager/                        # 管理器
│   ├── ItemListingManager.java     # 物品上架管理
│   └── TransactionManager.java     # 交易管理
├── network/                        # 网络通信
│   ├── NetworkHandler.java         # 网络处理器
│   └── packet/                     # 数据包
└── util/                          # 工具类
    └── TransactionRecord.java      # 交易记录
```

## 功能特性

### 核心功能
- **虚拟货币系统**: 完整的玩家货币管理
- **物品交易**: 支持物品上架和购买
- **交易历史**: 完整的交易记录追踪
- **数据持久化**: 自动保存所有交易数据

### 技术特性
- **客户端-服务端同步**: 实时数据同步
- **多语言支持**: 中文和英文界面
- **模块化设计**: 清晰的代码结构
- **事件驱动**: 基于Forge事件系统

## 开发环境

### 要求
- Java 17+
- Minecraft 1.20.1
- Minecraft Forge 47.4.9+
- IntelliJ IDEA 或 Eclipse

### 构建项目
```bash
# 清理项目
./gradlew clean

# 编译项目
./gradlew build

# 运行客户端
./gradlew runClient

# 运行服务端
./gradlew runServer
```

### 生成的文件
构建完成后，JAR文件位于：
- `build/libs/tradesystem-1.0.0.jar`

## 安装使用

详细安装说明请参考 [MOD安装说明.md](MOD安装说明.md)

## 开发说明

### 添加新功能
1. 在相应的包中创建新类
2. 注册必要的事件处理器
3. 添加网络数据包（如需要）
4. 更新数据持久化逻辑

### 数据流程
1. **客户端操作** → 发送数据包到服务端
2. **服务端处理** → 更新数据并保存
3. **数据同步** → 广播更新到所有客户端
4. **持久化** → 自动保存到世界数据

### 调试技巧
- 使用 `./gradlew runClient` 启动开发环境
- 查看 `logs/debug.log` 获取详细日志
- 使用IDE断点调试服务端逻辑

## 版本历史

### v1.0.0
- 初始版本发布
- 基础货币系统
- 物品交易功能
- 数据持久化
- 多语言支持

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 创建 Pull Request

## 许可证

All Rights Reserved

## 联系方式

如有问题或建议，请通过以下方式联系：
- 项目Issues
- 开发团队邮箱

---

**注意**: 此项目仅供学习和研究使用。