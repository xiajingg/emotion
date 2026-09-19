// subpage1/pages/reminisce/index.js
const { request } = require('../../../utils/request');
const { withAntiDoubleClick } = require('../../../utils/util');
const FILE_VIEW_BASE_URL = 'https://www.onekey-ai.top/files/view?id=';

Page({
  data: {
    // 页面状态
    isFirstVisit: false,
    hasData: false,
    showGuide: false,  // 引导弹窗，需要等接口返回后再决定是否显示
    
    // 数据概览
    totalTimes: 0,
    
    // 动画显示的值
    displayTotalTimes: 0,
    
    // 使用次数（推广期放在"我的"页面）
    remainingUses: 0,
    totalUses: 0,
    
    // 历史记录
    steps: [],
    page: 1,
    size: 20,
    pages: 0,
    total: 0,  // 总条数
    hasMore: true,
    
    // 图片预览
    showPreview: false,
    previewImages: [],
    
    showScrollHint: true,  // 显示下滑提示
    
    // 动画定时器管理
    _animTimers: [],
    
    // 用户资料
    nickname: '',
    constellation: '',
    constellationSymbol: '⭐',  // 新增：预计算的星座符号
    isProfileLoaded: false,  // 新增：用户资料加载状态标志
    
    // 弹窗控制
    showEditModal: false,
    showConstellationPicker: false,
    tempNickname: '',
    tempConstellation: '',
    
    // 星座选项
    constellationOptions: [
      '白羊座', '金牛座', '双子座', '巨蟹座', '狮子座', '处女座',
      '天秤座', '天蝎座', '射手座', '摩羯座', '水瓶座', '双鱼座'
    ],
    
    // 防重复点击标志
    isSavingNickname: false,
    tapLock: ''
  },

  onLoad(options) {
    // 检查首次访问
    const visited = wx.getStorageSync('visited');
    this.setData({ isFirstVisit: !visited });
    
    // 先显示空数据
    this.setData({ displayTotalTimes: 0 });
    
    // 加载数据
    this.loadData();
    
    // 加载用户资料
    this.loadUserProfile();

    // 🔑 处理自动打开星座设置弹窗的参数
    if (options && options.autoOpenConstellation === '1') {
      // 等待用户资料加载完成后，自动打开星座编辑弹窗
      this._pendingAutoOpenConstellation = true;
      // 记录是否需要在设置完成后跳转至星座运势页
      this._pendingNavigateToHoroscope = options.navigateToHoroscope === '1';
    }
  },

  onShow() {
    // 只有从其他页面返回时才刷新（避免首次加载时重复调用）
    if (this._hasLoaded) {
      this.setData({ page: 1, steps: [] });
      this.loadData();
    }
    
    // ===== 已禁用：刷新使用次数（推广期无限使用，2026-05-21）=====
    // this.getRemainingUses();
  },

  // 自动打开星座设置弹窗（供 loadUserProfile 回调调用）
  autoOpenConstellationModal() {
    if (this._pendingAutoOpenConstellation) {
      this._pendingAutoOpenConstellation = false;
      // 延迟打开，确保用户资料已完全加载并渲染
      setTimeout(() => {
        this.editConstellation();
      }, 500);
    }
  },

  // 页面滚动监听 - 控制下滑提示显示/隐藏
  onPageScroll(e) {
    // 滑到顶部（scrollTop <= 10）重新显示提示
    if (e.scrollTop <= 10) {
      if (!this.data.showScrollHint) {
        this.setData({ showScrollHint: true });
      }
    } 
    // 下滑超过 50px 隐藏提示
    else if (e.scrollTop > 50 && this.data.showScrollHint) {
      this.setData({ showScrollHint: false });
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 1, steps: [] });
    Promise.all([
      this.loadOverview(),
      this.loadSteps()
    ]).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (this.data.hasMore) {
      this.setData({ page: this.data.page + 1 });
      this.loadSteps();
    }
  },

  // 加载所有数据
  loadData() {
    // 防止重复加载
    if (this._isLoading) return;
    this._isLoading = true;
    this._hasLoaded = true;
    
    Promise.all([
      this.loadOverview(),
      this.loadSteps()
    ]).then(() => {
      // 所有数据加载完成后，根据是否有数据决定是否显示引导
      if (!this.data.hasData && this.data.isFirstVisit) {
        this.setData({ showGuide: true });
      }
    }).finally(() => {
      this._isLoading = false;
    });
  },

  // 加载数据概览
  loadOverview() {
    return request({
      url: '/user/api/v1/getTiAnalysisData',
      method: 'GET'
    }).then(res => {
      const data = res.data || {};
      const totalCount = data.totalCount || 0;
      
      this.setData({
        totalTimes: totalCount,
        hasData: totalCount > 0
      });
      
      this.animateNumber('displayTotalTimes', totalCount, 800);
    }).catch(err => {
      console.error('[reminisce] 获取数据概览失败:', err);
    });
  },

  // 数字动画
  animateNumber(key, target, duration) {
    const start = this.data[key] || 0;
    const change = target - start;
    if (change === 0) return;

    const startTime = Date.now();
    // ✅ 关键修复：增加帧间隔从16ms到32ms，减少setData频率，避免真机报错
    const frameDuration = 32; // 约30fps，平衡性能和流畅度
    
    const step = () => {
      // 关键修复：检查页面实例是否存在，防止在页面销毁后继续 setData
      if (!this.setData || !this.data) return;

      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      // 缓动函数
      const easeProgress = 1 - Math.pow(1 - progress, 3);
      const current = Math.round(start + change * easeProgress);
      
      this.setData({ [key]: current });
      
      if (progress < 1) {
        const timer = setTimeout(step, frameDuration);
        this.data._animTimers.push(timer);
      }
    };
    
    const timer = setTimeout(step, frameDuration);
    this.data._animTimers.push(timer);
  },

  // 加载历史记录
  loadSteps() {
    return request({
      url: `/user/api/v1/historySubmit?page=${this.data.page}&size=${this.data.size}`,
      method: 'GET'
    }).then(res => {
      const newSteps = (res.data.records || []).map(item => this.normalizeHistoryStep(item));
      
      const allSteps = this.data.page === 1 ? newSteps : [...this.data.steps, ...newSteps];
      
      this.setData({
        steps: allSteps,
        pages: res.data.pages || 0,
        total: res.data.total || 0,  // 保存总条数
        hasMore: this.data.page < (res.data.pages || 0),
        hasData: allSteps.length > 0
      });
    }).catch(err => {
      console.error('[reminisce] 获取历史记录失败:', err);
    });
  },

  normalizeHistoryStep(item) {
    const imageUrls = this.normalizeHistoryImages(item);
    const isDoodle = !!item.doodle;
    const inputText = item.inputText || '';
    const doodleImage = imageUrls[0] || (isDoodle && this.isImageUrl(inputText) ? inputText : '');
    const images = isDoodle
      ? (doodleImage ? [doodleImage] : imageUrls)
      : imageUrls;

    return {
      id: item.id,
      title: (item.emotion || (isDoodle ? '涂鸦记录' : '心情记录')) + ' · ' + (item.createTime || ''),
      inputText: isDoodle ? '' : inputText,
      rawInputText: inputText,
      emotion: item.emotion || '',
      reminder: item.reminder || item.aiComment || '',
      images,
      doodle: isDoodle,
      viewHint: item.viewHint || '点击查看完整分析',
      finished: true
    };
  },

  isImageUrl(value) {
    if (!value || typeof value !== 'string') return false;
    return /^https?:\/\//i.test(value);
  },

  normalizeHistoryImages(item) {
    const ids = item.imgId || [];
    if (ids.length > 0) {
      return ids.map(id => `${FILE_VIEW_BASE_URL}${id}`);
    }
    return item.imageUrls || [];
  },

  // 开始记录
  startRecord() {
    if (this.data.tapLock) return;
    this.setData({ tapLock: 'record' });
    wx.setStorageSync('visited', true);
    this.setData({ 
      isFirstVisit: false,
      showGuide: false
    });
    wx.navigateTo({
      url: '/pages/index/index',
      complete: () => this.releaseTapLock()
    });
  },

  // 关闭引导
  closeGuide() {
    this.setData({ showGuide: false });
  },

  // 跳转记录页
  goToRecord() {
    if (this.data.tapLock) return;
    this.setData({ tapLock: 'record' });
    wx.navigateTo({
      url: '/pages/index/index',
      complete: () => this.releaseTapLock()
    });
  },

  // 跳转情绪周报
  goToWeeklyReport() {
    if (this.data.tapLock) return;
    this.setData({ tapLock: 'weekly' });
    wx.navigateTo({
      url: '/pages/weekly-report/weekly-report',
      complete: () => this.releaseTapLock()
    });
  },

  // 跳转到星座运势页面
  goToHoroscope() {
    if (this.data.tapLock) return;
    // 🔑 前置校验：检查用户是否已设置星座
    const constellation = this.data.constellation;
    
    if (!constellation) {
      // 未设置星座，弹出提示并提供去设置选项
      wx.showModal({
        title: '提示',
        content: '请先设置您的星座，以便查看专属运势',
        confirmText: '去设置',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            // 用户点击“去设置”，打开星座编辑弹窗
            this.editConstellation();
          }
        }
      });
      return;
    }
    
    // 已设置星座，正常跳转至运势详情页
    this.setData({ tapLock: 'horoscope' });
    wx.navigateTo({ 
      url: '/subpage1/pages/horoscope/index',
      complete: () => this.releaseTapLock()
    });
  },

  // 图片预览
  onStepImagePreview(e) {
    const { images } = e.detail;
    // 安全检查：确保页面仍然活跃
    if (!this.setData) return;
    this.setData({
      showPreview: true,
      previewImages: images || []
    });
  },

  onStepViewDetail(e) {
    const { id } = e.detail || {};
    if (!id) {
      wx.showToast({ title: '记录不存在', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: `/subpage1/pages/reminisce/detail?id=${id}`
    });
  },

  hidePreview() {
    // 安全检查：确保页面仍然活跃
    if (!this.setData) return;
    this.setData({ showPreview: false });
  },

  // ---- 心情趋势折线图相关方法 ----

  onHide() {
    // 页面隐藏时清除所有动画定时器，防止后台执行 setData
    this.data._animTimers.forEach(timer => clearTimeout(timer));
    this.data._animTimers = [];
  },

  onUnload() {
    // 页面卸载时彻底清理
    this.data._animTimers.forEach(timer => clearTimeout(timer));
    this.data._animTimers = [];
    
    // 清理预览状态，防止页面销毁后仍有异步操作
    this.setData({
      showPreview: false,
      previewImages: []
    });
  },

  // 分享给朋友
  onShareAppMessage() {
    return {
      title: '心情回顾与情绪趋势 - 记录压力变化',
      path: '/subpage1/pages/reminisce/index',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: '心情回顾与情绪趋势，追踪压力和焦虑变化',
      query: '',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // ---- 用户资料相关方法 ----

  // 加载用户资料
  loadUserProfile() {
    return request({
      url: '/user/api/v1/profile',
      method: 'GET'
    }).then(res => {
      if (res.data) {
        console.log('[用户资料] 后端返回数据:', res.data);
        const nickname = res.data.nickname || '';
        const constellation = res.data.constellation || '';
        const constellationSymbol = this.getConstellationSymbol(constellation);
        
        console.log('[用户资料] 设置后 - 昵称:', nickname, '星座:', constellation, '符号:', constellationSymbol);
        
        // ✅ 更新全局数据
        const app = getApp();
        if (app && app.globalData) {
          app.globalData.nickname = nickname;
          app.globalData.constellation = constellation;
          app.globalData.constellationUpdated = true; // 标记已更新
          console.log('[用户资料] 已更新全局数据');
        }
        
        // 先更新数据，再设置加载状态触发淡入动画
        this.setData({
          nickname: nickname,
          constellation: constellation,
          constellationSymbol: constellationSymbol
        }, () => {
          // 数据更新完成后，触发淡入动画
          setTimeout(() => {
            this.setData({ isProfileLoaded: true });
            // 🔑 如果设置了自动打开星座弹窗标志，则在此处触发
            this.autoOpenConstellationModal();
          }, 50);  // 短暂延迟确保DOM已渲染
        });
        
        // 🔑 关键修复：同步更新本地存储的 userInfo，供其他页面使用
        const existingUserInfo = wx.getStorageSync('userInfo') || {};
        wx.setStorageSync('userInfo', {
          ...existingUserInfo,
          nickname: nickname,
          constellation: constellation
        });
        console.log('[用户资料] 已同步到本地存储');
      }
    }).catch(err => {
      console.error('[reminisce] 获取用户资料失败:', err);
      // 即使失败也设置为已加载，避免一直显示占位符
      this.setData({ isProfileLoaded: true });
    });
  },

  // 编辑个人信息（昵称和星座）
  editNickname() {
    if (this.data.tapLock) return;
    this.setData({ tapLock: 'edit' });
    this.setData({
      showEditModal: true,
      tempNickname: this.data.nickname,
      tempConstellation: this.data.constellation,
      showConstellationPicker: false
    });
    this.releaseTapLock();
  },

  // 关闭编辑弹窗
  closeEditModal() {
    this.setData({ 
      showEditModal: false,
      showConstellationPicker: false
    });
  },

  // 显示星座选择器
  showConstellationPicker() {
    this.setData({
      showConstellationPicker: !this.data.showConstellationPicker
    });
  },

  // 昵称输入
  onNicknameInput(e) {
    this.setData({ tempNickname: e.detail.value });
  },

  // 选择星座
  selectConstellation(e) {
    this.setData({
      tempConstellation: e.currentTarget.dataset.constellation,
      showConstellationPicker: false  // 选择后自动关闭下拉框
    });
  },

  // 保存个人信息（昵称和星座）
  saveProfile() {
    const nickname = this.data.tempNickname.trim();
    const constellation = this.data.tempConstellation;
    
    if (!nickname) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' });
      return;
    }
    
    if (nickname.length > 50) {
      wx.showToast({ title: '昵称不能超过50个字符', icon: 'none' });
      return;
    }
    
    if (!constellation) {
      wx.showToast({ title: '请选择星座', icon: 'none' });
      return;
    }
    
    // 使用防重复点击装饰器包装
    const handler = async () => {
      try {
        wx.showLoading({ title: '保存中...' });
        
        // 同时更新昵称和星座
        await request({
          url: '/user/api/v1/profile/update',
          method: 'POST',
          data: { 
            nickname: nickname,
            constellation: constellation
          }
        });
        
        wx.hideLoading();
        wx.showToast({ title: '保存成功', icon: 'success' });
        
        // 预计算星座符号
        const newSymbol = this.getConstellationSymbol(constellation);
        
        // 同时更新昵称、星座和符号
        this.setData({
          nickname: nickname,
          constellation: constellation,
          constellationSymbol: newSymbol,
          showEditModal: false,
          showConstellationPicker: false
        }, () => {
          // 🔑 关键修复：同步更新本地存储的 userInfo
          const existingUserInfo = wx.getStorageSync('userInfo') || {};
          wx.setStorageSync('userInfo', {
            ...existingUserInfo,
            nickname: nickname,
            constellation: constellation
          });
          console.log('[保存个人信息] 已同步到本地存储');
          
          // 🔑 通知全局刷新星座信息（用于 TabBar 组件更新）
          const app = getApp();
          if (app && app.globalData) {
            app.globalData.constellationUpdated = true;
            app.globalData.constellation = constellation;
            app.globalData.nickname = nickname;
          }
        });
      } catch (err) {
        wx.hideLoading();
        wx.showToast({ title: err.message || '保存失败', icon: 'none' });
        throw err;
      }
    };

    // 应用防重复点击
    withAntiDoubleClick(handler, this, 'isSavingNickname')();
  },

  // 编辑星座（复用统一编辑弹窗）
  editConstellation() {
    this.setData({
      showEditModal: true,
      tempNickname: this.data.nickname,
      tempConstellation: this.data.constellation,
      showConstellationPicker: false
    });
  },

  releaseTapLock() {
    setTimeout(() => {
      this.setData({ tapLock: '' });
    }, 600);
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止点击内容区域时关闭弹窗
  },

  // ---- MVP 重构新增方法 ----

  // 获取星座符号
  getConstellationSymbol(constellation) {
    if (!constellation) {
      return '\u2B50'; // ⭐
    }
    
    // 使用 Unicode 转义序列确保兼容性
    const symbolMap = {
      '白羊座': '\u2648', // ♈
      '金牛座': '\u2649', // ♉
      '双子座': '\u264A', // ♊
      '巨蟹座': '\u264B', // ♋
      '狮子座': '\u264C', // ♌
      '处女座': '\u264D', // ♍
      '天秤座': '\u264E', // ♎
      '天蝎座': '\u264F', // ♏
      '射手座': '\u2650', // ♐
      '摩羯座': '\u2651', // ♑
      '水瓶座': '\u2652', // ♒
      '双鱼座': '\u2653'  // ♓
    };
    
    const symbol = symbolMap[constellation];
    console.log('[星座符号] 星座:', constellation, '→ 符号:', symbol || '\u2B50');
    return symbol || '\u2B50'; // ⭐
  },

  // ---- 使用次数相关方法 ----

  // ===== 已禁用：获取剩余次数（推广期无限使用，2026-05-21）=====
  // getRemainingUses() {
  //   request({
  //     url: '/user/getRemaining',
  //     method: 'GET'
  //   }).then(res => {
  //     this.setData({
  //       remainingUses: res.data.daily || 0,
  //       totalUses: res.data.total || 0
  //     });
  //   }).catch(err => {
  //     console.error('获取剩余次数失败:', err);
  //   });
  // },

  // ===== 已禁用：支付功能（2026-05-21）=====
  // goToPayment() {
  //   wx.showModal({
  //     title: '提示',
  //     content: '观看广告视频可获取3次分析机会',
  //     confirmText: '确定',
  //     cancelText: '取消',
  //     success: (res) => {
  //       if (res.confirm) {
  //         // 调用广告
  //         if (wx.createRewardedVideoAd) {
  //           const rewardedAd = wx.createRewardedVideoAd({
  //             adUnitId: 'adunit-1ff3406970f5b729'
  //           });
  //           
  //           rewardedAd.onLoad(() => {});
  //           rewardedAd.onError((err) => {
  //             console.error('广告加载失败', err);
  //           });
  //           rewardedAd.onClose((res) => {
  //             if (res && res.isEnded) {
  //               this.grantReward();
  //             } else {
  //               wx.showToast({ title: '未完整观看广告', icon: 'none', duration: 2000 });
  //             }
  //           });
  //           
  //           rewardedAd.show().catch(err => {
  //             console.error('广告显示失败:', err);
  //           });
  //         } else {
  //           wx.showToast({ title: '广告未准备好', icon: 'none' });
  //         }
  //       }
  //     }
  //   });
  // },

  // 发放广告奖励
  grantReward() {
    request({
      url: '/user/grantReward?param=2222',
      method: 'GET'
    }).then(() => {
      wx.showToast({ title: '观看完成，次数已增加3次', icon: 'success', duration: 2000 });
      // ===== 已禁用：刷新使用次数（推广期无限使用，2026-05-21）=====
      // this.getRemainingUses();
    }).catch(err => {
      console.error('发放奖励失败:', err);
    });
  }
});
