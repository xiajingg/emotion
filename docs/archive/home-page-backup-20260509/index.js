// pages/home/index.js
const { request } = require('../../utils/request');
const { typeWriter, withAntiDoubleClick } = require('../../utils/util');

Page({
  data: {
    motivation: '',
    likeCount: 0,
    dislikeCount: 0,
    isFeedbackSubmitting: false  // 防重复点击标志
  },

  onLoad() {
    this._motivLoading = false;
    this.getMotivation();
  },

  onShow() {
    // 已加载过则不重复调用，避免多次请求
  },

  // 获取今日心语（打字机效果）
  getMotivation() {
    // 防止重复请求
    if (this._motivLoading) return;
    this._motivLoading = true;

    request({
      url: '/daily-motivation/api/v1/getMotivation',
      method: 'GET'
    }).then(res => {
      console.log('心语数据:', res);
      this.setData({
        likeCount: res.data.totalLikes || 0,
        dislikeCount: res.data.totalDislikes || 0
      });
      // 直接显示心语（避免打字机效果导致的渲染层警告）
      this.setData({ motivation: res.data.motivationContent || '今天也要开心哦～' });
      // 请求完成后重置加载标志
      this._motivLoading = false;
    }).catch(err => {
      // 401 未登录
      if (err.message === '401') {
        wx.showToast({ title: '请先登录', icon: 'none', duration: 2000 });
        require('../../utils/request').loginWithWechat().then(() => {
          // 登录成功后重新获取，先重置加载标志
          this._motivLoading = false;
          this.getMotivation();
        }).catch(() => {
          // 登录失败，重置加载标志
          this._motivLoading = false;
        });
      } else {
        console.error('获取心语失败:', err);
        // 其他错误也重置加载标志
        this._motivLoading = false;
      }
    });
  },

  // 打字机效果
  typeWriter(text, speed = 50) {
    let i = 0;
    const timer = setInterval(() => {
      // 检查组件是否还在
      if (!this || !this.data) {
        clearInterval(timer);
        return;
      }
      
      if (i < text.length) {
        this.setData({ motivation: this.data.motivation + text[i] });
        i++;
      } else {
        clearInterval(timer);
      }
    }, speed);
    
    // 保存定时器ID用于清理
    this._typeTimer = timer;
  },

  // 心语反馈（点赞/点踩）
  onFeedback(e) {
    const type = e.currentTarget.dataset.type;
    
    // 使用防重复点击装饰器包装
    const handler = async () => {
      try {
        const res = await request({
          url: `/daily-motivation/api/v1/motivationFeedback?feedbackType=${type}`,
          method: 'GET'
        });
        
        if (res.code === '500') {
          wx.showToast({ title: res.msg, icon: 'none' });
          return;
        }
        
        if (type === '1') {
          this.setData({ likeCount: this.data.likeCount + 1 });
        } else {
          this.setData({ dislikeCount: this.data.dislikeCount + 1 });
        }
      } catch (err) {
        console.error('反馈失败:', err);
        throw err;
      }
    };
    
    // 应用防重复点击
    withAntiDoubleClick(handler, this, 'isFeedbackSubmitting')(e);
  },

  // 跳转统计页面
  goToMain() {
    // ✅ 跳转到记录页面，用户可立即使用核心功能
    wx.reLaunch({ url: '/pages/index/index' });
  },

  onUnload() {
    // 清除定时器
    if (this._typeTimer) {
      clearInterval(this._typeTimer);
      this._typeTimer = null;
    }
  },

  // 分享给朋友
  onShareAppMessage() {
    return {
      title: '心情日记 - 今日心语',
      path: '/pages/home/index',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: '心情日记 - 今日心语',
      query: '',
      imageUrl: '' // 可以设置自定义分享图片
    };
  }
});
