// subpage1/pages/daily-bonus/index.js
const { request } = require('../../../utils/request');

Page({
  data: {
    quotaHistory: [],
    refreshTime: ''
  },

  onLoad() {
    this.refreshPageTime();
    this.getQuotaHistory();
  },

  onPullDownRefresh() {
    this.refreshPageTime();
    this.getQuotaHistory();
    wx.stopPullDownRefresh();
  },

  goToHomeRecord() {
    wx.reLaunch({
      url: '/pages/index/index'
    });
  },

  refreshPageTime() {
    this.setData({
      refreshTime: this.formatTime(new Date())
    });
  },

  // 获取最近参与记录
  async getQuotaHistory() {
    try {
      const res = await request({
        url: '/emotion/quota/history',
        method: 'POST'
      });
      
      const quotaHistory = res.data.map(item => {
        // ✅ 判断是否为当前用户（检查昵称是否包含_IS_ME_标记）
        const isMe = item.nickname && item.nickname.includes('_IS_ME_');
        
        // ✅ 后端已脱敏，直接使用 nickname 字段
        let displayName;
        if (item.nickname) {
          // 移除_IS_ME_标记
          displayName = item.nickname.replace('_IS_ME_', '');
        } else {
          // 理论上不会走到这里，因为后端已经处理了
          displayName = '未知用户';
        }
        
        return {
          id: item.id,  // ✅ 使用 id 而非 openId
          displayName: displayName,
          claimTime: item.grabTime,
          claimTimeFormatted: this.formatTime(item.grabTime),
          amount: item.rewardUsageCount,
          isMe: isMe
        };
      });
      
      this.setData({ quotaHistory });
    } catch (error) {
      console.error('获取参与记录失败:', error);
      wx.showToast({
        title: '获取记录失败，请下拉刷新',
        icon: 'none',
        duration: 2000
      });
    }
  },

  // 格式化时间
  formatTime(time) {
    const date = new Date(time);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}年${month}月${day}日 ${hours}:${minutes}`;
  },

  // 分享给朋友
  onShareAppMessage() {
    return {
      title: '每日情绪补给 - 今天也可以轻一点',
      path: '/subpage1/pages/daily-bonus/index',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: '每日情绪补给 - 今天也可以轻一点',
      query: '',
      imageUrl: '' // 可以设置自定义分享图片
    };
  }
});
