// app.js - 全局入口
const { request, loginWithWechat } = require('./utils/request');

App({
  globalData: {
    userInfo: null,
    token: '',
    userId: '',
    constellation: '',       // 用户星座
    nickname: '',             // 用户昵称
    constellationUpdated: false,  // 星座更新标志
    // ✅ 通知未读数
    unreadCount: undefined,        // 总未读数
    dailyBonusUnread: 0,           // 每日补给未读数
    horoscopeUnread: 0             // 星座运势未读数
  },

  onLaunch() {
    console.log('App Launch');
    console.log('[App] globalData 初始化:', this.globalData);
    this.checkLogin();
  },

  onShow() {
    console.log('App Show');
  },

  onHide() {
    console.log('App Hide');
  },

  // 检查登录状态
  checkLogin() {
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.globalData.userId = wx.getStorageSync('id');
    } else {
      this.doLogin();
    }
  },

  // 执行登录
  doLogin() {
    loginWithWechat().then(token => {
      this.globalData.token = token;
      this.globalData.userId = wx.getStorageSync('id');
      console.log('登录成功，token:', token);
      
      // ✅ 新增：登录成功后，立即获取用户资料
      this.loadUserProfile();
    }).catch(err => {
      console.error('登录失败:', err);
    });
  },
  
  // ✅ 新增：获取用户资料并更新全局数据
  loadUserProfile() {
    request({
      url: '/user/api/v1/profile',
      method: 'GET'
    }).then(res => {
      if (res.data) {
        console.log('[App] 获取用户资料成功:', res.data);
        const nickname = res.data.nickname || '';
        const constellation = res.data.constellation || '';
        
        // 更新全局数据
        this.globalData.nickname = nickname;
        this.globalData.constellation = constellation;
        this.globalData.constellationUpdated = true;
        
        console.log('[App] 已更新全局用户资料 - 昵称:', nickname, '星座:', constellation);
        
        // 同步到本地缓存
        const existingUserInfo = wx.getStorageSync('userInfo') || {};
        wx.setStorageSync('userInfo', {
          ...existingUserInfo,
          nickname: nickname,
          constellation: constellation
        });
      }
    }).catch(err => {
      console.error('[App] 获取用户资料失败:', err);
    });
  }
});
