@echo off
echo ================================
echo  启动开发环境容器（仅启动未运行的）
echo ================================
cd /d D:\project\group-buy-market-hth\docs\dev-ops
docker compose -f docker-compose-environment-aliyun.yml up -d
echo.
echo 全部已启动！
pause
