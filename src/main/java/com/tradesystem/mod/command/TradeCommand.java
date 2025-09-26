package com.tradesystem.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.tradesystem.mod.TradeMod;
import com.tradesystem.mod.network.NetworkHandler;
import com.tradesystem.mod.network.packet.OpenTradeGuiPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 交易命令处理类
 * 处理 /trade 命令
 */
public class TradeCommand {
    
    /**
     * 注册交易命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("trade")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(TradeCommand::executeTradeCommand)
        );
        
        // 注册别名命令
        dispatcher.register(
            Commands.literal("tradeshop")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(TradeCommand::executeTradeCommand)
        );
        
        TradeMod.getLogger().info("交易命令已注册: /trade 和 /tradeshop");
    }
    
    /**
     * 执行交易命令
     */
    private static int executeTradeCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayer player) {
            try {
                // 发送数据包到客户端打开交易界面
                NetworkHandler.sendToPlayer(new OpenTradeGuiPacket(), player);
                
                // 发送确认消息给玩家
                player.sendSystemMessage(
                    Component.translatable("command.tradesystem.trade.success")
                );
                
                TradeMod.getLogger().info("玩家 {} 使用命令打开交易界面", player.getName().getString());
                return 1; // 命令执行成功
                
            } catch (Exception e) {
                TradeMod.getLogger().error("执行交易命令时发生错误: {}", e.getMessage());
                player.sendSystemMessage(
                    Component.translatable("command.tradesystem.trade.error")
                );
                return 0; // 命令执行失败
            }
        }
        
        return 0;
    }
}