// utils/util.js
/**
 * 打字机效果（逐字显示）
 * @param {string} text      - 完整文本
 * @param {Function} callback - 每次更新回调，传入当前已输出文本
 * @param {number} speed    - 每字间隔 ms，默认 50
 * @returns {Function}        - 调用返回的函数可清除定时器
 */
const typeWriter = (text, callback, speed = 50) => {
  let output = '';
  let i = 0;
  const timer = setInterval(() => {
    if (i < text.length) {
      output += text[i];
      callback(output);
      i++;
    } else {
      clearInterval(timer);
    }
  }, speed);
  // 返回停止函数
  return () => clearInterval(timer);
};

/**
 * 格式化日期为 YYYY-MM-DD
 */
const formatDate = (date) => {
  const d = new Date(date);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

/**
 * 格式化时间为 HH:MM
 */
const formatTime = (date) => {
  const d = new Date(date);
  const h = String(d.getHours()).padStart(2, '0');
  const m = String(d.getMinutes()).padStart(2, '0');
  return `${h}:${m}`;
};

/**
 * 获取今日日期字符串 YYYY-MM-DD
 */
const getToday = () => formatDate(new Date());

/**
 * 防抖函数
 */
const debounce = (fn, delay = 300) => {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
};

/**
 * 防重复点击装饰器（Anti-Double-Click）
 * @param {Function} handler - 原始点击处理函数
 * @param {Object} context - Page/Component 实例（this）
 * @param {String} flagKey - 标志位名称，默认为 '_isSubmitting'
 * @param {Boolean} showLoading - 是否显示全局 loading，默认 false
 * @returns {Function} - 包装后的处理函数
 */
const withAntiDoubleClick = (handler, context, flagKey = '_isSubmitting', showLoading = false) => {
  return async function (...args) {
    // 检查是否正在提交
    if (context.data && context.data[flagKey]) {
      console.warn('[防重复点击] 操作正在进行中，忽略重复点击');
      return;
    }

    try {
      // 设置标志位
      if (context.setData) {
        await new Promise((resolve) => {
          context.setData({ [flagKey]: true }, resolve);
        });
      } else {
        context[flagKey] = true;
      }

      // 可选：显示全局 loading
      if (showLoading) {
        wx.showLoading({ title: '处理中...', mask: true });
      }

      // 执行原始处理函数
      const result = await handler.apply(context, args);
      
      return result;
    } catch (error) {
      console.error('[防重复点击] 处理异常:', error);
      throw error;
    } finally {
      // 恢复状态
      if (showLoading) {
        wx.hideLoading();
      }
      
      if (context.setData) {
        context.setData({ [flagKey]: false });
      } else {
        context[flagKey] = false;
      }
    }
  };
};

module.exports = {
  typeWriter,
  formatDate,
  formatTime,
  getToday,
  debounce,
  withAntiDoubleClick
};
