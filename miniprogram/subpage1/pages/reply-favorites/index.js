const { requestWithLogin } = require('../../../utils/request');

const STYLE_TEXT = {
  safe: '稳妥表达',
  firm: '坚定边界',
  gentle: '温柔缓和',
  short: '短句版'
};

Page({
  data: {
    favorites: [],
    page: 1,
    size: 20,
    hasMore: true,
    loading: false
  },

  onLoad() {
    this.loadFavorites(true);
  },

  onPullDownRefresh() {
    this.loadFavorites(true).then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    this.loadMore();
  },

  loadMore() {
    if (this.data.loading || !this.data.hasMore) return;
    this.setData({ page: this.data.page + 1 });
    this.loadFavorites(false);
  },

  loadFavorites(reset) {
    if (this.data.loading) return Promise.resolve();
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true, page });
    return requestWithLogin({
      url: `/emotion/api/v1/favorites?page=${page}&size=${this.data.size}`,
      method: 'GET'
    }).then(res => {
      const data = res.data || {};
      const records = (data.records || []).map(item => ({
        ...item,
        replyStyleText: STYLE_TEXT[item.replyStyle] || item.replyStyle || '收藏话术',
        createTimeText: this.formatTime(item.createTime)
      }));
      this.setData({
        favorites: reset ? records : this.data.favorites.concat(records),
        hasMore: page < (data.pages || 0),
        loading: false
      });
    }).catch(err => {
      console.error('[reply favorites] 加载失败:', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
      this.setData({ loading: false });
    });
  },

  copyReply(e) {
    const text = e.currentTarget.dataset.text || '';
    if (!text) return;
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: '已复制', icon: 'success' })
    });
  },

  deleteFavorite(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    requestWithLogin({
      url: `/emotion/api/v1/favorites/${id}`,
      method: 'DELETE'
    }).then(() => {
      wx.showToast({ title: '已删除', icon: 'success' });
      this.setData({
        favorites: this.data.favorites.filter(item => item.id !== id)
      });
    }).catch(err => {
      console.error('[reply favorites] 删除失败:', err);
      wx.showToast({ title: '删除失败', icon: 'none' });
    });
  },

  formatTime(value) {
    if (!value) return '';
    if (typeof value === 'string') return value.replace('T', ' ').slice(0, 16);
    return '';
  }
});
