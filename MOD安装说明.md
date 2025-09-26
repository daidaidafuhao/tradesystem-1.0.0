# Trade System MOD 安装说明

## MOD 信息
- **MOD名称**: Trade System (交易系统)
- **版本**: 1.0.0
- **Minecraft版本**: 1.20.1
- **Forge版本**: 47.4.9+
- **文件名**: `tradesystem-1.0.0.jar`

## 系统要求
- Minecraft 1.20.1
- Minecraft Forge 47.4.9 或更高版本
- Java 17 或更高版本

## 安装步骤

### 客户端安装
1. 确保已安装 Minecraft Forge 1.20.1-47.4.9 或更高版本
2. 将 `tradesystem-1.0.0.jar` 文件复制到 Minecraft 的 `mods` 文件夹中
   - Windows: `%APPDATA%\.minecraft\mods\`
   - macOS: `~/Library/Application Support/minecraft/mods/`
   - Linux: `~/.minecraft/mods/`
3. 启动 Minecraft 客户端

### 服务端安装
1. 确保服务器运行 Minecraft Forge 1.20.1-47.4.9 或更高版本
2. 将 `tradesystem-1.0.0.jar` 文件复制到服务器的 `mods` 文件夹中
3. 重启服务器

## 功能特性
- **玩家货币系统**: 支持虚拟货币交易
- **物品上架系统**: 玩家可以上架物品进行交易
- **交易历史记录**: 完整的交易记录追踪
- **多语言支持**: 支持中文和英文
- **数据持久化**: 所有交易数据自动保存

## 使用说明

### 基本命令
- `/currency balance` - 查看当前货币余额
- `/currency add <数量>` - 添加货币（管理员）
- `/currency remove <数量>` - 扣除货币（管理员）
- `/trade list` - 查看市场上架物品
- `/trade sell <价格>` - 上架手中物品

### 游戏内操作
1. 使用命令或GUI界面进行交易
2. 查看个人货币余额和交易历史
3. 浏览市场上的可购买物品
4. 上架自己的物品进行出售

## 配置文件
MOD会在服务器的 `world/data/` 目录下创建数据文件，用于存储：
- 玩家货币数据
- 市场物品信息
- 交易历史记录

## 兼容性说明
- **客户端-服务端**: 需要在客户端和服务端都安装此MOD
- **单人游戏**: 支持单人游戏模式
- **多人游戏**: 支持多人服务器环境
- **其他MOD**: 与大多数MOD兼容，无已知冲突

## 故障排除

### 常见问题
1. **MOD无法加载**
   - 检查Forge版本是否匹配
   - 确认Java版本为17或更高

2. **数据丢失**
   - 检查服务器的数据文件权限
   - 确保 `world/data/` 目录可写

3. **命令无效**
   - 确认MOD已正确加载
   - 检查玩家权限设置

### 日志文件
如遇问题，请查看以下日志文件：
- 客户端: `.minecraft/logs/latest.log`
- 服务端: `logs/latest.log`

## 技术支持
如需技术支持或报告问题，请提供：
1. Minecraft版本
2. Forge版本
3. MOD版本
4. 详细的错误描述
5. 相关日志文件

## 更新说明
- 定期备份世界数据
- 更新前请先备份 `world/data/` 目录
- 新版本通常向后兼容

---
**注意**: 此MOD需要在客户端和服务端同时安装才能正常工作。