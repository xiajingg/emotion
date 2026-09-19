# 微信小程序源码压缩工具 - 使用说明

## 🚀 一键执行

```bash
cd /Users/xiajing/emotion
./scripts/compress-miniprogram.sh
```

## ✨ 自动完成的操作

1. ✅ **备份代码** - 自动备份到 `backups/compress-时间戳/`
2. ✅ **清理无用文件** - 删除 .DS_Store、备份文件、示例文件
3. ✅ **优化配置** - 关闭 SourceMap 和代码保护
4. ✅ **统计结果** - 显示优化前后的大小对比

## 📊 预期效果

- 上传体积减少：**30-50%**（主要来自关闭 SourceMap）
- 代码体积减少：**5-10%**（清理无用文件）

## ⚠️ 注意事项

1. 脚本会自动备份，但建议先提交 Git
2. 如果有本地图片，需要手动用 TinyPNG 压缩
3. 执行后在微信开发者工具中重新编译上传

## 🔄 恢复备份

如果出现问题，可以恢复备份：

```bash
cd /Users/xiajing/emotion
rm -rf miniprogram/pages miniprogram/components miniprogram/utils miniprogram/subpage1
cp -r backups/compress-最新时间戳/* miniprogram/
```
