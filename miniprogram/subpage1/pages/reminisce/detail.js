const { request } = require('../../../utils/request');
const FILE_VIEW_BASE_URL = 'https://www.onekey-ai.top/files/view?id=';
const SHARE_THEMES = [
  { key: 'purple', name: '紫色', colorStart: '#0c0c2d', colorMid: '#1a1040', colorEnd: '#2d1b69' },
  { key: 'pink', name: '粉红色', colorStart: '#9D174D', colorMid: '#F472B6', colorEnd: '#FBCFE8' }
];

Page({
  data: {
    id: '',
    loading: true,
    detail: null,
    images: [],
    isDoodle: false,
    reliefActions: [],
    replyItems: [],
    activeReplyIndex: 0,
    activeReplyText: '',
    source: '',
    selectedShareThemeKey: 'purple',
    isSharingCard: false,
    isGeneratingPoster: false
  },

  onLoad(options) {
    const id = options && options.id;
    if (!id) {
      wx.showToast({ title: '记录不存在', icon: 'none' });
      this.setData({ loading: false });
      return;
    }
    this.setData({ id, source: options.source || '' });
    this.loadDetail(id);
  },

  loadDetail(id) {
    this.setData({ loading: true });
    const url = this.data.source === 'friend'
      ? `/api/friend/timeline/detail?id=${id}`
      : `/user/api/v1/historySubmit/detail?id=${id}`;
    request({
      url,
      method: 'GET'
    }).then(res => {
      const detail = res.data || {};
      const images = this.normalizeImages(detail);
      const replies = detail.replies || {};
      const replyItems = [
        { label: '高情商', text: replies.high_eq || '' },
        { label: '发疯文学', text: replies.crazy || '' },
        { label: '温柔版', text: replies.gentle || '' },
        { label: '阴阳怪气版', text: replies.sarcastic || '' }
      ].filter(item => !!item.text);

      this.setData({
        detail,
        images,
        isDoodle: !!detail.doodle,
        reliefActions: detail.reliefActions || [],
        replyItems,
        activeReplyIndex: 0,
        activeReplyText: replyItems[0] ? replyItems[0].text : '',
        loading: false
      });
    }).catch(err => {
      console.error('[history detail] 获取详情失败:', err);
      this.setData({ loading: false });
      wx.showToast({ title: '详情加载失败', icon: 'none' });
    });
  },

  normalizeImages(detail) {
    const ids = detail.imgId || [];
    if (ids.length > 0) {
      return ids.map(id => `${FILE_VIEW_BASE_URL}${id}`);
    }
    const images = detail.imageUrls || [];
    const inputText = detail.inputText || '';
    if (detail.doodle && /^https?:\/\//i.test(inputText)) {
      return [inputText];
    }
    return images;
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.src;
    if (!current || this.data.images.length === 0) return;
    wx.previewImage({
      current,
      urls: this.data.images
    });
  },

  switchReplyTab(e) {
    const index = Number(e.currentTarget.dataset.index || 0);
    const item = this.data.replyItems[index] || {};
    this.setData({
      activeReplyIndex: index,
      activeReplyText: item.text || ''
    });
  },

  getSelectedTheme() {
    return SHARE_THEMES.find(item => item.key === this.data.selectedShareThemeKey) || SHARE_THEMES[0];
  },

  async handleShareCard() {
    if (this.data.isSharingCard) return;
    this.setData({ isSharingCard: true });
    const themeKey = await this.chooseShareTheme();
    if (!themeKey) {
      this.setData({ isSharingCard: false });
      return;
    }
    this.setData({ selectedShareThemeKey: themeKey });
    const posterPath = await this.generateSharePoster();
    if (!posterPath) {
      this.setData({ isSharingCard: false });
      return;
    }
    wx.showShareImageMenu({
      path: posterPath,
      needShowEntrance: true,
      entrancePath: '/pages/index/index',
      complete: () => {
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

  generateSharePoster() {
    if (this.data.isGeneratingPoster) return Promise.resolve(null);
    this.setData({ isGeneratingPoster: true });
    wx.showLoading({ title: '正在生成卡片...', mask: true });

    return new Promise((resolve) => {
      const query = wx.createSelectorQuery().in(this);
      query.select('#historyShareCanvas')
        .fields({ node: true, size: true })
        .exec(async (res) => {
          try {
            if (!res || !res[0] || !res[0].node) {
              throw new Error('Canvas节点未找到');
            }
            const canvas = res[0].node;
            const ctx = canvas.getContext('2d');
            const dpr = wx.getSystemInfoSync().pixelRatio;
            const width = 750;
            const height = 1100;
            canvas.width = width * dpr;
            canvas.height = height * dpr;
            ctx.scale(dpr, dpr);
            const image = await this.loadCanvasImage(canvas, this.data.images[0]);
            this.drawSharePoster(ctx, width, height, image);
            wx.canvasToTempFilePath({
              canvas,
              success: (tempRes) => {
                wx.hideLoading();
                this.setData({ isGeneratingPoster: false });
                resolve(tempRes.tempFilePath);
              },
              fail: () => {
                wx.hideLoading();
                this.setData({ isGeneratingPoster: false });
                wx.showToast({ title: '生成失败，请重试', icon: 'none' });
                resolve(null);
              }
            });
          } catch (err) {
            console.error('历史分享卡生成失败:', err);
            wx.hideLoading();
            this.setData({ isGeneratingPoster: false });
            wx.showToast({ title: '生成失败，请重试', icon: 'none' });
            resolve(null);
          }
        });
    });
  },

  loadCanvasImage(canvas, src) {
    return new Promise((resolve) => {
      if (!src || !canvas || !canvas.createImage) {
        resolve(null);
        return;
      }
      const image = canvas.createImage();
      image.onload = () => resolve(image);
      image.onerror = () => resolve(null);
      image.src = src;
    });
  },

  drawSharePoster(ctx, width, height, image) {
    const detail = this.data.detail || {};
    const theme = this.getSelectedTheme();
    const gradient = ctx.createLinearGradient(0, 0, width, height);
    gradient.addColorStop(0, theme.colorStart);
    gradient.addColorStop(0.55, theme.colorMid);
    gradient.addColorStop(1, theme.colorEnd);
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);

    ctx.fillStyle = 'rgba(255,255,255,0.13)';
    ctx.beginPath();
    ctx.arc(width - 120, 120, 210, 0, Math.PI * 2);
    ctx.fill();

    const left = 58;
    const cardW = width - 116;
    let y = 82;
    ctx.textAlign = 'center';
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 42px sans-serif';
    ctx.fillText('情绪解压卡', width / 2, y);
    y += 54;
    ctx.font = '22px sans-serif';
    ctx.fillStyle = 'rgba(255,255,255,0.76)';
    ctx.fillText(detail.createTime || '', width / 2, y);
    y += 52;

    if (image) {
      this.drawRoundRect(ctx, left, y, cardW, 250, 22);
      ctx.fillStyle = '#FFFFFF';
      ctx.fill();
      this.drawImageContain(ctx, image, left + 18, y + 18, cardW - 36, 214);
      y += 286;
    }

    this.drawRoundRect(ctx, left, y, cardW, 360, 24);
    ctx.fillStyle = 'rgba(255,255,255,0.94)';
    ctx.fill();
    ctx.textAlign = 'left';
    ctx.fillStyle = theme.colorStart;
    ctx.font = 'bold 30px sans-serif';
    ctx.fillText('我现在怎么了', left + 34, y + 52);
    ctx.fillStyle = '#24141B';
    ctx.font = 'bold 46px sans-serif';
    ctx.fillText(detail.emotion || '心情记录', left + 34, y + 112);
    ctx.fillStyle = '#5C4A52';
    ctx.font = '28px sans-serif';
    this.wrapText(ctx, detail.aiComment || detail.reminder || '这条记录值得被好好看见。', left + 34, y + 166, cardW - 68, 40, 4);
    y += 400;

    this.drawRoundRect(ctx, left, y, cardW, 126, 22);
    ctx.fillStyle = 'rgba(255,255,255,0.16)';
    ctx.fill();
    ctx.textAlign = 'center';
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 34px sans-serif';
    ctx.fillText(detail.answerBook || '顺其自然', width / 2, y + 58);
    ctx.font = '22px sans-serif';
    ctx.fillStyle = 'rgba(255,255,255,0.68)';
    ctx.fillText('每一次情绪都有自己的入口', width / 2, y + 94);

    ctx.fillStyle = 'rgba(255,255,255,0.7)';
    ctx.font = '20px sans-serif';
    ctx.fillText('—— 情绪解压卡 ——', width / 2, height - 74);
  },

  drawRoundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
  },

  drawImageContain(ctx, image, x, y, w, h) {
    const ratio = Math.min(w / image.width, h / image.height);
    const dw = image.width * ratio;
    const dh = image.height * ratio;
    ctx.drawImage(image, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh);
  },

  wrapText(ctx, text, x, y, maxWidth, lineHeight, maxLines) {
    const chars = String(text || '').split('');
    let line = '';
    let lineCount = 0;
    for (let i = 0; i < chars.length; i += 1) {
      const testLine = line + chars[i];
      if (ctx.measureText(testLine).width > maxWidth && line) {
        lineCount += 1;
        if (maxLines && lineCount >= maxLines) {
          ctx.fillText(line + '...', x, y);
          return;
        }
        ctx.fillText(line, x, y);
        line = chars[i];
        y += lineHeight;
      } else {
        line = testLine;
      }
    }
    if (line) ctx.fillText(line, x, y);
  },

  onShareAppMessage() {
    const detail = this.data.detail || {};
    return {
      title: `我生成了一张情绪解压卡 · ${detail.emotion || '心情记录'}`,
      path: `/pages/index/index`
    };
  },

  onShareTimeline() {
    const detail = this.data.detail || {};
    return {
      title: `情绪解压卡 · ${detail.emotion || '整理这一刻'}`,
      query: ''
    };
  }
});
