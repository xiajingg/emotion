#!/bin/bash
# 执行答案之书相关SQL脚本

echo "========================================="
echo "开始执行答案之书数据库初始化脚本"
echo "========================================="

# 数据库配置（请根据实际情况修改）
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="emotion"
DB_USER="root"
DB_PASSWORD=""

echo ""
echo "1. 创建 user_usage_log 表..."
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} ${DB_NAME} < user_usage_log.sql

if [ $? -eq 0 ]; then
    echo "✓ user_usage_log 表创建成功"
else
    echo "✗ user_usage_log 表创建失败"
    exit 1
fi

echo ""
echo "2. 创建 answer_book_answers 和 answer_book_records 表..."
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} ${DB_NAME} < answer_book.sql

if [ $? -eq 0 ]; then
    echo "✓ 答案之书相关表创建成功"
else
    echo "✗ 答案之书相关表创建失败"
    exit 1
fi

echo ""
echo "3. 导入预设答案数据..."
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} ${DB_NAME} < answer_book_data.sql

if [ $? -eq 0 ]; then
    echo "✓ 预设答案数据导入成功"
else
    echo "✗ 预设答案数据导入失败"
    exit 1
fi

echo ""
echo "========================================="
echo "数据库初始化完成！"
echo "========================================="
echo ""
echo "验证表是否创建成功："
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} ${DB_NAME} -e "SHOW TABLES LIKE '%answer%'; SHOW TABLES LIKE '%usage_log%';"
