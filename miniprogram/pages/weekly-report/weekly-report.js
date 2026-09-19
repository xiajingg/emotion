// pages/weekly-report/weekly-report.js
const { requestWithLogin } = require('../../utils/request');

const UNIVERSE_TIPS = [
  '顺其自然，会有好事发生。',
  '你比自己想象的更强大。',
  '允许一切发生，你只管温柔而坚定。',
  '今天的不开心到此为止，明天依旧光芒万丈。',
  '内心丰盈者，独行也如众。',
  '所有的遗憾都是惊喜的铺垫。',
  '月亮在慢慢变圆，事情在慢慢变好。'
];

Page({
  data: {
    loading: true,
    hasData: false,

    // 基础信息
    startDate: '', endDate: '', dateRange: '',

    // 核心评分
    averageScore: 0, emotionState: '', mainTag: '',
    recordCount: 0, checkInDays: 0, scoreRank: 0,

    // 情绪成分
    emotionComposition: {}, compositionSummary: '',
    compositionList: [],  // 处理后的列表 [{label, pct, width}]

    // 趋势分析
    dailyScores: [], trendDirection: '', trendClass: '', trendDescription: '',
    turningPoints: [],

    // 高光与低谷
    highPoint: null, lowPoint: null,

    // 周环比
    weekComparison: {},

    // 情绪触发点
    triggers: [],

    // AI 洞察与建议
    aiCommentary: '', aiInsight: '', aiSuggestion: '',
    suggestionList: [],

    // 下周预测
    nextWeekForecast: {},
    forecastRiskColor: '',

    // 成就徽章
    badges: [],

    // 宇宙提示
    universeTip: '',

    // 订阅状态
    hasSubscribed: false,      // 本周是否已有有效授权
    hasEverSubscribed: false,  // 历史上是否订阅过（区分首次/再次）
    showSubscribeBtn: true,

    // 海报
    isGeneratingPoster: false,
    isSwitchingWeek: false,
    isSubscribingWeeklyReport: false
  },

  onLoad(options) {
    let { startDate, endDate } = options;
    // 没有参数时默认上周（上周一～上周日）
    if (!startDate || !endDate) {
      const now = new Date();
      const dayOfWeek = now.getDay(); // 0=周日
      // 上周一 = 本周一 - 7 天
      const thisMonday = new Date(now);
      thisMonday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
      const lastMonday = new Date(thisMonday);
      lastMonday.setDate(thisMonday.getDate() - 7);
      const lastSunday = new Date(lastMonday);
      lastSunday.setDate(lastMonday.getDate() + 6);
      startDate = this.formatDate(lastMonday);
      endDate = this.formatDate(lastSunday);
    }
    this.setData({ startDate, endDate });
    this.loadReportData(startDate, endDate);
    this.randomUniverseTip();
    this.checkSubscriptionStatus();
    this.logWeeklyEvent('weekly_report_open');
  },

  logWeeklyEvent(eventType) {
    requestWithLogin({
      url: '/emotion/api/v1/events',
      method: 'POST',
      data: {
        eventType,
        taskType: 'GENERAL',
        extra: `${this.data.startDate || ''}_${this.data.endDate || ''}`
      }
    }).catch(err => {
      console.warn('[weekly event] 记录失败:', eventType, err);
    });
  },

  // 切换上一周
  goToPrevWeek() {
    if (this.data.isSwitchingWeek || this.data.loading) return;
    const prev = this.weekOffset(-7);
    this.setData({ startDate: prev.start, endDate: prev.end, loading: true, isSwitchingWeek: true });
    this.loadReportData(prev.start, prev.end).finally(() => {
      this.setData({ isSwitchingWeek: false });
    });
  },

  // 切换下一周（不能滑到本周或未来）
  goToNextWeek() {
    if (this.data.isSwitchingWeek || this.data.loading) return;
    const next = this.weekOffset(7);
    // 计算本周一，不允许切换到 >= 本周一的周
    const now = new Date();
    const dayOfWeek = now.getDay();
    const thisMonday = new Date(now);
    thisMonday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
    const thisMondayStr = this.formatDate(thisMonday);
    if (next.start >= thisMondayStr) {
      wx.showToast({ title: '本周还未结束，暂无周报数据', icon: 'none', duration: 2000 });
      return;
    }
    this.setData({ startDate: next.start, endDate: next.end, loading: true, isSwitchingWeek: true });
    this.loadReportData(next.start, next.end).finally(() => {
      this.setData({ isSwitchingWeek: false });
    });
  },

  // 是否可以切换到下一周（用于 UI 判断箭头是否可点）
  canGoNext() {
    const { startDate } = this.data;
    if (!startDate) return false;
    const now = new Date();
    const dayOfWeek = now.getDay();
    const thisMonday = new Date(now);
    thisMonday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
    const next = this.weekOffset(7);
    return next.start < this.formatDate(thisMonday);
  },

  weekOffset(days) {
    const { startDate } = this.data;
    const base = new Date(startDate.replace(/-/g, '/'));
    base.setDate(base.getDate() + days);
    const end = new Date(base);
    end.setDate(base.getDate() + 6);
    return { start: this.formatDate(base), end: this.formatDate(end) };
  },

  formatDate(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  // 趋势方向中文 → 英文 CSS 类名映射（WXSS 不支持中文选择器）
  mapTrendClass(dir) {
    const map = { '上升': 'up', '下降': 'down', '波动': 'wave', '平稳': 'stable', '数据不足': 'nodata' };
    return map[dir] || 'nodata';
  },

  async loadReportData(startDate, endDate) {
    try {
      let url = '/api/v1/weekly-report/detail';
      if (startDate && endDate) url += `?startDate=${startDate}&endDate=${endDate}`;
      const res = await requestWithLogin({ url, method: 'GET' });
      const d = res.data;
      if (!d) {
        this.setData({ loading: false });
        return;
      }
      const hasData = d.recordCount > 0;

      // 处理情绪成分列表
      let compositionList = [];
      if (d.emotionComposition) {
        compositionList = Object.keys(d.emotionComposition).map(k => ({
          label: k,
          pct: Math.round(d.emotionComposition[k] * 100),
          width: Math.round(d.emotionComposition[k] * 100) + '%'
        }));
      }

      // 处理建议换行
      let suggestionList = [];
      if (d.aiSuggestion) {
        suggestionList = d.aiSuggestion.split('\n').filter(line => line.trim());
      }

      // 预测风险颜色
      let forecastRiskColor = '';
      if (d.nextWeekForecast && d.nextWeekForecast.riskLevel === 'high') forecastRiskColor = '#FF6B6B';
      else if (d.nextWeekForecast && d.nextWeekForecast.riskLevel === 'medium') forecastRiskColor = '#FFA726';
      else forecastRiskColor = '#66BB6A';

      this.setData({
        loading: false, hasData,
        startDate: d.startDate || startDate, endDate: d.endDate || endDate,
        dateRange: d.dateRange || '',
        averageScore: Math.round(d.averageScore || 0),
        emotionState: d.emotionState || '', mainTag: d.mainTag || '',
        recordCount: d.recordCount || 0, checkInDays: d.checkInDays || 0,
        scoreRank: d.scoreRank || 0,
        emotionComposition: d.emotionComposition || {},
        compositionSummary: d.compositionSummary || '',
        compositionList,
        dailyScores: d.dailyScores || [],
        trendDirection: d.trendDirection || '',
        trendClass: this.mapTrendClass(d.trendDirection),
        trendDescription: d.trendDescription || '',
        turningPoints: d.turningPoints || [],
        highPoint: d.highPoint || null,
        lowPoint: d.lowPoint || null,
        weekComparison: d.weekComparison || {},
        triggers: d.triggers || [],
        aiCommentary: d.aiCommentary || '',
        aiInsight: d.aiInsight || '',
        aiSuggestion: d.aiSuggestion || '',
        suggestionList,
        nextWeekForecast: d.nextWeekForecast || {},
        forecastRiskColor,
        badges: d.badges || [],
        events: d.events || []
      });

    } catch (err) {
      console.error('加载周报失败:', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
      this.setData({ loading: false });
    }
  },

  // ==================== 宇宙提示 ====================

  randomUniverseTip() {
    const idx = Math.floor(Math.random() * UNIVERSE_TIPS.length);
    this.setData({ universeTip: UNIVERSE_TIPS[idx] });
  },

  // ==================== 海报（Canvas 2D + 原生分享菜单）====================

  async handleShareCard() {
    if (this.data.isGeneratingPoster) return;
    this.setData({ isGeneratingPoster: true });
    wx.showLoading({ title: '生成海报中...', mask: true });

    try {
      const posterPath = await this.generatePoster();
      wx.hideLoading();
      this.setData({ isGeneratingPoster: false });
      if (!posterPath) return;
      wx.showShareImageMenu({
        path: posterPath,
        needShowEntrance: true,
        entrancePath: '/pages/index/index'
      });
    } catch (err) {
      console.error('生成海报失败:', err);
      wx.hideLoading();
      this.setData({ isGeneratingPoster: false });
      wx.showToast({ title: '生成失败，请重试', icon: 'none' });
    }
  },

  generatePoster() {
    return new Promise((resolve, reject) => {
      const query = wx.createSelectorQuery().in(this);
      query.select('#posterCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          if (!res || !res[0] || !res[0].node) {
            reject(new Error('Canvas节点未找到'));
            return;
          }
          try {
            const canvas = res[0].node;
            const ctx = canvas.getContext('2d');
            const dpr = wx.getSystemInfoSync().pixelRatio;
            const w = 375, h = 700;
            canvas.width = w * dpr;
            canvas.height = h * dpr;
            ctx.scale(dpr, dpr);

            this.drawPosterContent(ctx, w, h);

            wx.canvasToTempFilePath({
              canvas: canvas,
              success: r => {
                console.log('海报生成成功:', r.tempFilePath);
                resolve(r.tempFilePath);
              },
              fail: reject
            });
          } catch (err) {
            reject(err);
          }
        });
    });
  },

  drawPosterContent(ctx, w, h) {
    const { dateRange, mainTag, recordCount, checkInDays, aiCommentary, emotionState } = this.data;

    // 美化日期：2026-05-11至2026-05-17 → 05.11 - 05.17
    let dateDisplay = dateRange;
    if (dateRange && dateRange.includes('至')) {
      const parts = dateRange.split('至');
      if (parts.length === 2) {
        const d1 = parts[0].substring(5).replace('-', '.');
        const d2 = parts[1].substring(5).replace('-', '.');
        dateDisplay = d1 + ' - ' + d2;
      }
    }

    // 背景渐变
    const bg = ctx.createLinearGradient(0, 0, 0, h);
    bg.addColorStop(0, '#7B68EE');
    bg.addColorStop(0.4, '#9B7CFA');
    bg.addColorStop(1, '#C4B5FD');
    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, w, h);

    // 装饰圆
    ctx.fillStyle = 'rgba(255,255,255,0.08)';
    ctx.beginPath(); ctx.arc(w - 40, 70, 130, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(30, 200, 90, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = 'rgba(255,255,255,0.05)';
    ctx.beginPath(); ctx.arc(w - 60, h - 120, 110, 0, Math.PI * 2); ctx.fill();

    // 标题区
    ctx.fillStyle = '#FFF';
    ctx.font = 'bold 24px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('📖 下周情绪预案', w / 2, 55);
    ctx.font = '13px sans-serif';
    ctx.fillStyle = 'rgba(255,255,255,0.75)';
    ctx.fillText(dateDisplay, w / 2, 82);

    // ===== 核心数据卡片 =====
    const card1Y = 105;
    const card1H = 155;

    // 卡片阴影 + 白色卡片
    ctx.fillStyle = 'rgba(244, 143, 177,0.1)';
    this.roundRect(ctx, 26, card1Y + 3, w - 52, card1H, 20);
    ctx.fill();
    ctx.fillStyle = 'rgba(255,255,255,0.97)';
    this.roundRect(ctx, 26, card1Y, w - 52, card1H, 20);
    ctx.fill();

    // 小标签
    ctx.fillStyle = '#F9A8C5';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('◆  本周情绪概览  ◆', w / 2, card1Y + 28);

    // 主标签（大字）
    ctx.fillStyle = '#F48FB1';
    ctx.font = 'bold 34px sans-serif';
    ctx.fillText(mainTag || '心情记录', w / 2, card1Y + 74);

    // 统计
    ctx.fillStyle = '#888';
    ctx.font = '13px sans-serif';
    ctx.fillText('记录 ' + recordCount + ' 次  ·  打卡 ' + checkInDays + ' 天', w / 2, card1Y + 108);

    // 情绪状态
    if (emotionState) {
      ctx.fillStyle = '#F9A8C5';
      ctx.font = '12px sans-serif';
      ctx.fillText(emotionState, w / 2, card1Y + 132);
    }

    // ===== AI 点评卡片（动态高度，先算行数再画）=====
    const cardGap = 18;
    let card2Y = card1Y + card1H + cardGap;

    if (aiCommentary) {
      const lineCount = this.countWrapLines(ctx, aiCommentary, w - 96, 13);
      const textHeight = lineCount * 20;
      const card2H = Math.max(120, textHeight + 70);

      // 阴影 + 白色卡片
      ctx.fillStyle = 'rgba(244, 143, 177,0.08)';
      this.roundRect(ctx, 26, card2Y + 3, w - 52, card2H, 20);
      ctx.fill();
      ctx.fillStyle = 'rgba(255,255,255,0.97)';
      this.roundRect(ctx, 26, card2Y, w - 52, card2H, 20);
      ctx.fill();

      // 标题
      ctx.fillStyle = '#F48FB1';
      ctx.font = 'bold 13px sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText('💡 AI 点评', 46, card2Y + 30);

      // 分隔线
      ctx.fillStyle = '#F0EDFF';
      ctx.fillRect(46, card2Y + 42, w - 92, 1);

      // 点评文字
      ctx.fillStyle = '#444';
      ctx.font = '13px sans-serif';
      this.wrapText(ctx, aiCommentary, 46, card2Y + 62, w - 92, 20);
    }

    // 底部
    ctx.fillStyle = 'rgba(255,255,255,0.6)';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('✨ 来自 情绪急救助手 · 下周预案', w / 2, h - 30);

    // 底部装饰线
    ctx.fillStyle = 'rgba(255,255,255,0.25)';
    ctx.fillRect(w / 2 - 25, h - 16, 50, 2);
  },

  countWrapLines(ctx, text, maxW, fontSize) {
    if (!text) return 0;
    ctx.font = fontSize + 'px sans-serif';
    let line = '', count = 1;
    for (let i = 0; i < text.length; i++) {
      const tl = line + text[i];
      if (ctx.measureText(tl).width > maxW && line) {
        count++;
        line = text[i];
      } else {
        line = tl;
      }
    }
    return count;
  },

  roundRect(ctx, x, y, w, h, r) {
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

  wrapText(ctx, text, x, y, maxW, lineH) {
    if (!text) return;
    let line = '', cy = y;
    for (let i = 0; i < text.length; i++) {
      const tl = line + text[i];
      if (ctx.measureText(tl).width > maxW && line) {
        ctx.fillText(line, x, cy);
        line = text[i];
        cy += lineH;
      } else {
        line = tl;
      }
    }
    if (line) ctx.fillText(line, x, cy);
  },

  // ==================== 情绪周报订阅 ====================

  checkSubscriptionStatus() {
    requestWithLogin({
      url: '/api/v1/weekly-report/check',
      method: 'GET'
    }).then(res => {
      if (res.data) {
        this.setData({
          hasSubscribed: res.data.hasSubscribed || false,
          hasEverSubscribed: res.data.hasEverSubscribed || false,
          showSubscribeBtn: !res.data.hasSubscribed
        });
      }
    }).catch(() => {
      // 失败不阻塞
    });
  },

  async subscribeWeeklyReport() {
    if (this.data.isSubscribingWeeklyReport) return;
    this.setData({ isSubscribingWeeklyReport: true });
    try {
      const res = await requestWithLogin({
        url: '/api/v1/weekly-report/template-id',
        method: 'GET'
      });
      const templateId = res.data && res.data.templateId;
      if (!templateId) {
        wx.showToast({ title: '获取模板配置失败', icon: 'none' });
        this.setData({ isSubscribingWeeklyReport: false });
        return;
      }
      wx.requestSubscribeMessage({
        tmplIds: [templateId],
        success: (subRes) => {
          if (subRes[templateId] === 'accept') {
            this.setData({ hasSubscribed: true, hasEverSubscribed: true });
            this.saveSubscriptionStatus(templateId);
            this.logWeeklyEvent('subscribe_accept');
            wx.showToast({ title: '授权成功，下周预案推送给你', icon: 'success' });
          } else if (subRes[templateId] === 'reject') {
            wx.showToast({ title: '好的，需要时再来授权', icon: 'none' });
          }
        },
        fail: () => {
          wx.showToast({ title: '订阅失败，请重试', icon: 'none' });
        },
        complete: () => {
          this.setData({ isSubscribingWeeklyReport: false });
        }
      });
    } catch (err) {
      console.error('订阅失败:', err);
      this.setData({ isSubscribingWeeklyReport: false });
    }
  },

  saveSubscriptionStatus(templateId) {
    requestWithLogin({
      url: '/api/v1/weekly-report/subscribe',
      method: 'POST',
      data: { templateId }
    }).catch(err => {
      console.error('保存授权状态失败:', err);
    });
  },

  // ==================== 分享 ====================

  onShareAppMessage() {
    const { startDate, endDate, averageScore, mainTag } = this.data;
    return {
      title: `下周情绪预案：${mainTag} · ${averageScore}分，查看触发点和应对话术`,
      path: `/pages/weekly-report/weekly-report?startDate=${startDate}&endDate=${endDate}`
    };
  }
});
