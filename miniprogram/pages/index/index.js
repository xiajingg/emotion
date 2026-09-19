// pages/index/index.js
const { request, loginWithWechat, requestWithLogin, uploadFileWithLogin } = require('../../utils/request');
const { withAntiDoubleClick } = require('../../utils/util');

const SCENE_TAGS = [
  { key: 'boss_criticism', scene: '被领导批评', text: '被领导批评', taskType: 'REPLY_RESCUE', draft: '领导刚刚批评了我，我有点委屈又不知道怎么回复，想说清楚但不想把关系弄僵。' },
  { key: 'colleague_blame', scene: '同事甩锅', text: '同事甩锅', taskType: 'REPLY_RESCUE', draft: '同事把问题推到我这里，我想回应得有边界，也想保留后续协作空间。' },
  { key: 'partner_cold', scene: '对方冷淡', text: '对方冷淡', taskType: 'REPLY_RESCUE', draft: '对方突然变冷淡，我很不安，想表达感受但不想显得逼迫或卑微。' },
  { key: 'friend_boundary', scene: '朋友越界', text: '朋友越界', taskType: 'REPLY_RESCUE', draft: '朋友的要求让我不舒服，我想拒绝或说明边界，但又不想把话说得太重。' },
  { key: 'family_pressure', scene: '家人催促', text: '家人催促', taskType: 'REPLY_RESCUE', draft: '家人的催促让我压力很大，我想回应得尊重一点，也让对方知道我的边界。' },
  { key: 'sleep_loop', scene: '睡前内耗', text: '睡前停不下来', taskType: 'SLEEP_RUMINATION', draft: '睡前脑子停不下来，一直反复想今天发生的事，想先把心放下来。' },
  { key: 'pressure_breakdown', scene: '压力急救', text: '现在快崩了', taskType: 'PRESSURE_RESCUE', draft: '我现在快被压力压住了，情绪很满，想先撑过这几分钟。' }
];

const REPLY_TABS = [
  { key: 0, style: 'safe', icon: '💬', text: '稳妥表达' },
  { key: 1, style: 'firm', icon: '🧱', text: '坚定边界' },
  { key: 2, style: 'gentle', icon: '🌸', text: '温柔缓和' },
  { key: 3, style: 'short', icon: '⚡', text: '短句版' }
];

const PLACEHOLDERS = [
  '把对方那句话贴进来...',
  '现在卡住的事是什么？',
  '先写一句，不急着回复...'
];

const SCENE_PLACEHOLDERS = {
  '被领导批评': '贴领导原话，或改成你的处境...',
  '同事甩锅': '贴同事原话，或改成你的处境...',
  '对方冷淡': '贴对方原话，或写现在的关系状态...',
  '朋友越界': '写下让你不舒服的要求...',
  '家人催促': '写下家人说了什么...',
  '睡前内耗': '写下今晚反复想的事...',
  '压力急救': '写下此刻最压住你的事...'
};

const DEFAULT_RELIEF_ACTIONS = [
  '慢慢呼吸30秒',
  '把最担心的事写成一句话',
  '先离开当前刺激源喝口水'
];

const DRAWING_COLORS = [
  { name: '墨黑', value: '#2D3436' },
  { name: '玫红', value: '#EC6F9F' },
  { name: '暖黄', value: '#F6C85F' },
  { name: '湖蓝', value: '#45B7D1' },
  { name: '草绿', value: '#6BCB77' },
  { name: '紫色', value: '#7C5CFF' }
];

const DRAWING_BRUSH_SIZES = [
  { name: '细', value: 4 },
  { name: '中', value: 8 },
  { name: '粗', value: 14 }
];

const DRAWING_MOOD_OPTIONS = ['压力', '烦躁', '委屈', '空空的', '累了'];

const SHARE_THEMES = [
  { key: 'purple', name: '紫色', colorStart: '#0c0c2d', colorMid: '#1a1040', colorEnd: '#2d1b69' },
  { key: 'pink', name: '粉红色', colorStart: '#9D174D', colorMid: '#F472B6', colorEnd: '#FBCFE8' }
];

