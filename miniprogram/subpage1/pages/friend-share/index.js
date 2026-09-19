const { requestWithLogin } = require('../../../utils/request');
const { withAntiDoubleClick } = require('../../../utils/util');
const FILE_VIEW_BASE_URL = 'https://www.onekey-ai.top/files/view?id=';

Page({
  data: {
    shareCode: '',
    friendCode: '',
    isBound: false,
    myNickname: '',  // ✅ 当前用户昵称
    friendNickname: '',
    timeline: [],
    dailyAnalysis: null,
    // ✅ 加载状态
    loadingTimeline: false,
    isCopyingCode: false,
    isBindingFriend: false,
    // ✅ 防止重复加载标志
    _hasLoaded: false
  },

  onLoad() {
    // 页面加载时获取绑定状态
    this.loadBindStatus();
  },

  onShow() {
    // ✅ 关键修复：只有从其他页面返回时才刷新（避免首次加载时重复调用）
    if (this._hasLoaded) {
      this.loadBindStatus();
    }
  },

  // 加载绑定状态
  async loadBindStatus() {
    try {
      const res = await requestWithLogin({
        url: '/api/friend/status',
        method: 'GET'
      });
      
      console.log('绑定状态响应:', res);
      
      // ✅ 兼容后端返回 code="0" 或 code=200 或 success=true
      if ((res.code === 200 || res.code === "0" || res.success === true) && res.data) {
        const { shareCode, isBound, myNickname, friendNickname } = res.data;
        
        this.setData({
          shareCode: shareCode || '',
          isBound: isBound || false,
          myNickname: myNickname || '我',  // ✅ 保存当前用户昵称
          friendNickname: friendNickname || ''
        }, () => {
          console.log('状态已更新 - 共享码:', this.data.shareCode, '已绑定:', this.data.isBound);
          
          // ✅ 标记已加载，防止 onShow 重复调用
          this._hasLoaded = true;
          
          // 如果已绑定，并行加载数据
          if (this.data.isBound) {
            this.loadAllData();
          }
        });
      } else {
        console.error('获取绑定状态失败');
        wx.showToast({ title: '获取状态失败', icon: 'none' });
      }
    } catch (err) {
      console.error('获取绑定状态异常:', err);
      wx.showToast({ title: '网络错误', icon: 'none' });
    }
  },
  
  /**
   * ✅ 并行加载所有数据（优化性能）
   */
  async loadAllData() {
    console.log('开始并行加载数据...');
    
    // ✅ 显示骨架屏或加载中提示
    wx.showLoading({ title: '加载中...' });
    
    try {
      const [timelineRes, analysisRes] = await Promise.all([
        requestWithLogin({ url: '/api/friend/timeline', method: 'GET' }),
        requestWithLogin({ url: '/api/friend/timeline/analysis', method: 'GET' })
      ]);
      
      console.log('时间线响应:', timelineRes);
      
      wx.hideLoading();
      
      if ((timelineRes.code === 200 || timelineRes.code === "0" || timelineRes.success === true) && timelineRes.data) {
        this.renderTimeline(timelineRes.data);
      }
      if ((analysisRes.code === 200 || analysisRes.code === "0" || analysisRes.success === true) && analysisRes.data) {
        this.setData({ dailyAnalysis: analysisRes.data });
      }
      
    } catch (err) {
      wx.hideLoading();
      console.error('加载数据失败:', err);
      wx.showToast({ title: '加载数据失败', icon: 'none' });
    }
  },
  
  /**
   * ✅ 渲染时间线
   */
  renderTimeline(data) {
    const { timeline } = data;
    
    // ✅ 处理 aiResponse，提取 reminder 字段
    if (timeline && timeline.length > 0) {
      timeline.forEach(day => {
        if (day.records && day.records.length > 0) {
          day.records.forEach(record => {
            if (record.aiResponse) {
              try {
                const parsed = JSON.parse(record.aiResponse);
                record.aiReminder = parsed.data?.ai_comment || parsed.data?.reminder || '';
              } catch (e) {
                console.warn('AI 回复解析失败:', e);
                record.aiReminder = record.aiResponse;
              }
            }
            record.images = this.normalizeRecordImages(record);
          });
        }
      });
    }
    
    this.setData({
      timeline: timeline || []
    });
    console.log('时间线已渲染');
  },

  normalizeRecordImages(record) {
    const ids = record.imgId || [];
    if (ids.length > 0) {
      return ids.map(id => `${FILE_VIEW_BASE_URL}${id}`);
    }
    return record.imageUrls || [];
  },

  openTimelineDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      wx.showToast({ title: '记录不存在', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: `/subpage1/pages/reminisce/detail?id=${id}&source=friend`
    });
  },

  buildDailyAnalysis(timeline) {
    const records = [];
    timeline.forEach(day => {
      (day.records || []).forEach(record => {
        records.push({ ...record, date: day.date });
      });
    });

    if (records.length === 0) {
      return {
        title: '今日共鸣分析',
        summary: '最近 7 天还没有可分析的共享记录。',
        meCount: 0,
        friendCount: 0,
        sameDayCount: 0,
        updatedAt: '每日 0 点更新'
      };
    }

    const meRecords = records.filter(record => record.owner === 'me');
    const friendRecords = records.filter(record => record.owner === 'friend');
    const days = timeline.map(day => day.date);
    const sameDayCount = days.filter(date => {
      const dayRecords = records.filter(record => record.date === date);
      return dayRecords.some(record => record.owner === 'me') && dayRecords.some(record => record.owner === 'friend');
    }).length;

    const latestDate = timeline[0] && timeline[0].date;
    const latestRecords = latestDate ? records.filter(record => record.date === latestDate) : records;
    const latestMe = latestRecords.some(record => record.owner === 'me');
    const latestFriend = latestRecords.some(record => record.owner === 'friend');

    let summary = '';
    if (latestMe && latestFriend) {
      summary = '你们最近都留下了情绪记录，适合用一句轻量问候开启连接。';
    } else if (latestMe) {
      summary = `${this.data.friendNickname || '好友'}最近记录较少，可以把你的状态温和同步给对方。`;
    } else if (latestFriend) {
      summary = `${this.data.friendNickname || '好友'}最近有新的情绪记录，适合先回应感受，不急着给建议。`;
    } else if (sameDayCount > 0) {
      summary = `最近 7 天有 ${sameDayCount} 天你们都在记录，连接感正在形成。`;
    } else {
      summary = '最近记录节奏不同步，可以先从一次简单分享开始。';
    }

    return {
      title: '今日共鸣分析',
      summary,
      meCount: meRecords.length,
      friendCount: friendRecords.length,
      sameDayCount,
      updatedAt: '每日 0 点更新'
    };
  },

  copyCode() {
    if (!this.data.shareCode) return;
    const handler = async () => {
      await new Promise((resolve, reject) => {
        wx.setClipboardData({
          data: this.data.shareCode,
          success: () => {
            wx.showToast({ title: '复制成功', icon: 'success' });
            resolve();
          },
          fail: reject
        });
      });
    };
    withAntiDoubleClick(handler, this, 'isCopyingCode')();
  },

  async handleBind() {
    if (!this.data.friendCode) {
      wx.showToast({ title: '请输入好友共享码', icon: 'none' });
      return;
    }
    
    const handler = async () => {
      wx.showLoading({ title: '绑定中...' });
      try {
        const res = await requestWithLogin({
          url: `/api/friend/bind?code=${this.data.friendCode}`,
          method: 'POST'
        });
        console.log('绑定响应:', res);
        wx.hideLoading();
        if (res.code === 200 || res.code === "0" || res.success === true) {
          wx.showToast({ title: '绑定成功', icon: 'success' });
          this.setData({ isBound: true });
          setTimeout(() => {
            this.loadAllData();
          }, 500);
        } else {
          wx.showToast({ title: res.message || res.msg || '绑定失败', icon: 'none' });
        }
      } catch (err) {
        wx.hideLoading();
        console.error('绑定异常:', err);
        wx.showToast({ title: err.message || '绑定失败', icon: 'none' });
        throw err;
      }
    };
    withAntiDoubleClick(handler, this, 'isBindingFriend')();
  }
});
