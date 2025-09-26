@echo off
echo 正在清空交易系统所有数据...

REM 删除交易系统数据文件夹
if exist "run\tradesystem" (
    echo 删除交易系统数据文件夹...
    rmdir /s /q "run\tradesystem"
    echo 交易系统数据文件夹已删除
) else (
    echo 交易系统数据文件夹不存在
)

REM 删除世界存档（可选，如果需要完全重置）
REM if exist "run\world" (
REM     echo 删除世界存档...
REM     rmdir /s /q "run\world"
REM     echo 世界存档已删除
REM )

REM 删除玩家数据（可选）
REM if exist "run\world\playerdata" (
REM     echo 删除玩家数据...
REM     rmdir /s /q "run\world\playerdata"
REM     echo 玩家数据已删除
REM )

echo.
echo 交易系统数据清空完成！
echo 下次启动服务器时将创建全新的数据文件。
echo.
pause