Page({
  data: {
    inputMode: 'text',
    textData: '',
    isAnalyzing: false,
    today: '',
    loading: false,
    loadingText: '加载中...',
    isVoiceRecording: false,
    voiceRecognizingText: '',
    textareaFocus: false,
    isNavigatingWeeklyReport: false,
    isSharingCard: false,
    isSubscribingWeeklyReport: false,
    copyingReplyKey: '',
    favoritingReplyKey: '',

    // 快速打卡相关
    todayCheckedIn: false,
    consecutiveDays: 0,
    _animTimers: [], // 动画定时器管理
    isSubmittingCheckIn: false,    // 快速打卡防重复点击标志

    // 问候语相关
    greetingText: '',
    nickname: '',

    // 情绪解压卡：常见烦恼灵感 + 回复 Tab
    selectedScene: '',
    selectedSceneKey: '',
    selectedTaskType: 'REPLY_RESCUE',
    selectedScenarioKey: '',
    tagDraftText: '',
    canAnalyze: false,
    sceneTags: SCENE_TAGS,
    tabLabels: REPLY_TABS,

    // 情绪涂鸦 MVP
    drawingColors: DRAWING_COLORS,
    selectedDrawingColor: DRAWING_COLORS[0].value,
    drawingBrushSizes: DRAWING_BRUSH_SIZES,
    selectedDrawingBrushSize: DRAWING_BRUSH_SIZES[1].value,
    drawingMoodOptions: DRAWING_MOOD_OPTIONS,
    selectedDrawingMood: '',
    drawingHasContent: false,
    drawingStrokeCount: 0,

    // 情绪解压卡结果
    showResult: false,
    emotionTag: '',
    aiComment: '',
    painPoint: '',
    stabilizeAction: '',
    dontSay: '',
    nextTimeTip: '',
    interactionId: null,
    reliefActions: DEFAULT_RELIEF_ACTIONS,
    replies: { safe: '', firm: '', gentle: '', shortReply: '', high_eq: '', crazy: '', sarcastic: '' },
    answerBook: '',
    activeTab: 0,

    // 占位符轮播
    placeholderIndex: 0,
    currentPlaceholder: '',  // 场景选中时覆盖轮播占位符，为空时使用轮播
    placeholders: PLACEHOLDERS,
    scenePlaceholders: SCENE_PLACEHOLDERS,
    _placeholderTimer: null,

    // 分析结果数据（用于分享）
    analysisData: {
      text: '',
      scene: '',
      drawingImagePath: '',
      emotionTag: '',
      aiComment: '',
      reliefActions: DEFAULT_RELIEF_ACTIONS,
      interactionId: null,
      taskType: 'REPLY_RESCUE',
      scenarioKey: '',
      painPoint: '',
      stabilizeAction: '',
      dontSay: '',
      nextTimeTip: '',
      highEqReply: '',
      boundaryReply: '',
      gentleReply: '',
      sarcasticReply: '',
      safeReply: '',
      firmReply: '',
      shortReply: '',
      answerBook: ''
    },

    // 情绪解压卡海报
    posterImagePath: '',
    isGeneratingPoster: false,
    selectedShareThemeKey: 'purple',

    // 情绪周报订阅相关
    showSubscribeBtn: true,  // 是否显示订阅按钮
    hasSubscribed: false     // 是否已订阅
  },

  onLoad() {
    // ✅ 已移除：激励视频广告已迁移到"我的"页面
    this.setData({ today: this.getToday() });
    this.updateGreeting(); // 初始化问候语
    this.startPlaceholderRotation(); // 启动占位符轮播
    this.initVoiceRecognition(); // 初始化语音输入
  },

  onReady() {
    this.initDrawingCanvas();
  },

  onShow() {
    this.startPlaceholderRotation(); // 恢复占位符轮播
    this.checkLoginThen(() => {
      // ===== 已禁用：刷新剩余次数（推广期无限使用，2026-05-21）=====
      // this.getRemainingUses();
      this.checkTodayStatus();  // 检查今日打卡状态
      this.updateGreeting(); // 更新问候语
      this.checkSubscriptionStatus(); // 检查订阅状态
      
      // ✅ 确保未读数已加载（如果 globalData 还没有）
      const app = getApp();
      if (app && app.globalData && app.globalData.unreadCount === undefined) {
        console.log('[Index] onShow: globalData 没有未读数，重新加载');
        this.loadUnreadCount();
      }
    });
  },

  onHide() {
    this.stopVoiceInput();
    // 页面隐藏时清理所有定时器
    this.clearAllTimers();
  },

  onUnload() {
    this.stopVoiceInput();
    // 页面卸载时清理所有定时器
    this.clearAllTimers();
  },

  // 初始化微信同声传译语音识别
  initVoiceRecognition() {
    try {
      const plugin = requirePlugin('WechatSI');
      this.voiceManager = plugin.getRecordRecognitionManager();
      this.voiceManager.onStart = () => {
        this.voiceBaseText = this.data.textData || '';
        this.setData({
          isVoiceRecording: true,
          voiceRecognizingText: '',
          textareaFocus: true
        });
      };
      this.voiceManager.onRecognize = (res) => {
        this.applyVoicePartial(res && res.result);
      };
      this.voiceManager.onStop = (res) => {
        this.applyVoiceText(res && res.result, this.voiceBaseText);
        this.voiceBaseText = '';
        this.setData({
          isVoiceRecording: false,
          voiceRecognizingText: ''
        });
      };
      this.voiceManager.onError = (err) => {
        console.error('[Index] 语音识别失败:', err);
        this.voiceBaseText = '';
        this.setData({
          isVoiceRecording: false,
          voiceRecognizingText: ''
        });
        this.showVoiceError(err);
      };
    } catch (err) {
      console.error('[Index] 微信同声传译插件初始化失败:', err);
      this.voiceManager = null;
    }
  },

  toggleVoiceInput() {
    if (!this.voiceManager) {
      wx.showToast({ title: '语音输入暂不可用', icon: 'none' });
      return;
    }
    if (this.data.isVoiceRecording) {
      this.stopVoiceInput();
      return;
    }
    wx.getSetting({
      success: (res) => {
        const recordAuth = res.authSetting['scope.record'];
        if (recordAuth === false) {
          this.openRecordSetting();
          return;
        }
        this.ensureVoiceReady(() => this.startVoiceInput());
      },
      fail: () => {
        this.ensureVoiceReady(() => this.startVoiceInput());
      }
    });
  },

  ensureVoiceReady(callback) {
    this.ensurePrivacyAuthorized(() => {
      this.ensureRecordAuthorized(callback);
    });
  },

  ensurePrivacyAuthorized(callback) {
    if (!wx.getPrivacySetting || !wx.requirePrivacyAuthorize) {
      callback && callback();
      return;
    }

    wx.getPrivacySetting({
      success: (res) => {
        console.log('[Index] 隐私协议状态:', res);
        if (!res.needAuthorization) {
          callback && callback();
          return;
        }

        wx.requirePrivacyAuthorize({
          success: () => {
            callback && callback();
          },
          fail: (err) => {
            console.error('[Index] 隐私协议授权失败:', err);
            wx.showModal({
              title: '需要同意隐私指引',
              content: '语音输入会使用麦克风录音并转成文字，请先同意小程序隐私保护指引。',
              confirmText: '知道了',
              showCancel: false
            });
          }
        });
      },
      fail: (err) => {
        console.warn('[Index] 查询隐私协议状态失败，继续请求录音权限:', err);
        callback && callback();
      }
    });
  },

  ensureRecordAuthorized(callback) {
    wx.getSetting({
      success: (res) => {
        const recordAuth = res.authSetting['scope.record'];
        console.log('[Index] 麦克风授权状态:', recordAuth);
        if (recordAuth === true) {
          callback && callback();
          return;
        }
        if (recordAuth === false) {
          this.openRecordSetting();
          return;
        }
        wx.authorize({
          scope: 'scope.record',
          success: () => {
            setTimeout(() => {
              this.checkRecordAuthorized(callback);
            }, 120);
          },
          fail: (err) => {
            console.error('[Index] 麦克风授权失败:', err);
            if (this.isPrivacyApiBanned(err)) {
              this.showPrivacyApiBannedTip();
              return;
            }
            this.openRecordSetting();
          }
        });
      },
      fail: () => {
        callback && callback();
      }
    });
  },

  checkRecordAuthorized(callback) {
    wx.getSetting({
      success: (res) => {
        const recordAuth = res.authSetting['scope.record'];
        console.log('[Index] 麦克风授权复核:', recordAuth);
        if (recordAuth === true) {
          callback && callback();
          return;
        }
        this.openRecordSetting();
      },
      fail: (err) => {
        console.warn('[Index] 麦克风授权复核失败，尝试启动录音:', err);
        callback && callback();
      }
    });
  },

  startVoiceInput() {
    if (!wx.canIUse('getRecorderManager')) {
      wx.showToast({ title: '当前微信版本不支持录音', icon: 'none' });
      return;
    }
    try {
      this.voiceManager.start({
        duration: 60000,
        lang: 'zh_CN'
      });
    } catch (err) {
      console.error('[Index] 启动语音输入失败:', err);
      wx.showToast({ title: '语音输入启动失败', icon: 'none' });
    }
  },

  showVoiceError(err) {
    const code = err && (err.retcode || err.errCode || err.errno || err.code);
    const msg = err && (err.msg || err.errMsg || err.message);
    let title = '语音识别失败';

    if (code === 10001 || String(msg || '').includes('auth')) {
      title = '请开启麦克风权限';
    } else if (this.isPrivacyApiBanned(err)) {
      title = '语音权限未生效';
    } else if (code === -30001) {
      title = '录音启动失败，请稍后重试';
    } else if (code === 10002 || String(msg || '').includes('network')) {
      title = '网络异常，请重试';
    } else if (code === -30011 || code === -30012 || String(msg || '').includes('no voice')) {
      title = '没听清，再试一次';
    } else if (code) {
      title = `语音失败 ${code}`;
    }

    wx.showToast({
      title,
      icon: 'none',
      duration: 2500
    });
  },

  isPrivacyApiBanned(err) {
    const msg = err && (err.errMsg || err.msg || err.message || '');
    return err && (err.errno === 1025 || String(msg).includes('privacy api banned'));
  },

  showPrivacyApiBannedTip() {
    wx.showModal({
      title: '语音权限未生效',
      content: '当前正式版还不能调用麦克风录音接口，请检查小程序后台的用户隐私保护指引是否已声明录音/麦克风用途，并重新提审发布。',
      confirmText: '知道了',
      showCancel: false
    });
  },

  openRecordSetting() {
    wx.showModal({
      title: '需要麦克风权限',
      content: '请在小程序设置里开启麦克风；如果没有这个选项，请到手机系统设置里允许微信使用麦克风。',
      confirmText: '去设置',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          wx.openSetting();
        }
      }
    });
  },

  stopVoiceInput() {
    if (this.voiceManager && this.data.isVoiceRecording) {
      this.voiceManager.stop();
    }
  },

  applyVoicePartial(text) {
    const partialText = (text || '').trim();
    if (!partialText) return;

    const nextText = this.mergeVoiceText(this.voiceBaseText || '', partialText);
    this.setData({
      textData: nextText,
      voiceRecognizingText: partialText,
      canAnalyze: nextText.trim().length >= 5
    });
  },

  applyVoiceText(text, baseText) {
    const voiceText = (text || this.data.voiceRecognizingText || '').trim();
    if (!voiceText) {
      wx.showToast({ title: '没听清，再试一次', icon: 'none' });
      return;
    }
    const stableBaseText = baseText !== undefined ? baseText : (this.voiceBaseText || '');
    const mergedText = this.mergeVoiceText(stableBaseText, voiceText);
    const nextText = mergedText.slice(0, 200);
    this.setData({
      textData: nextText,
      canAnalyze: nextText.trim().length >= 5
    });
    if (mergedText.length > 200) {
      wx.showToast({ title: '已截取前200字', icon: 'none' });
    }
  },

  mergeVoiceText(baseText, voiceText) {
    const cleanBase = (baseText || '').trim();
    const cleanVoice = (voiceText || '').trim();
    const mergedText = cleanBase ? `${cleanBase} ${cleanVoice}` : cleanVoice;
    const nextText = mergedText.slice(0, 200);
    return nextText;
  },

  // 启动占位符轮播
  startPlaceholderRotation() {
    this.stopPlaceholderRotation();
    this.data._placeholderTimer = setInterval(() => {
      const next = (this.data.placeholderIndex + 1) % this.data.placeholders.length;
      this.setData({ placeholderIndex: next });
    }, 3000);
  },

  // 停止占位符轮播
  stopPlaceholderRotation() {
    if (this.data._placeholderTimer) {
      clearInterval(this.data._placeholderTimer);
      this.data._placeholderTimer = null;
    }
  },

  // 清理所有定时器
  clearAllTimers() {
    this.stopPlaceholderRotation();
    if (this.data._animTimers && this.data._animTimers.length > 0) {
      this.data._animTimers.forEach(timer => {
        clearTimeout(timer);
        clearInterval(timer);
      });
      this.data._animTimers = [];
    }
  },

  // ✅ 已移除：激励视频广告相关代码已迁移到"我的"页面
  // initAd() { ... }
  // grantReward() { ... }

  // 先登录再执行回调
  checkLoginThen(callback) {
    const token = wx.getStorageSync('token');
    if (!token) {
      loginWithWechat().then(() => {
        // ✅ 新增：登录成功后，延迟查询未读消息数（确保 TabBar 已加载）
        setTimeout(() => {
          this.loadUnreadCount();
        }, 300);
        callback && callback();
      }).catch(err => {
        console.error('登录失败:', err);
      });
    } else {
      // ✅ 已有token，也查询一次未读数（确保数据最新）
      this.loadUnreadCount();
      callback && callback();
    }
  },
  
  // ✅ 加载未读消息数
  loadUnreadCount() {
    request({
      url: '/user/api/v1/notification/unread-count',
      method: 'GET'
    }).then(res => {
      if (res.data) {
        console.log('[Index] 未读消息数:', res.data);
        // ✅ 更新全局数据
        const app = getApp();
        if (app && app.globalData) {
          app.globalData.unreadCount = res.data.totalCount || 0;
          app.globalData.dailyBonusUnread = res.data.dailyBonusUnread || 0;
          app.globalData.horoscopeUnread = res.data.horoscopeUnread || 0;
          console.log('[Index] 已更新全局未读数');
        }
        
        // ✅ 直接通知 TabBar 组件更新
        setTimeout(() => {
          const tabBar = this.selectComponent('#tabBar');
          if (tabBar) {
            console.log('[Index] 找到 TabBar 组件，手动更新');
            tabBar.setData({
              unreadCount: res.data.totalCount || 0,
              dailyBonusUnread: res.data.dailyBonusUnread || 0,
              horoscopeUnread: res.data.horoscopeUnread || 0
            });
            console.log('[Index] TabBar 更新完成, unreadCount:', tabBar.data.unreadCount);
          } else {
            console.warn('[Index] 未找到 TabBar 组件');
          }
        }, 100);
      }
    }).catch(err => {
      console.error('[Index] 获取未读数失败:', err);
    });
  },

  // 获取今日日期字符串
  getToday() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}年${m}月${day}日`;
  },

  // ---- 输入框事件 ----
  onTextInput(e) {
    const value = e.detail.value;
    this.setData({
      textData: value,
      canAnalyze: this.data.inputMode === 'text' ? value.trim().length >= 5 : this.data.drawingHasContent
    });
    
    // 用户开始输入时，不再隐藏默认回复，保持显示
    // 只有当用户提交分析后，才会被新数据替换
  },

  switchInputMode(e) {
    const mode = e.currentTarget.dataset.mode;
    if (!mode || mode === this.data.inputMode) return;
    this.stopVoiceInput();
    const nextData = {
      inputMode: mode,
      canAnalyze: mode === 'drawing' ? this.data.drawingHasContent : this.data.textData.trim().length >= 5
    };
    if (mode === 'drawing') {
      nextData.selectedScene = '';
      nextData.selectedSceneKey = '';
      nextData.tagDraftText = '';
      nextData.currentPlaceholder = '';
    }
    this.setData(nextData);
    if (mode === 'drawing') {
      setTimeout(() => this.initDrawingCanvas(), 80);
    }
  },

  // ---- 清除输入框内容 ----
  clearText() {
    this.setData({ 
      textData: '',
      showResult: false,  // 隐藏结果区域
      selectedScene: '',
      selectedSceneKey: '',
      tagDraftText: '',
      currentPlaceholder: '',
      canAnalyze: false
    });
    this.startPlaceholderRotation();
  },

  initDrawingCanvas() {
    if (!this.drawingStrokes) {
      this.drawingStrokes = [];
    }
    const query = wx.createSelectorQuery().in(this);
    query.select('#emotionCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (res && res[0] && res[0].node) {
          const canvas = res[0].node;
          const dpr = wx.getSystemInfoSync().pixelRatio || 1;
          canvas.width = res[0].width * dpr;
          canvas.height = res[0].height * dpr;
          const ctx = canvas.getContext('2d');
          ctx.scale(dpr, dpr);
          ctx.lineCap = 'round';
          ctx.lineJoin = 'round';
          this.drawingCanvas = canvas;
          this.drawingCtx = ctx;
          this.drawingCanvasSize = { width: res[0].width, height: res[0].height };
          this.drawingCtxMode = '2d';
          this.redrawDrawingCanvas();
          return;
        }
        if (!wx.createCanvasContext) return;
        this.drawingCtx = wx.createCanvasContext('emotionCanvas', this);
        this.drawingCtxMode = 'legacy';
        this.redrawDrawingCanvas();
      });
  },

  redrawDrawingCanvas() {
    const ctx = this.drawingCtx || wx.createCanvasContext('emotionCanvas', this);
    if (!ctx) return;
    this.drawingCtx = ctx;
    const size = this.drawingCanvasSize || { width: 345, height: 260 };
    if (this.drawingCtxMode === '2d') {
      ctx.clearRect(0, 0, size.width, size.height);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(0, 0, size.width, size.height);
    } else {
      ctx.setFillStyle('#FFFFFF');
      ctx.fillRect(0, 0, 345, 260);
    }
    (this.drawingStrokes || []).forEach((stroke) => {
      this.drawStroke(ctx, stroke);
    });
    if (this.drawingCtxMode !== '2d') {
      ctx.draw();
    }
  },

  drawStroke(ctx, stroke) {
    const points = stroke.points || [];
    if (points.length === 0) return;
    if (this.drawingCtxMode === '2d') {
      ctx.strokeStyle = stroke.color || this.data.selectedDrawingColor;
      ctx.lineWidth = stroke.width || this.data.selectedDrawingBrushSize;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
    } else {
      ctx.setStrokeStyle(stroke.color || this.data.selectedDrawingColor);
      ctx.setLineWidth(stroke.width || this.data.selectedDrawingBrushSize);
      ctx.setLineCap('round');
      ctx.setLineJoin('round');
    }
    ctx.beginPath();
    ctx.moveTo(points[0].x, points[0].y);
    if (points.length === 1) {
      ctx.lineTo(points[0].x + 0.5, points[0].y + 0.5);
    } else {
      for (let i = 1; i < points.length; i += 1) {
        ctx.lineTo(points[i].x, points[i].y);
      }
    }
    ctx.stroke();
  },

  onDrawingStart(e) {
    if (!this.drawingCtx) {
      this.initDrawingCanvas();
    }
    const point = this.getTouchPoint(e);
    if (!point) return;
    if (!this.drawingStartedAt) {
      this.drawingStartedAt = Date.now();
    }
    this.currentDrawingStroke = {
      color: this.data.selectedDrawingColor,
      width: this.data.selectedDrawingBrushSize,
      points: [point]
    };
    this.setData({
      drawingHasContent: true,
      canAnalyze: this.data.inputMode === 'drawing' ? true : this.data.canAnalyze
    });
  },

  onDrawingMove(e) {
    if (!this.currentDrawingStroke) return;
    const point = this.getTouchPoint(e);
    if (!point) return;
    const points = this.currentDrawingStroke.points;
    const last = points[points.length - 1];
    points.push(point);

    const ctx = this.drawingCtx;
    if (this.drawingCtxMode === '2d') {
      ctx.strokeStyle = this.currentDrawingStroke.color;
      ctx.lineWidth = this.currentDrawingStroke.width;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
    } else {
      ctx.setStrokeStyle(this.currentDrawingStroke.color);
      ctx.setLineWidth(this.currentDrawingStroke.width);
      ctx.setLineCap('round');
      ctx.setLineJoin('round');
    }
    ctx.beginPath();
    ctx.moveTo(last.x, last.y);
    ctx.lineTo(point.x, point.y);
    ctx.stroke();
    if (this.drawingCtxMode !== '2d') {
      ctx.draw(true);
    }
  },

  onDrawingEnd() {
    if (!this.currentDrawingStroke) return;
    this.drawingStrokes = this.drawingStrokes || [];
    this.drawingStrokes.push(this.currentDrawingStroke);
    const shouldRedraw = this.currentDrawingStroke.points.length <= 1;
    this.currentDrawingStroke = null;
    this.setData({
      drawingStrokeCount: this.drawingStrokes.length,
      drawingHasContent: this.drawingStrokes.length > 0,
      canAnalyze: this.data.inputMode === 'drawing' ? this.drawingStrokes.length > 0 : this.data.canAnalyze
    });
    if (shouldRedraw) {
      this.redrawDrawingCanvas();
    }
  },

  getTouchPoint(e) {
    const touch = e.touches && e.touches[0];
    if (!touch) return null;
    return {
      x: touch.x,
      y: touch.y
    };
  },

  selectDrawingColor(e) {
    const color = e.currentTarget.dataset.color;
    if (!color) return;
    this.setData({ selectedDrawingColor: color });
  },

  selectDrawingBrushSize(e) {
    const size = Number(e.currentTarget.dataset.size);
    if (!size) return;
    this.setData({ selectedDrawingBrushSize: size });
  },

  selectDrawingMood(e) {
    const mood = e.currentTarget.dataset.mood;
    this.setData({
      selectedDrawingMood: this.data.selectedDrawingMood === mood ? '' : mood
    });
  },

  undoDrawing() {
    this.drawingStrokes = this.drawingStrokes || [];
    if (this.drawingStrokes.length === 0) return;
    this.drawingStrokes.pop();
    const hasContent = this.drawingStrokes.length > 0;
    this.setData({
      drawingStrokeCount: this.drawingStrokes.length,
      drawingHasContent: hasContent,
      canAnalyze: this.data.inputMode === 'drawing' ? hasContent : this.data.canAnalyze
    });
    this.redrawDrawingCanvas();
  },

  clearDrawing() {
    this.drawingStrokes = [];
    this.currentDrawingStroke = null;
    this.drawingStartedAt = null;
    this.setData({
      drawingHasContent: false,
      drawingStrokeCount: 0,
      canAnalyze: this.data.inputMode === 'drawing' ? false : this.data.canAnalyze
    });
    this.redrawDrawingCanvas();
  },

  buildDrawingEmotionText() {
    const strokes = this.drawingStrokes || [];
    const colorMap = {};
    let pointCount = 0;
    let minX = Infinity;
    let minY = Infinity;
    let maxX = 0;
    let maxY = 0;
    let heavyStrokeCount = 0;

    strokes.forEach((stroke) => {
      const colorName = (DRAWING_COLORS.find(item => item.value === stroke.color) || {}).name || stroke.color;
      colorMap[colorName] = (colorMap[colorName] || 0) + 1;
      if ((stroke.width || 0) >= 12) heavyStrokeCount += 1;
      (stroke.points || []).forEach((point) => {
        pointCount += 1;
        minX = Math.min(minX, point.x);
        minY = Math.min(minY, point.y);
        maxX = Math.max(maxX, point.x);
        maxY = Math.max(maxY, point.y);
      });
    });

    const duration = this.drawingStartedAt ? Math.max(1, Math.round((Date.now() - this.drawingStartedAt) / 1000)) : 1;
    const colors = Object.keys(colorMap).map(name => `${name}${colorMap[name]}笔`).join('、') || '未识别颜色';
    const areaWidth = Number.isFinite(minX) ? Math.round(maxX - minX) : 0;
    const areaHeight = Number.isFinite(minY) ? Math.round(maxY - minY) : 0;
    const density = pointCount >= 180 ? '线条很密' : pointCount >= 80 ? '线条中等' : '线条较少';
    const coverage = areaWidth > 230 || areaHeight > 160 ? '覆盖范围较大' : '集中在局部区域';
    const pressure = heavyStrokeCount >= Math.max(2, strokes.length / 2) ? '粗线条较多' : '线条力度较轻';
    const mood = this.data.selectedDrawingMood || '未选择';

    return [
      '用户没有输入文字，而是画了一张情绪涂鸦。以下是小程序记录的绘制过程信息。',
      `用户选择的当前感受：${mood}。`,
      `绘制时长约${duration}秒，共${strokes.length}笔，${density}，${coverage}，${pressure}。`,
      `颜色使用：${colors}。`
    ].join('\n');
  },

  getDrawingShareText() {
    const strokes = this.drawingStrokes || [];
    const mood = this.data.selectedDrawingMood ? ` · ${this.data.selectedDrawingMood}` : '';
    return `我画了一张情绪涂鸦${mood}，用了${strokes.length}笔把这一刻放下来。`;
  },

  exportDrawingCanvas() {
    return new Promise((resolve, reject) => {
      const options = this.drawingCtxMode === '2d' && this.drawingCanvas
        ? { canvas: this.drawingCanvas, fileType: 'jpg', quality: 0.92 }
        : { canvasId: 'emotionCanvas', fileType: 'jpg', quality: 0.92 };
      wx.canvasToTempFilePath({
        ...options,
        success: (res) => {
          resolve(res.tempFilePath);
        },
        fail: (err) => {
          console.error('[Index] 导出涂鸦图片失败:', err);
          reject(err);
        }
      }, this);
    });
  },

  async uploadDrawingImage() {
    const imagePath = await this.exportDrawingCanvas();
    const uploadRes = await uploadFileWithLogin(imagePath, {
      url: '/files/upload',
      name: 'file'
    });
    if (!uploadRes || !uploadRes.data) {
      throw new Error('涂鸦图片上传失败');
    }
    return {
      uploadFileId: uploadRes.data,
      imagePath
    };
  },

  // ---- 滚动到输入框并聚焦 ----
  scrollToInput() {
    // 滚动到输入卡片位置
    wx.pageScrollTo({
      selector: '.input-card',
      duration: 300,
      success: () => {
        // 延迟一下再提示，确保滚动完成
        setTimeout(() => {
          wx.showToast({
            title: '请在上方输入你的心情',
            icon: 'none',
            duration: 2000
          });
        }, 400);
      }
    });
  },

  // ---- 生成心情卡片（共用逻辑） ----
  async generateHeartCard() {
    // 判断是否已经进行过分析
    if (!this.data.showResult || !this.data.analysisData.text) {
      this.scrollToInput();
      setTimeout(() => {
        wx.showToast({
          title: '请先分析心情，再生成专属卡片',
          icon: 'none',
          duration: 2500
        });
      }, 800);
      return null;
    }

    // 防止重复点击
    if (this.data.isGeneratingPoster) return null;

    this.setData({ isGeneratingPoster: true });
    wx.showLoading({ title: '正在生成心情卡片...', mask: true });

    try {
      const posterPath = await this.generatePoster();
      wx.hideLoading();
      this.setData({ posterImagePath: posterPath, isGeneratingPoster: false });
      return posterPath;
    } catch (err) {
      console.error('生成心情卡片失败:', err);
      wx.hideLoading();
      this.setData({ isGeneratingPoster: false });
      wx.showToast({ title: '生成失败，请重试', icon: 'none' });
      return null;
    }
  },

  // ---- 处理分享卡片按钮点击：直接弹出原生分享菜单 ----
  async handleShareCard() {
    if (this.data.isSharingCard) return;
    this.setData({ isSharingCard: true });
    const themeKey = await this.chooseShareTheme();
    if (!themeKey) {
      this.setData({ isSharingCard: false });
      return;
    }
    this.setData({ selectedShareThemeKey: themeKey });
    const posterPath = await this.generateHeartCard();
    if (!posterPath) {
      this.setData({ isSharingCard: false });
      return;
    }
    wx.showShareImageMenu({
      path: posterPath,
      needShowEntrance: true,
      entrancePath: '/pages/index/index',
      complete: () => {
        this.logEmotionEvent('template_share', { extra: 'poster' });
        this.setData({ isSharingCard: false });
      }
    });
  },

  chooseShareTheme() {
    return new Promise((resolve) => {
      wx.showActionSheet({
        itemList: SHARE_THEMES.map(item => item.name),
        success: (res) => {
          const theme = SHARE_THEMES[res.tapIndex];
          resolve(theme ? theme.key : '');
        },
        fail: () => resolve('')
      });
    });
  },

  getSelectedShareTheme() {
    return SHARE_THEMES.find(item => item.key === this.data.selectedShareThemeKey) || SHARE_THEMES[0];
  },

  // Canvas 绘制海报 → 返回临时图片路径
  generatePoster() {
    return new Promise((resolve, reject) => {
      const query = wx.createSelectorQuery().in(this);
      query.select('#shareCanvas')
        .fields({ node: true, size: true })
        .exec(async (res) => {
          if (!res || !res[0] || !res[0].node) {
            reject(new Error('Canvas节点未找到'));
            return;
          }
          try {
            const canvas = res[0].node;
            const ctx = canvas.getContext('2d');
            const dpr = wx.getSystemInfoSync().pixelRatio;

            const width = 750;
            const height = 1334;
            canvas.width = width * dpr;
            canvas.height = height * dpr;
            ctx.scale(dpr, dpr);

            const theme = this.getSelectedShareTheme();

            // 1. 背景渐变
            const bgGrad = ctx.createLinearGradient(0, 0, 0, height);
            bgGrad.addColorStop(0, theme.colorStart);
            bgGrad.addColorStop(0.55, theme.colorMid);
            bgGrad.addColorStop(1, theme.colorEnd);
            ctx.fillStyle = bgGrad;
            ctx.fillRect(0, 0, width, height);

            // 2. 装饰光晕
            ctx.save();
            const glowGrad = ctx.createRadialGradient(width * 0.75, 80, 20, width * 0.75, 100, 300);
            glowGrad.addColorStop(0, 'rgba(255, 255, 255, 0.16)');
            glowGrad.addColorStop(1, 'rgba(249, 168, 197, 0)');
            ctx.fillStyle = glowGrad;
            ctx.beginPath();
            ctx.arc(width * 0.75, 100, 280, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();

            // 3. 调用海报绘制
            const doodleImage = await this.loadPosterImage(canvas, this.data.analysisData && this.data.analysisData.drawingImagePath);
            this.drawPosterContent(ctx, width, height, doodleImage);

            // 4. 转为临时图片
            wx.canvasToTempFilePath({
              canvas: canvas,
              success: (tempRes) => {
                console.log('海报生成成功:', tempRes.tempFilePath);
                resolve(tempRes.tempFilePath);
              },
              fail: (err) => {
                reject(err);
              }
            });
          } catch (err) {
            reject(err);
          }
        });
    });
  },

  loadPosterImage(canvas, imagePath) {
    return new Promise((resolve) => {
      if (!imagePath || !canvas || !canvas.createImage) {
        resolve(null);
        return;
      }
      const image = canvas.createImage();
      image.onload = () => resolve(image);
      image.onerror = (err) => {
        console.warn('加载涂鸦图片失败，跳过海报涂鸦预览:', err);
        resolve(null);
      };
      image.src = imagePath;
    });
  },

  // 绘制海报内容（750x1334）—— 移除二维码，修复卡片样式
  drawPosterContent(ctx, width, height, doodleImage) {
    const data = this.data.analysisData;
    if (!data) return;

    const left = 60;
    const right = width - 60;
    const cw = right - left;
    let y = 80;

    ctx.textAlign = 'center';

    // ============ 1. 顶部标题 + 日期 ============
    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.font = 'bold 38px sans-serif';
    ctx.fillText('情绪急救模板', width / 2, y);
    y += 52;

    const now = new Date();
    const dateStr = `${now.getFullYear()} / ${now.getMonth() + 1} / ${now.getDate()}`;
    ctx.fillStyle = 'rgba(255, 255, 255, 0.45)';
    ctx.font = '22px sans-serif';
    ctx.fillText(dateStr, width / 2, y);
    y += 56;

    // 装饰线
    ctx.strokeStyle = 'rgba(249, 168, 197, 0.3)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(left + 60, y);
    ctx.lineTo(right - 60, y);
    ctx.stroke();
    y += 36;

    // ============ 2. 场景标签 ============
    if (data.scene) {
      ctx.textAlign = 'left';
      const sceneText = data.scene;
      ctx.font = 'bold 20px sans-serif';
      const tagW = ctx.measureText(sceneText).width + 36;
      ctx.fillStyle = 'rgba(244, 143, 177, 0.35)';
      this.drawRoundRect(ctx, left, y - 6, tagW, 36, 18);
      ctx.fill();
      ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
      ctx.fillText(sceneText, left + 18, y + 16);
      y += 56;
    }

    // ============ 3. 用户输入/涂鸦卡片 ============
    if (doodleImage) {
      const doodleCardH = 300;
      ctx.fillStyle = 'rgba(255, 255, 255, 0.1)';
      this.drawRoundRect(ctx, left, y, cw, doodleCardH, 16);
      ctx.fill();
      ctx.strokeStyle = 'rgba(249, 168, 197, 0.22)';
      ctx.lineWidth = 1;
      ctx.stroke();

      ctx.textAlign = 'left';
      ctx.fillStyle = 'rgba(255, 255, 255, 0.55)';
      ctx.font = '20px sans-serif';
      ctx.fillText('我的涂鸦', left + 20, y + 32);

      const imgBoxX = left + 24;
      const imgBoxY = y + 52;
      const imgBoxW = cw - 48;
      const imgBoxH = 220;
      ctx.fillStyle = '#FFFFFF';
      this.drawRoundRect(ctx, imgBoxX, imgBoxY, imgBoxW, imgBoxH, 12);
      ctx.fill();
      this.drawImageContain(ctx, doodleImage, imgBoxX + 12, imgBoxY + 12, imgBoxW - 24, imgBoxH - 24);
      y += doodleCardH + 28;
    } else {
      const userText = data.scene ? `场景：${data.scene}` : '我刚整理了一句更稳妥的回复';
      ctx.font = 'italic 26px sans-serif';
      const userLines = this.measureLines(ctx, userText, cw - 48);
      const userCardH = userLines * 38 + 52;

      // 先画卡片背景
      ctx.fillStyle = 'rgba(255, 255, 255, 0.08)';
      this.drawRoundRect(ctx, left, y, cw, userCardH, 14);
      ctx.fill();

      // 再画文字
      ctx.textAlign = 'left';
      ctx.fillStyle = 'rgba(255, 255, 255, 0.45)';
      ctx.font = '20px sans-serif';
      ctx.fillText('低隐私模板', left + 20, y + 30);

      ctx.fillStyle = 'rgba(255, 255, 255, 0.78)';
      ctx.font = 'italic 26px sans-serif';
      this.wrapText(ctx, '"' + userText + '"', left + 24, y + 58, cw - 48, 38);
      y += userCardH + 28;
    }

    // ============ 4. 情绪解压卡 ============
    const diagCardTop = y;

    // 先计算内容高度（背景需要先画）
    const emotionTag = data.emotionTag || '情绪波动中';
    const aiComment = data.aiComment || '每一种情绪都值得被看见~';

    ctx.font = 'bold 24px sans-serif';
    const titleH = 36;
    ctx.font = 'bold 42px sans-serif';
    const tagH = 54;
    ctx.font = '26px sans-serif';
    const commentLines = this.measureLines(ctx, aiComment, cw - 48);
    const commentH = commentLines * 38 + 16;
    const diagCardH = 24 + titleH + tagH + commentH + 20;

    // 画卡片背景（更不透明，更明显）
    ctx.fillStyle = 'rgba(88, 70, 200, 0.22)';
    this.drawRoundRect(ctx, left, diagCardTop, cw, diagCardH, 16);
    ctx.fill();
    ctx.strokeStyle = 'rgba(249, 168, 197, 0.3)';
    ctx.lineWidth = 1;
    ctx.stroke();

    // 画内容
    y = diagCardTop + 28;
    ctx.textAlign = 'left';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.65)';
    ctx.font = 'bold 24px sans-serif';
    ctx.fillText('我现在怎么了', left + 20, y);
    y += titleH + 8;

    ctx.fillStyle = '#FFD93D';
    ctx.font = 'bold 42px sans-serif';
    ctx.fillText(emotionTag, left + 20, y);

    // 下划线
    const tagMetrics = ctx.measureText(emotionTag);
    const tagWidth = tagMetrics.width;
    ctx.strokeStyle = 'rgba(255, 217, 61, 0.4)';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(left + 20, y + 14);
    ctx.lineTo(left + 20 + tagWidth, y + 14);
    ctx.stroke();
    y += tagH + 4;

    ctx.fillStyle = 'rgba(255, 255, 255, 0.88)';
    ctx.font = '26px sans-serif';
    this.wrapText(ctx, aiComment, left + 20, y, cw - 48, 38);
    y = diagCardTop + diagCardH + 28;

    // ============ 5. 先稳住一下 ============
    const actions = (data.reliefActions && data.reliefActions.length ? data.reliefActions : DEFAULT_RELIEF_ACTIONS).slice(0, 3);
    const reliefCardTop = y;
    const reliefCardH = 170;
    ctx.fillStyle = 'rgba(255, 255, 255, 0.08)';
    this.drawRoundRect(ctx, left, reliefCardTop, cw, reliefCardH, 16);
    ctx.fill();

    ctx.textAlign = 'left';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.65)';
    ctx.font = 'bold 24px sans-serif';
    ctx.fillText('先稳住一下', left + 20, reliefCardTop + 36);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.88)';
    ctx.font = '24px sans-serif';
    actions.forEach((action, index) => {
      ctx.fillText(`${index + 1}. ${action}`, left + 24, reliefCardTop + 76 + index * 34);
    });
    y = reliefCardTop + reliefCardH + 28;

    // ============ 6. 宇宙提示卡片 ============
    const cosmosCardTop = y;
    const cosmosCardH = 200;

    // 深色卡片背景
    ctx.fillStyle = 'rgba(0, 0, 0, 0.4)';
    this.drawRoundRect(ctx, left, cosmosCardTop, cw, cosmosCardH, 16);
    ctx.fill();
    ctx.strokeStyle = 'rgba(249, 168, 197, 0.2)';
    ctx.lineWidth = 1;
    ctx.stroke();

    // 星星装饰（更明显）
    ctx.fillStyle = 'rgba(255, 255, 255, 0.35)';
    const stars = [
      [left + 30, cosmosCardTop + 25],
      [right - 40, cosmosCardTop + 35],
      [left + cw / 2, cosmosCardTop + 18],
      [left + 55, cosmosCardTop + 160],
      [right - 65, cosmosCardTop + 140]
    ];
    stars.forEach(([sx, sy]) => {
      ctx.beginPath();
      ctx.arc(sx, sy, 2, 0, Math.PI * 2);
      ctx.fill();
    });

    ctx.textAlign = 'left';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
    ctx.font = 'bold 22px sans-serif';
    ctx.fillText('宇宙给你的提示', left + 24, cosmosCardTop + 46);

    const answer = data.answerBook || '顺其自然';
    ctx.textAlign = 'center';
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 44px sans-serif';
    ctx.fillText(answer, width / 2, cosmosCardTop + 100);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.38)';
    ctx.font = '20px sans-serif';
    ctx.fillText('每一次遇见都是最好的安排', width / 2, cosmosCardTop + 140);

    y = cosmosCardTop + cosmosCardH + 40;

    // ============ 7. 底部品牌信息（无二维码） ============
    // 让底部信息自适应位置，不强制贴底，避免大块空白
    const bottomY = Math.min(y + 40, height - 100);

    ctx.textAlign = 'center';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.25)';
    ctx.font = '18px sans-serif';
    ctx.fillText('—— 情绪解压卡 ——', width / 2, bottomY);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.font = '16px sans-serif';
    ctx.fillText('先稳住，再生成一条能发出去的话', width / 2, bottomY + 28);
  },

  // ---- 保存到相册（结果区域底部按钮）直接保存 ----
  async saveHealingCard() {
    const posterPath = await this.generateHeartCard();
    if (!posterPath) return;
    wx.showLoading({ title: '保存中...' });
    this.checkAndSavePhoto(posterPath);
  },

  // ---- 情绪解压卡：提交文本分析 ----
  async submitText() {
    const isDrawingMode = this.data.inputMode === 'drawing';
    if (isDrawingMode) {
      if (!this.data.drawingHasContent || !this.drawingStrokes || this.drawingStrokes.length === 0) {
        wx.showToast({ title: '先在画板上画几笔', icon: 'none' });
        return;
      }
    } else if (this.data.textData.trim().length < 5) {
      wx.showToast({ title: '先点一个灵感，或写下5个字以上', icon: 'none' });
      return;
    }
    if (this.data.isAnalyzing) return;

    const drawingMeta = isDrawingMode ? this.buildDrawingEmotionText() : '';
    const submitText = isDrawingMode ? drawingMeta : this.data.textData;
    const submitScene = isDrawingMode ? '情绪涂鸦' : (this.data.selectedScene || undefined);
    const displayText = isDrawingMode ? this.getDrawingShareText() : this.data.textData;

    this.setData({
      isAnalyzing: true,
      showResult: false,
      emotionTag: '',
      aiComment: '',
      painPoint: '',
      stabilizeAction: '',
      dontSay: '',
      nextTimeTip: '',
      interactionId: null,
      reliefActions: DEFAULT_RELIEF_ACTIONS,
      replies: { safe: '', firm: '', gentle: '', shortReply: '', high_eq: '', crazy: '', sarcastic: '' },
      answerBook: '',
      activeTab: 0
    });

    try {
      this.logEmotionEvent('submit_start');
      let res;
      let drawingImagePath = '';
      if (isDrawingMode) {
        const drawingUpload = await this.uploadDrawingImage();
        drawingImagePath = drawingUpload.imagePath;
        res = await requestWithLogin({
          url: '/user/api/v1/submitDrawing',
          method: 'POST',
          data: {
            uploadFileId: drawingUpload.uploadFileId,
            selectedMood: this.data.selectedDrawingMood || '',
            drawingMeta,
            drawingShareText: displayText
          }
        });
      } else {
        res = await requestWithLogin({
          url: '/user/api/v1/submitText',
          method: 'POST',
          data: {
            text: submitText,
            scene: submitScene,
            taskType: this.data.selectedTaskType || 'REPLY_RESCUE',
            scenarioKey: this.data.selectedScenarioKey || ''
          }
        });
      }
      console.log('情绪解压卡生成完成:', res);

      if ((res.code === 200 || res.code === "0" || res.success === true) && res.data) {
        const d = res.data;
        const replies = d.replies || {};
        const interactionId = d.interactionId || d.interaction_id || null;
        const reliefActions = Array.isArray(d.relief_actions) && d.relief_actions.length > 0
          ? d.relief_actions.slice(0, 3)
          : DEFAULT_RELIEF_ACTIONS;
        this.setData({
          isAnalyzing: false,
          showResult: true,
          emotionTag: d.emotion_tag || '情绪波动中',
          aiComment: d.ai_comment || '每一种情绪都值得被看见~',
          painPoint: d.pain_point || '你真正难受的点，可能是边界被碰到却还想把话说清楚。',
          stabilizeAction: d.stabilize_action || reliefActions[0] || DEFAULT_RELIEF_ACTIONS[0],
          dontSay: d.dont_say || '先别用反问、指责或翻旧账开头。',
          nextTimeTip: d.next_time_tip || '下次先确认底线，再决定是否立刻回复。',
          interactionId,
          reliefActions,
          replies: {
            safe: replies.safe || replies.high_eq || '',
            firm: replies.firm || replies.crazy || '',
            gentle: replies.gentle || '',
            shortReply: replies.short || replies.short_reply || replies.sarcastic || '',
            high_eq: replies.high_eq || replies.safe || '',
            crazy: replies.crazy || replies.firm || '',
            sarcastic: replies.sarcastic || replies.short || replies.short_reply || ''
          },
          answerBook: d.answer_book || '顺其自然',
          analysisData: {
            text: displayText,
            scene: submitScene || '',
            drawingImagePath,
            interactionId,
            taskType: d.task_type || this.data.selectedTaskType || 'REPLY_RESCUE',
            scenarioKey: this.data.selectedScenarioKey || '',
            emotionTag: d.emotion_tag || '',
            aiComment: d.ai_comment || '',
            painPoint: d.pain_point || '',
            stabilizeAction: d.stabilize_action || '',
            dontSay: d.dont_say || '',
            nextTimeTip: d.next_time_tip || '',
            reliefActions,
            highEqReply: replies.high_eq || replies.safe || '',
            boundaryReply: replies.crazy || replies.firm || '',
            gentleReply: replies.gentle || '',
            sarcasticReply: replies.sarcastic || replies.short || replies.short_reply || '',
            safeReply: replies.safe || replies.high_eq || '',
            firmReply: replies.firm || replies.crazy || '',
            shortReply: replies.short || replies.short_reply || replies.sarcastic || '',
            answerBook: d.answer_book || ''
          }
        });
        this.logEmotionEvent('submit_success', { interactionId });

        // 平滑滚动：将生成按钮置于视图顶部，解压卡自然展示在下方
        setTimeout(() => {
          wx.pageScrollTo({ selector: '.action-section', duration: 300 });
        }, 150);
      } else {
        this.setData({ isAnalyzing: false });
        wx.showToast({ title: res.message || '感知失败', icon: 'none' });
      }
    } catch (err) {
      console.error('分析失败:', err);
      this.setData({ isAnalyzing: false });
      if (err.message !== '401') {
        wx.showToast({ title: '感知失败，请重试', icon: 'none' });
      }
    }
  },

  // ---- 情绪解压卡：常见烦恼灵感点击 ----
  onSceneTap(e) {
    const { key, scene } = e.currentTarget.dataset;
    const tag = this.data.sceneTags.find(item => item.key === key) || {};
    const isSelected = this.data.selectedScene === scene;
    const currentText = (this.data.textData || '').trim();
    const previousDraft = this.data.tagDraftText || '';
    const shouldReplaceText = !currentText || this.data.textData === previousDraft;

    if (isSelected) {
      // 取消选中：清空灵感、恢复默认占位符轮播
      const nextText = this.data.textData === previousDraft ? '' : this.data.textData;
      this.setData({
        selectedScene: '',
        selectedSceneKey: '',
        selectedTaskType: 'REPLY_RESCUE',
        selectedScenarioKey: '',
        tagDraftText: '',
        textData: nextText,
        currentPlaceholder: '',
        canAnalyze: nextText.trim().length >= 5
      });
      this.startPlaceholderRotation();
    } else {
      // 选中灵感：停止轮播、设置对应占位符；空输入时直接填充可提交草稿
      this.stopPlaceholderRotation();
      const nextText = shouldReplaceText ? (tag.draft || '') : this.data.textData;
      this.setData({
        selectedScene: scene,
        selectedSceneKey: key,
        selectedTaskType: tag.taskType || 'REPLY_RESCUE',
        selectedScenarioKey: key,
        tagDraftText: tag.draft || '',
        textData: nextText,
        currentPlaceholder: this.data.scenePlaceholders[scene] || '',
        canAnalyze: nextText.trim().length >= 5
      });
      if (!shouldReplaceText) {
        wx.showToast({ title: `已按「${scene}」来分析`, icon: 'none' });
      }
    }
  },

  // ---- 情绪解压卡：回复 Tab 切换 ----
  switchReplyTab(e) {
    const tab = parseInt(e.currentTarget.dataset.tab);
    this.setData({ activeTab: tab });
  },

  getReplyMeta(style) {
    const tab = this.data.activeTab;
    const map = {
      safe: { key: 'safe', label: '稳妥表达', text: this.data.replies.safe || this.data.replies.high_eq },
      firm: { key: 'firm', label: '坚定边界', text: this.data.replies.firm || this.data.replies.crazy },
      gentle: { key: 'gentle', label: '温柔缓和', text: this.data.replies.gentle },
      short: { key: 'short', label: '短句版', text: this.data.replies.shortReply || this.data.replies.sarcastic }
    };
    if (style && map[style]) return map[style];
    const active = this.data.tabLabels.find(item => item.key === tab);
    return map[(active && active.style) || 'safe'];
  },

  logEmotionEvent(eventType, overrides = {}) {
    requestWithLogin({
      url: '/emotion/api/v1/events',
      method: 'POST',
      data: {
        eventType,
        interactionId: overrides.interactionId || this.data.interactionId || this.data.analysisData.interactionId || null,
        taskType: overrides.taskType || this.data.analysisData.taskType || this.data.selectedTaskType || 'REPLY_RESCUE',
        scenarioKey: overrides.scenarioKey || this.data.analysisData.scenarioKey || this.data.selectedScenarioKey || '',
        replyStyle: overrides.replyStyle || '',
        extra: overrides.extra || ''
      }
    }).catch(err => {
      console.warn('[emotion event] 记录失败:', eventType, err);
    });
  },

  // ---- 一键复制当前回复 ----
  copyReply() {
    const meta = this.getReplyMeta();
    this.copyReplyByMeta(meta);
  },

  copyReplyByMeta(meta) {
    if (!meta || !meta.text) {
      wx.showToast({ title: '暂无内容', icon: 'none' });
      return;
    }
    if (this.data.copyingReplyKey) return;
    this.setData({ copyingReplyKey: meta.key });
    wx.setClipboardData({
      data: meta.text,
      success: () => {
        wx.showToast({ title: `已复制${meta.label}`, icon: 'success' });
        this.logEmotionEvent('reply_copy', { replyStyle: meta.key });
      },
      complete: () => { this.setData({ copyingReplyKey: '' }); }
    });
  },

  favoriteReplyByMeta(meta) {
    if (!meta || !meta.text) {
      wx.showToast({ title: '暂无内容', icon: 'none' });
      return;
    }
    if (this.data.favoritingReplyKey) return;
    this.setData({ favoritingReplyKey: meta.key });
    requestWithLogin({
      url: '/emotion/api/v1/favorites',
      method: 'POST',
      data: {
        interactionId: this.data.interactionId || this.data.analysisData.interactionId || null,
        scenarioKey: this.data.analysisData.scenarioKey || this.data.selectedScenarioKey || '',
        replyStyle: meta.key,
        replyText: meta.text
      }
    }).then(res => {
      if (res.success || res.code === '0' || res.code === 200) {
        wx.showToast({ title: '已收藏话术', icon: 'success' });
      } else {
        wx.showToast({ title: res.msg || '收藏失败', icon: 'none' });
      }
      this.setData({ favoritingReplyKey: '' });
    }).catch(err => {
      console.error('[favorite reply] 失败:', err);
      wx.showToast({ title: '收藏失败，请重试', icon: 'none' });
      this.setData({ favoritingReplyKey: '' });
    });
  },

  shareTemplateByMeta(meta) {
    if (!meta || !meta.text) {
      wx.showToast({ title: '暂无内容', icon: 'none' });
      return;
    }
    const template = `我刚整理了一句${meta.label}：\n${meta.text}`;
    wx.setClipboardData({
      data: template,
      success: () => {
        wx.showToast({ title: '模板已复制', icon: 'success' });
        this.logEmotionEvent('template_share', { replyStyle: meta.key });
      }
    });
  },

  // ---- 独立复制高情商回复 ----
  copyHighEqReply() {
    this.copyReplyByMeta(this.getReplyMeta('safe'));
  },

  // ---- 独立复制发疯文学回复 ----
  copyBoundaryReply() {
    this.copyReplyByMeta(this.getReplyMeta('firm'));
  },

  // 兼容旧绑定
  copyCrazyReply() {
    this.copyBoundaryReply();
  },

  // ---- 独立复制温柔回复 ----
  copyGentleReply() {
    this.copyReplyByMeta(this.getReplyMeta('gentle'));
  },

  // ---- 独立复制阴阳怪气回复 ----
  copySarcasticReply() {
    this.copyReplyByMeta(this.getReplyMeta('short'));
  },

  favoriteActiveReply() {
    this.favoriteReplyByMeta(this.getReplyMeta());
  },

  shareActiveReplyTemplate() {
    this.shareTemplateByMeta(this.getReplyMeta());
  },

  // ---- 更新问候语 ----
  updateGreeting() {
    const hour = new Date().getHours();
    const userInfo = wx.getStorageSync('userInfo') || {};
    const nickname = userInfo.nickname || '朋友';
    
    let greeting = '';
    
    if (hour < 6) {
      greeting = '夜深了';
    } else if (hour < 9) {
      greeting = '早安';
    } else if (hour < 12) {
      greeting = '上午好';
    } else if (hour < 14) {
      greeting = '午安';
    } else if (hour < 18) {
      greeting = '下午好';
    } else {
      greeting = '晚上好';
    }
    
    this.setData({ 
      greetingText: greeting,
      nickname: nickname
    });
  },
  // ✅ 检查权限并保存图片
  checkAndSavePhoto(filePath) {
    console.log('开始检查相册权限...');
    
    // 第一步：检查当前权限状态
    wx.getSetting({
      success: (settingRes) => {
        console.log('当前权限设置:', settingRes);
        
        const hasWritePhotosAlbumAuth = settingRes.authSetting['scope.writePhotosAlbum'];
        
        if (hasWritePhotosAlbumAuth === true) {
          // ✅ 已授权，直接保存
          console.log('已有相册权限，直接保存');
          this.savePhotoToAlbum(filePath);
        } else if (hasWritePhotosAlbumAuth === false) {
          // ❌ 用户明确拒绝过，引导去设置
          console.log('用户已拒绝相册权限，引导去设置');
          wx.hideLoading();
          wx.showModal({
            title: '需要相册权限',
            content: '保存图片需要访问您的相册权限，请在设置中开启',
            confirmText: '去设置',
            cancelText: '取消',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting({
                  success: (settingRes) => {
                    if (settingRes.authSetting['scope.writePhotosAlbum']) {
                      // 用户开启了权限，重新保存
                      console.log('用户在设置中开启了权限，重新保存');
                      wx.showLoading({ title: '保存中...' });
                      this.savePhotoToAlbum(filePath);
                    } else {
                      // 用户未开启权限，再次引导
                      wx.showModal({
                        title: '权限未开启',
                        content: '您尚未开启相册权限，无法保存图片。请点击"去设置"开启权限',
                        confirmText: '去设置',
                        cancelText: '取消',
                        success: (res) => {
                          if (res.confirm) {
                            wx.openSetting({
                              success: (settingRes) => {
                                if (settingRes.authSetting['scope.writePhotosAlbum']) {
                                  wx.showLoading({ title: '保存中...' });
                                  this.savePhotoToAlbum(filePath);
                                } else {
                                  wx.showToast({ title: '仍未开启权限', icon: 'none' });
                                }
                              }
                            });
                          }
                        }
                      });
                    }
                  }
                });
              }
            }
          });
        } else {
          // ⚠️ 从未请求过权限，直接引导去设置页面
          console.log('从未请求过权限，引导去设置');
          wx.hideLoading();
          wx.showModal({
            title: '需要相册权限',
            content: '保存图片需要访问您的相册权限，请点击"去设置"开启',
            confirmText: '去设置',
            cancelText: '取消',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting({
                  success: (settingRes) => {
                    if (settingRes.authSetting['scope.writePhotosAlbum']) {
                      console.log('用户在设置中开启了权限，重新保存');
                      wx.showLoading({ title: '保存中...' });
                      this.savePhotoToAlbum(filePath);
                    } else {
                      // 用户未开启权限，再次引导
                      wx.showModal({
                        title: '权限未开启',
                        content: '您尚未开启相册权限，无法保存图片。请点击"去设置"开启权限',
                        confirmText: '去设置',
                        cancelText: '取消',
                        success: (res) => {
                          if (res.confirm) {
                            wx.openSetting({
                              success: (settingRes) => {
                                if (settingRes.authSetting['scope.writePhotosAlbum']) {
                                  wx.showLoading({ title: '保存中...' });
                                  this.savePhotoToAlbum(filePath);
                                } else {
                                  wx.showToast({ title: '仍未开启权限', icon: 'none' });
                                }
                              }
                            });
                          }
                        }
                      });
                    }
                  },
                  fail: (err) => {
                    console.error('打开设置失败:', err);
                    wx.showToast({ title: '打开设置失败', icon: 'none' });
                  }
                });
              } else {
                // 用户点击取消，给予明确提示
                wx.showToast({ 
                  title: '已取消，可再次点击保存重试', 
                  icon: 'none',
                  duration: 2500
                });
              }
            }
          });
        }
      },
      fail: (err) => {
        console.error('获取权限设置失败:', err);
        // 如果获取权限设置失败，直接尝试保存（兼容处理）
        this.savePhotoToAlbum(filePath);
      }
    });
  },

  // ✅ 执行保存到相册
  savePhotoToAlbum(filePath) {
    console.log('执行保存到相册:', filePath);
    
    wx.saveImageToPhotosAlbum({
      filePath: filePath,
      success: () => {
        console.log('保存到相册成功');
        wx.hideLoading();
        wx.showToast({ title: '已保存到相册', icon: 'success', duration: 2000 });
      },
      fail: (err) => {
        console.error('保存到相册失败:', err);
        wx.hideLoading();
        
        // 处理各种失败情况
        if (err.errMsg.includes('auth deny')) {
          // 用户在保存时拒绝
          wx.showModal({
            title: '需要相册权限',
            content: '保存图片需要访问您的相册权限，请在设置中开启',
            confirmText: '去设置',
            cancelText: '取消',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting({
                  success: (settingRes) => {
                    if (settingRes.authSetting['scope.writePhotosAlbum']) {
                      wx.showLoading({ title: '保存中...' });
                      this.savePhotoToAlbum(filePath);
                    }
                  }
                });
              }
            }
          });
        } else if (err.errMsg.includes('fail')) {
          wx.showToast({ title: '保存失败: ' + (err.errMsg || '未知错误'), icon: 'none', duration: 3000 });
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      }
    });
  },

  // 绘制圆角矩形路径
  drawRoundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.arcTo(x + w, y, x + w, y + r, r);
    ctx.lineTo(x + w, y + h - r);
    ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
    ctx.lineTo(x + r, y + h);
    ctx.arcTo(x, y + h, x, y + h - r, r);
    ctx.lineTo(x, y + r);
    ctx.arcTo(x, y, x + r, y, r);
    ctx.closePath();
  },

  drawImageContain(ctx, image, x, y, w, h) {
    if (!image) return;
    const iw = image.width || w;
    const ih = image.height || h;
    const scale = Math.min(w / iw, h / ih);
    const dw = iw * scale;
    const dh = ih * scale;
    const dx = x + (w - dw) / 2;
    const dy = y + (h - dh) / 2;
    ctx.drawImage(image, dx, dy, dw, dh);
  },

  // 纯测量行数（不绘制，用于需要先画背景再画文字的场景）
  measureLines(ctx, text, maxWidth) {
    let lines = 0;
    let line = '';
    for (let i = 0; i < text.length; i++) {
      const testLine = line + text[i];
      if (ctx.measureText(testLine).width > maxWidth && line.length > 0) {
        lines++;
        line = text[i];
      } else {
        line = testLine;
      }
    }
    if (line) lines++;
    return Math.max(lines, 1);
  },

  // 文字换行
  wrapText(ctx, text, x, y, maxWidth, lineHeight) {
    const words = text.split('');
    let line = '';
    let testLine = '';
    let lineArray = [];
    
    for (let i = 0; i < words.length; i++) {
      testLine += words[i];
      const metrics = ctx.measureText(testLine);
      if (metrics.width > maxWidth && i > 0) {
        lineArray.push(line);
        line = words[i];
        testLine = words[i];
      } else {
        line += words[i];
      }
    }
    lineArray.push(line);
    
    for (let j = 0; j < lineArray.length; j++) {
      ctx.fillText(lineArray[j], x, y + j * lineHeight);
    }
  },

  // ---- 快速打卡相关方法 ----

  // 检查今日打卡状态
  checkTodayStatus() {
    request({
      url: '/sign-in/api/v1/todayStatus',
      method: 'GET'
    }).then(res => {
      this.setData({
        todayCheckedIn: res.data.hasCheckedIn,
        consecutiveDays: res.data.consecutiveDays || 0
      });
    }).catch(err => {
      console.error('查询打卡状态失败:', err);
    });
  },

  // 快速打卡
  onQuickCheckIn(e) {
    const moodType = e.currentTarget.dataset.mood;
    const emojis = {1: '😊', 2: '😐', 3: '😢', 4: '😡', 5: '😴'};

    // 使用防重复点击装饰器包装
    const handler = async () => {
      try {
        wx.showLoading({ title: '打卡中...' });

        const res = await request({
          url: '/sign-in/api/v1/quickCheckIn',
          method: 'POST',
          data: {
            moodEmoji: emojis[moodType],
            moodType: moodType
          }
        });

        wx.hideLoading();
        wx.showToast({
          title: `已连续 ${res.data.consecutiveDays || 1} 天`,
          icon: 'success'
        });

        // 更新状态
        this.setData({
          todayCheckedIn: true,
          consecutiveDays: res.data.consecutiveDays || 0
        });

        // ===== 已禁用：刷新剩余次数（推广期无限使用，2026-05-21）=====
        // this.getRemainingUses();
      } catch (err) {
        wx.hideLoading();
        wx.showToast({ title: err.msg || '打卡失败', icon: 'none' });
        throw err;
      }
    };

    // 应用防重复点击
    withAntiDoubleClick(handler, this, 'isSubmittingCheckIn')(e);
  },

  // 分享给朋友
  onShareAppMessage() {
    return {
      title: '情绪急救助手：先稳住，再决定怎么回',
      path: '/pages/index/index'
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: '情绪急救助手：生成稳妥表达和边界话术',
      query: '',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // ==================== 情绪周报订阅功能 ====================
  // 微信订阅消息为一次性：每次推送前需用户重新授权
  // 按钮始终可见，状态区分"未授权"和"已授权待推送"

  // 订阅/刷新授权
  async subscribeWeeklyReport() {
    if (this.data.isSubscribingWeeklyReport) return;
    this.setData({ isSubscribingWeeklyReport: true });
    try {
      const res = await request({
        url: '/api/v1/weekly-report/template-id',
        method: 'GET'
      });
      const templateId = res.data && res.data.templateId;
      if (!templateId) {
        wx.showToast({ title: '获取模板配置失败', icon: 'none' });
        return;
      }

      wx.requestSubscribeMessage({
        tmplIds: [templateId],
        success: (subRes) => {
          if (subRes[templateId] === 'accept') {
            this.setData({ hasSubscribed: true });
            this.saveSubscriptionStatus(templateId);
            this.logEmotionEvent('subscribe_accept', { taskType: 'GENERAL' });
            wx.showToast({ title: '授权成功，周一推送下周预案', icon: 'success' });
          } else if (subRes[templateId] === 'reject') {
            wx.showToast({ title: '好的，需要时再来授权', icon: 'none' });
          }
        },
        fail: () => {
          wx.showToast({ title: '授权失败，请稍后重试', icon: 'none' });
        },
        complete: () => {
          this.setData({ isSubscribingWeeklyReport: false });
        }
      });
    } catch (err) {
      console.error('获取模板ID失败:', err);
      wx.showToast({ title: '获取模板配置失败', icon: 'none' });
      this.setData({ isSubscribingWeeklyReport: false });
    }
  },

  // 保存授权状态到后端
  saveSubscriptionStatus(templateId) {
    requestWithLogin({
      url: '/api/v1/weekly-report/subscribe',
      method: 'POST',
      data: { templateId }
    }).catch(err => {
      console.error('保存授权状态失败:', err);
    });
  },

  // 检查本周是否已授权（页面加载时调用）
  checkSubscriptionStatus() {
    requestWithLogin({
      url: '/api/v1/weekly-report/check',
      method: 'GET'
    }).then(res => {
      if (res.data && res.data.hasSubscribed) {
        this.setData({ hasSubscribed: true });
      }
    }).catch(() => {
      // 失败时不阻塞，按钮仍然可点
    });
  },

  // 跳转查看情绪周报
  goToWeeklyReport() {
    if (this.data.isNavigatingWeeklyReport) return;
    this.setData({ isNavigatingWeeklyReport: true });
    wx.navigateTo({
      url: '/pages/weekly-report/weekly-report',
      complete: () => {
        setTimeout(() => {
          this.setData({ isNavigatingWeeklyReport: false });
        }, 600);
      }
    });
  }
});
