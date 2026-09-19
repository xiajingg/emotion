// components/tab-bar/index.js
const { request } = require('../../utils/request');

Component({
  properties: {
    currentPage: {
      type: String,
      value: 'index'
    }
  },

  data: {
    showFunctionMenu: false,  // 控制功能菜单显示
    userConstellation: '',     // 用户星座
    unreadCount: 0,             // ✅ 未读消息总数
    dailyBonusUnread: 0,        // ✅ 每日补给未读数
    horoscopeUnread: 0,          // ✅ 星座运势未读数
    tapLock: ''
  },

  // 组件加载时获取用户信息
  attached() {
    this.loadUserInfo();
    this.loadUnreadCount();  // ✅ 加载未读数
  },

  // 每次显示时刷新用户信息
  pageLifetimes: {
    show() {
      this.loadUserInfo();
      this.loadUnreadCount();  // ✅ 每次显示时刷新未读数
      
      // 🔑 检查是否需要从 globalData 刷新星座信息
      const app = getApp();
      if (app && app.globalData && app.globalData.constellationUpdated) {
        // 使用 globalData 中的最新数据
        this.setData({
          userConstellation: app.globalData.constellation || ''
        });
        
        // 同时更新本地缓存
        const existingUserInfo = wx.getStorageSync('userInfo') || {};
        wx.setStorageSync('userInfo', {
          ...existingUserInfo,
          constellation: app.globalData.constellation,
          nickname: app.globalData.nickname
        });
        
        // 清除标志,避免重复刷新
        app.globalData.constellationUpdated = false;
      }
    }
  },

  methods: {
    // 加载用户信息
    loadUserInfo() {
      console.log('[TabBar] loadUserInfo 被调用');
      
      // ✅ 优先使用全局数据（如果“我的”页面已经加载过）
      const app = getApp();
      if (app && app.globalData && app.globalData.constellation) {
        console.log('[TabBar] 使用全局星座数据:', app.globalData.constellation);
        this.setData({ userConstellation: app.globalData.constellation });
        return; // 已有全局数据，不需要再请求
      }
      
      console.log('[TabBar] globalData 没有星座，发起请求');
      // 否则发起请求
      request({
        url: '/user/api/v1/profile',
        method: 'GET'
      }).then(res => {
        if (res.data) {
          const constellation = res.data.constellation || '';
          this.setData({ userConstellation: constellation });
          
          // ✅ 同步更新全局数据
          if (app && app.globalData) {
            app.globalData.constellation = constellation;
            app.globalData.nickname = res.data.nickname || '';
          }
          
          // 同步更新本地缓存
          const existingUserInfo = wx.getStorageSync('userInfo') || {};
          wx.setStorageSync('userInfo', {
            ...existingUserInfo,
            constellation: constellation,
            nickname: res.data.nickname || ''
          });
        }
      }).catch(err => {
        console.error('[TabBar] 获取用户信息失败:', err);
      });
    },
    
    onIndexTap() {
      if (this.data.currentPage === 'index' || this.data.tapLock) return;
      this.setData({ tapLock: 'index' });
      wx.reLaunch({
        url: '/pages/index/index',
        complete: () => this.releaseTapLock()
      });
    },
    
    // ✅ 打开功能菜单
    onFunctionMenuTap() {
      if (this.data.tapLock) return;
      this.setData({ tapLock: 'menu' });
      // 🔑 打开菜单前,从本地缓存获取最新的星座信息
      const userInfo = wx.getStorageSync('userInfo') || {};
      const latestConstellation = userInfo.constellation || '';
      
      // 更新组件数据,确保function-menu能拿到最新的星座
      this.setData({ 
        showFunctionMenu: true,
        userConstellation: latestConstellation
      });
      this.releaseTapLock();
    },
    
    // ✅ 关闭功能菜单
    onCloseFunctionMenu() {
      this.setData({ showFunctionMenu: false });
    },
    
    // ✅ 选择功能项
    onFunctionSelect(e) {
      if (this.data.tapLock) return;
      this.setData({ tapLock: e.detail.page || 'function' });
      const page = e.detail.page;
      
      // ✅ 根据页面类型标记对应的消息为已读
      if (page === 'daily-bonus') {
        this.markAsRead(1); // 每日补给
      } else if (page === 'horoscope') {
        this.markAsRead(2); // 星座运势
      }
      
      this.setData({ showFunctionMenu: false });
      
      // 延迟跳转，让关闭动画完成
      setTimeout(() => {
        this.navigateToPage(page);
      }, 200);
    },
    
    // ✅ 页面跳转逻辑
    navigateToPage(page) {
      // 🔑 星象解读特殊处理：校验是否已设置星座
      if (page === 'horoscope') {
        // 使用组件中最新的星座信息
        const constellation = this.data.userConstellation;
        
        if (!constellation) {
          wx.showModal({
            title: '提示',
            content: '请先设置您的星座，以便查看专属运势',
            confirmText: '去设置',
            cancelText: '取消',
            success: (res) => {
              if (res.confirm) {
                // 跳转至“我的”页面，并携带自动打开星座设置弹窗及设置后跳转运势页的参数
                wx.reLaunch({ 
                  url: '/subpage1/pages/reminisce/index?autoOpenConstellation=1&navigateToHoroscope=1',
                  complete: () => this.releaseTapLock()
                });
              } else {
                this.releaseTapLock();
              }
            },
            fail: () => {
              this.releaseTapLock();
            }
          });
          return;
        }
      }

      const pageMap = {
        'ranking': '/subpage1/pages/ranking/index',
        'daily-bonus': '/subpage1/pages/daily-bonus/index',
        'friend-share': '/subpage1/pages/friend-share/index',
        'reply-favorites': '/subpage1/pages/reply-favorites/index',
        'answer-book': '/subpage1/pages/answer-book/index',
        'horoscope': '/subpage1/pages/horoscope/index'
      };
      
      const url = pageMap[page];
      if (url) {
        wx.navigateTo({
          url,
          complete: () => this.releaseTapLock()
        });
      } else {
        this.releaseTapLock();
      }
    },
    
    onMineTap() {
      if (this.data.currentPage === 'mine' || this.data.tapLock) return;
      this.setData({ tapLock: 'mine' });
      wx.reLaunch({
        url: '/subpage1/pages/reminisce/index',
        complete: () => this.releaseTapLock()
      });
    },

    releaseTapLock() {
      setTimeout(() => {
        this.setData({ tapLock: '' });
      }, 600);
    },
    
    // ✅ 加载未读消息数
    loadUnreadCount() {
      console.log('[TabBar] loadUnreadCount 被调用');
      
      // ✅ 优先使用全局数据（如果首页已经加载过）
      const app = getApp();
      if (app && app.globalData && app.globalData.unreadCount !== undefined) {
        console.log('[TabBar] 使用全局未读数:', app.globalData.unreadCount);
        this.setData({
          unreadCount: app.globalData.unreadCount,
          dailyBonusUnread: app.globalData.dailyBonusUnread || 0,
          horoscopeUnread: app.globalData.horoscopeUnread || 0
        });
        console.log('[TabBar] setData 完成, unreadCount:', this.data.unreadCount);
        return; // 已有全局数据，不需要再请求
      }
      
      console.log('[TabBar] globalData 不存在，发起请求');
      // 否则发起请求
      request({
        url: '/user/api/v1/notification/unread-count',
        method: 'GET'
      }).then(res => {
        if (res.data) {
          console.log('[TabBar] 接口返回:', res.data);
          this.setData({
            unreadCount: res.data.totalCount || 0,
            dailyBonusUnread: res.data.dailyBonusUnread || 0,
            horoscopeUnread: res.data.horoscopeUnread || 0
          });
          console.log('[TabBar] setData 完成, unreadCount:', this.data.unreadCount);
          // ✅ 同步到全局数据
          if (app && app.globalData) {
            app.globalData.unreadCount = res.data.totalCount || 0;
            app.globalData.dailyBonusUnread = res.data.dailyBonusUnread || 0;
            app.globalData.horoscopeUnread = res.data.horoscopeUnread || 0;
          }
        }
      }).catch(err => {
        console.error('[TabBar] 获取未读数失败:', err);
      });
    },
    
    // ✅ 标记消息为已读
    markAsRead(notificationType) {
      request({
        url: '/user/api/v1/notification/mark-read',
        method: 'POST',
        data: { notificationType }
      }).then(res => {
        if (res.success) {
          // 重新加载未读数,更新红点
          this.loadUnreadCount();
        }
      }).catch(err => {
        console.error('[TabBar] 标记已读失败:', err);
      });
    }
  }
});
