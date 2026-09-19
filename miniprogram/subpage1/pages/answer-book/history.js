// pages/answer-book/history.js
const { request } = require('../../../utils/request.js');

Page({
  data: {
    records: [],
    page: 1,
    size: 20,
    hasMore: true,
    loading: false
  },

  onLoad() {
    this.loadHistory();
  },

  // 上拉加载（触底自动加载）
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      this.loadHistory();
    }
  },

  // 加载历史记录
  loadHistory() {
    const { page, size } = this.data;
    
    this.setData({ loading: true });

    return request({
      url: `/answer-book/api/v1/history?page=${page}&size=${size}`,
      method: 'GET'
    }).then(res => {
      if (res.data && res.data.records) {
        const newRecords = res.data.records;
        const allRecords = page === 1 ? newRecords : [...this.data.records, ...newRecords];
        
        this.setData({
          records: allRecords,
          hasMore: newRecords.length >= size,
          loading: false
        });
      } else {
        this.setData({
          hasMore: false,
          loading: false
        });
      }
    }).catch(err => {
      console.error('加载历史记录失败', err);
      this.setData({ loading: false });
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ page: 1, records: [] });
    this.loadHistory().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 分享给朋友
  onShareAppMessage() {
    return {
      title: '答案之书 - 历史记录',
      path: '/subpage1/pages/answer-book/history',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: '答案之书 - 历史记录',
      query: '',
      imageUrl: '' // 可以设置自定义分享图片
    };
  }
});
