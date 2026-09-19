// subpage1/pages/ranking/index.js
const { request } = require('../../../utils/request');

Page({
  data: {
    totalData: [],
    isLoadingRank: false
  },

  onLoad() {
    this.loadRank('total');
  },

  onShow() {
    this.loadRank('total');
  },

  // 加载排行数据
  loadRank(type) {
    if (this.data.isLoadingRank) return Promise.resolve();
    this.setData({ isLoadingRank: true });

    wx.showLoading({ title: '加载中...' });

    return request({
      url: '/rank/api/v1/getUserRank?type=1',
      method: 'GET'
    }).then(res => {
      wx.hideLoading();
      const list = (res.data || []).map(it => {
        // 判断是否为当前用户（用户名包含"（我）"或以"我"结尾）
        const isMe = it.userName && (it.userName.includes('（我）') || it.userName === '我');
        return {
          user: it.userName,
          total: it.total,
          isMe: isMe
        };
      });
      this.setData({ totalData: list });
    }).catch(err => {
      wx.hideLoading();
      console.error('获取排行失败:', err);
      wx.showToast({ title: '获取数据失败', icon: 'none' });
    }).finally(() => {
      this.setData({ isLoadingRank: false });
    });
  },

  // 分享给朋友
  onShareAppMessage() {
    return {
      title: '心情排行榜 - 看看谁的心情最好',
      path: '/subpage1/pages/ranking/index',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: '心情排行榜 - 看看谁的心情最好',
      query: '',
      imageUrl: '' // 可以设置自定义分享图片
    };
  }
});
