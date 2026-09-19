// subpage1/pages/horoscope/index.js
// 每日星座运势页面逻辑（用户专属版）

const { request } = require('../../../utils/request');

const SHARE_THEMES = [
  { key: 'purple', name: '紫色', colorStart: '#0c0e30', colorMid: '#1a1d4a', colorEnd: '#2d1b69' },
  { key: 'pink', name: '粉红色', colorStart: '#9D174D', colorMid: '#F472B6', colorEnd: '#FBCFE8' }
];

Page({
  data: {
    // 当前用户的星座
    userConstellation: '',
    
    // 星座中文名称
    constellationName: '',
    
    // 星座图标
    constellationIcon: '',
    
    // 显示的日期
    displayDate: '',
    
    // 运势数据
    horoscopeData: null,
    
    // 解析后的宜忌事项
    parsedDosAndDonts: null,
    
    // 🔑 已移除：星象解码不再需要折叠状态（默认展开）
    
    // 加载状态
    loading: false,
    
    // 错误信息
    errorMsg: '',
    
    // 是否未设置星座
    notSetConstellation: false,
    
    // 🔑 是否显示滚动提示（根据屏幕高度和内容长度动态判断）
    showScrollHint: false,
    isNavigatingToSetConstellation: false,
    isSharingHoroscope: false,
    selectedShareThemeKey: 'purple'
  },

  onLoad(options) {
    // 设置显示日期
    const today = new Date();
    const dateStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    this.setData({
      displayDate: dateStr
    });

    // 从本地存储获取用户星座
    this.loadUserConstellation();
  },

  /**
   * 加载用户星座信息
   */
  loadUserConstellation() {
    // 🔑 优化：优先从本地存储读取，如果不存在则尝试从页面栈获取
    let userInfo = wx.getStorageSync('userInfo');
    
    // 如果本地存储没有，尝试从“我的”页面获取（页面栈）
    if (!userInfo || !userInfo.constellation) {
      const pages = getCurrentPages();
      if (pages.length > 0) {
        const prevPage = pages[pages.length - 1];
        if (prevPage && prevPage.data && prevPage.data.constellation) {
          userInfo = {
            constellation: prevPage.data.constellation,
            nickname: prevPage.data.nickname
          };
          // 同步到本地存储
          wx.setStorageSync('userInfo', userInfo);
          console.log('[星座运势] 从页面栈获取用户信息');
        }
      }
    }
    
    if (!userInfo || !userInfo.constellation) {
      // 未设置星座，引导用户去设置
      console.warn('[星座运势] 用户未设置星座');
      this.setData({
        notSetConstellation: true,
        loading: false
      });
      return;
    }
    
    const constellation = userInfo.constellation;
    console.log('[星座运势] 用户星座:', constellation);
    
    // 映射星座名称和图标
    const constellationMap = {
      '白羊座': { sign: 'Aries', icon: '♈' },
      '金牛座': { sign: 'Taurus', icon: '♉' },
      '双子座': { sign: 'Gemini', icon: '♊' },
      '巨蟹座': { sign: 'Cancer', icon: '♋' },
      '狮子座': { sign: 'Leo', icon: '♌' },
      '处女座': { sign: 'Virgo', icon: '♍' },
      '天秤座': { sign: 'Libra', icon: '♎' },
      '天蝎座': { sign: 'Scorpio', icon: '♏' },
      '射手座': { sign: 'Sagittarius', icon: '♐' },
      '摩羯座': { sign: 'Capricorn', icon: '♑' },
      '水瓶座': { sign: 'Aquarius', icon: '♒' },
      '双鱼座': { sign: 'Pisces', icon: '♓' }
    };
    
    const mapped = constellationMap[constellation];
    
    if (!mapped) {
      console.error('[星座运势] 未知的星座:', constellation);
      this.setData({
        notSetConstellation: true,
        loading: false
      });
      return;
    }
    
    this.setData({
      userConstellation: mapped.sign,
      constellationName: constellation,
      constellationIcon: mapped.icon
    });
    
    // 加载运势数据
    this.loadHoroscope();
  },

  /**
   * 加载星座运势数据（后端驱动版）
   */
  loadHoroscope() {
    const { userConstellation } = this.data;

    if (this.data.loading) {
      return;
    }
    
    if (!userConstellation) {
      console.error('[星座运势] 用户未设置星座');
      return;
    }
    
    this.setData({
      loading: true,
      errorMsg: '',
      horoscopeData: null
    });

    // 尝试从缓存获取
    const cacheKey = `horoscope_${this.data.displayDate}_${userConstellation}`;
    const cached = wx.getStorageSync(cacheKey);
    
    if (cached && cached.expireTime > Date.now()) {
      console.log('[星座运势] 从缓存读取数据');
      this.processHoroscopeData(cached.data);
      // 🔑 关键修复：从缓存读取后也要关闭 loading 状态
      this.setData({ loading: false });
      return;
    }

    // 🔑 关键优化：使用统一的 request 工具，自动处理 BaseURL 和 Token
    // 后端会自动从 Token 中识别用户，获取其星座并返回当日运势
    request({
      url: '/api/horoscope/my-today',
      method: 'GET'
    })
    .then((res) => {
      if (res.code === 200 || res.code === '0') {
        const data = res.data;
        
        // 存入缓存（有效期到次日凌晨）
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        tomorrow.setHours(0, 0, 0, 0);
        
        wx.setStorageSync(cacheKey, {
          data: data,
          expireTime: tomorrow.getTime()
        });
        
        console.log('[星座运势] 数据加载成功');
        this.processHoroscopeData(data);
      } else if (res.code === 400 && res.msg && res.msg.includes('未设置星座')) {
        // 后端返回未设置星座的错误
        console.warn('[星座运势] 后端提示未设置星座');
        this.setData({
          notSetConstellation: true,
          errorMsg: ''
        });
      } else {
        this.setData({
          errorMsg: res.msg || '获取运势数据失败'
        });
      }
    })
    .catch((err) => {
      console.error('[星座运势] 请求失败:', err);
      // request 工具已经显示了 Toast，这里只需更新状态
      this.setData({
        errorMsg: '网络请求失败，请检查网络连接'
      });
    })
    .finally(() => {
      this.setData({
        loading: false
      });
    });
  },

  /**
   * 处理运势数据
   */
  processHoroscopeData(data) {
    // 🔑 调试日志：打印接收到的数据
    console.log('[星座运势] 接收到的完整数据:', JSON.stringify(data));
    console.log('[星座运势] loveFortune 原始值:', data.loveFortune, '类型:', typeof data.loveFortune);
    console.log('[星座运势] wealthFortune 原始值:', data.wealthFortune, '类型:', typeof data.wealthFortune);
    console.log('[星座运势] careerFortune 原始值:', data.careerFortune, '类型:', typeof data.careerFortune);
    
    // 🔑 关键修复：确保分数是数字类型，并提供合理的默认值
    // 注意：使用 Number() 而不是 || 运算符，避免 0 被替换为 50
    const loveScore = data.loveFortune != null && data.loveFortune !== '' ? Number(data.loveFortune) : 50;
    const wealthScore = data.wealthFortune != null && data.wealthFortune !== '' ? Number(data.wealthFortune) : 50;
    const careerScore = data.careerFortune != null && data.careerFortune !== '' ? Number(data.careerFortune) : 50;
    
    console.log('[星座运势] 转换后的分数:', {
      loveFortune: loveScore,
      wealthFortune: wealthScore,
      careerFortune: careerScore
    });
    
    data.astroAnalysis = this.compactAstroAnalysis(data.astroAnalysis);

    // 更新数据对象
    data.loveFortune = loveScore;
    data.wealthFortune = wealthScore;
    data.careerFortune = careerScore;
    
    // 🔑 关键修复：在 JS 中预先计算样式类名（WXML 不支持直接调用函数）
    data.loveFortuneClass = this.getScoreClass(loveScore);
    data.wealthFortuneClass = this.getScoreClass(wealthScore);
    data.careerFortuneClass = this.getScoreClass(careerScore);
    
    console.log('[星座运势] 样式类名:', {
      loveFortuneClass: data.loveFortuneClass,
      wealthFortuneClass: data.wealthFortuneClass,
      careerFortuneClass: data.careerFortuneClass
    });
    
    // 解析宜忌事项
    const parsedDosAndDonts = this.parseDosAndDonts(data.dosAndDonts);
    
    this.setData({
      horoscopeData: data,
      parsedDosAndDonts: parsedDosAndDonts
    });
    
    // 🔑 再次验证 setData 后的数据
    console.log('[星座运势] setData 后 horoscopeData:', this.data.horoscopeData);
    
    // 🔑 延迟检测是否需要显示滚动提示（等待 DOM 渲染完成）
    setTimeout(() => {
      this.checkScrollHint();
    }, 300);
  },

  compactAstroAnalysis(text) {
    if (!text) return '';
    const cleaned = String(text).replace(/\s+/g, ' ').trim();
    if (!cleaned.includes('【核心天体】') && cleaned.length <= 90) {
      return cleaned;
    }

    const sun = cleaned.match(/太阳黄经\s*[\d.]+°（([^）]+)）/);
    const moon = cleaned.match(/月亮黄经\s*[\d.]+°（([^）]+)）/);
    const venus = cleaned.match(/金星黄经\s*[\d.]+°（([^）]+)）/);
    const moonVenus = cleaned.match(/月金夹角\s*[\d.]+°（([^）]+)）/);
    const sunMercury = cleaned.match(/日水([^（]+)（/);

    const focus = [
      sun && `太阳在${sun[1]}`,
      moon && `月亮在${moon[1]}`,
      venus && `金星在${venus[1]}`
    ].filter(Boolean).join('，');
    const emotion = moonVenus ? `月金${moonVenus[1]}，适合温和表达感受` : '今天适合把感受说得更具体';
    const communication = sunMercury ? `日水${sunMercury[1]}，回复前先确认信息` : '重要沟通先慢半拍';

    return `${focus ? `今日重点：${focus}。` : '今日重点：关注情绪和沟通节奏。'}\n情绪提示：${emotion}；${communication}。`;
  },

  /**
   * 解析宜忌事项字符串
   * 输入示例："宜：沟通、约会；忌：熬夜、争吵"
   */
  parseDosAndDonts(text) {
    if (!text) return null;
    
    const match = text.match(/宜：(.+)；忌：(.+)/);
    if (match) {
      return {
        dos: match[1].split('、'),
        donts: match[2].split('、')
      };
    }
    
    return null;
  },


  /**
   * 去设置星座
   */
  goToSetConstellation() {
    if (this.data.isNavigatingToSetConstellation) return;
    this.setData({ isNavigatingToSetConstellation: true });

    wx.navigateBack({
      success: () => {
        // 返回后触发“我的”页面的星座编辑
        const pages = getCurrentPages();
        if (pages.length > 0) {
          const prevPage = pages[pages.length - 1];
          if (prevPage && prevPage.editConstellation) {
            prevPage.editConstellation();
          }
        }
      },
      complete: () => {
        this.setData({ isNavigatingToSetConstellation: false });
      }
    });
  },

  /**
   * 根据分数获取样式类名
   */
  getScoreClass(score) {
    console.log('[星座运势] getScoreClass 输入:', score, '类型:', typeof score);
    if (score >= 71) {
      console.log('[星座运势] getScoreClass 返回: high');
      return 'high';
    }
    if (score >= 41) {
      console.log('[星座运势] getScoreClass 返回: medium');
      return 'medium';
    }
    console.log('[星座运势] getScoreClass 返回: low');
    return 'low';
  },

  /**
   * 🔑 检测是否需要显示滚动提示
   */
  checkScrollHint() {
    // 创建选择器查询节点信息
    const query = wx.createSelectorQuery();
    
    // 查询内容区域的高度和可视区域高度
    query.select('.content-area').boundingClientRect();
    query.selectViewport().scrollOffset();
    
    query.exec((res) => {
      if (!res || !res[0]) return;
      
      const contentHeight = res[0].height; // 内容总高度
      const windowHeight = wx.getSystemInfoSync().windowHeight; // 窗口高度
      
      console.log('[星座运势] 内容高度:', contentHeight, '窗口高度:', windowHeight);
      
      // 如果内容高度超过窗口高度的 85%，则显示滚动提示
      const shouldShowHint = contentHeight > windowHeight * 0.85;
      
      this.setData({
        showScrollHint: shouldShowHint
      });
      
      console.log('[星座运势] 是否显示滚动提示:', shouldShowHint);
      
      // 🔑 如果需要显示提示，则在 2 秒后自动隐藏（用户已经看到）
      if (shouldShowHint) {
        setTimeout(() => {
          this.setData({
            showScrollHint: false
          });
        }, 5000); // 5 秒后自动隐藏
      }
    });
  },

  /**
   * 🔑 页面滚动事件处理
   */
  onPageScroll(e) {
    // 用户开始滚动后，立即隐藏滚动提示
    if (this.data.showScrollHint && e.scrollTop > 10) {
      this.setData({
        showScrollHint: false
      });
    }
  },

  // ---- 分享运势卡片功能 ----

  // ✅ 检查权限并保存图片
  checkAndSavePhoto(filePath) {
    console.log('[星座运势] 开始检查相册权限...');
    
    wx.getSetting({
      success: (settingRes) => {
        console.log('[星座运势] 当前权限设置:', settingRes);
        
        const hasWritePhotosAlbumAuth = settingRes.authSetting['scope.writePhotosAlbum'];
        
        if (hasWritePhotosAlbumAuth === true) {
          // ✅ 已授权，直接保存
          console.log('[星座运势] 已有相册权限，直接保存');
          this.savePhotoToAlbum(filePath);
        } else if (hasWritePhotosAlbumAuth === false) {
          // ❌ 用户明确拒绝过，引导去设置
          console.log('[星座运势] 用户已拒绝相册权限，引导去设置');
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
                      console.log('[星座运势] 用户在设置中开启了权限，重新保存');
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
          console.log('[星座运势] 从未请求过权限，引导去设置');
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
                      console.log('[星座运势] 用户在设置中开启了权限，重新保存');
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
                    console.error('[星座运势] 打开设置失败:', err);
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
        console.error('[星座运势] 获取权限设置失败:', err);
        // 如果获取权限设置失败，直接尝试保存（兼容处理）
        this.savePhotoToAlbum(filePath);
      }
    });
  },

  // ✅ 执行保存到相册
  savePhotoToAlbum(filePath) {
    console.log('[星座运势] 执行保存到相册:', filePath);
    
    wx.saveImageToPhotosAlbum({
      filePath: filePath,
      success: () => {
        console.log('[星座运势] 保存到相册成功');
        wx.hideLoading();
        wx.showToast({ title: '已保存到相册', icon: 'success', duration: 2000 });
      },
      fail: (err) => {
        console.error('[星座运势] 保存到相册失败:', err);
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

  // 分享今日运势卡片
  shareHoroscopeCard() {
    if (this.data.isSharingHoroscope) return;
    this.setData({ isSharingHoroscope: true });

    console.log('开始生成运势卡片...');

    const { constellationName, constellationIcon, horoscopeData } = this.data;

    if (!horoscopeData) {
      wx.showToast({ title: '暂无运势数据', icon: 'none' });
      this.setData({ isSharingHoroscope: false });
      return;
    }

    this.chooseShareTheme((themeKey) => {
      if (!themeKey) {
        this.setData({ isSharingHoroscope: false });
        return;
      }
      this.setData({ selectedShareThemeKey: themeKey });
      this.generateHoroscopeShareImage(constellationName, constellationIcon, horoscopeData);
    });
  },

  chooseShareTheme(done) {
    wx.showActionSheet({
      itemList: SHARE_THEMES.map(item => item.name),
      success: (res) => {
        const theme = SHARE_THEMES[res.tapIndex];
        done(theme ? theme.key : '');
      },
      fail: () => done('')
    });
  },

  getSelectedShareTheme() {
    return SHARE_THEMES.find(item => item.key === this.data.selectedShareThemeKey) || SHARE_THEMES[0];
  },

  generateHoroscopeShareImage(constellationName, constellationIcon, horoscopeData) {
    wx.showLoading({ title: '生成中...' });
    // 使用 canvas 生成图片
    const query = wx.createSelectorQuery().in(this);
    query.select('#horoscopeShareCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) {
          wx.hideLoading();
          wx.showToast({ title: '生成失败', icon: 'none' });
          this.setData({ isSharingHoroscope: false });
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

          // 绘制星象风格背景
          const theme = this.getSelectedShareTheme();
          const gradient = ctx.createLinearGradient(0, 0, width, height);
          gradient.addColorStop(0, theme.colorStart);
          gradient.addColorStop(0.5, theme.colorMid);
          gradient.addColorStop(1, theme.colorEnd);
          ctx.fillStyle = gradient;
          ctx.fillRect(0, 0, width, height);

          // 绘制星空装饰
          this.drawStarfield(ctx, width, height);

          // 绘制内容
          this.drawHoroscopeCardContent(ctx, width, height, constellationName, constellationIcon, horoscopeData);

          // 转换为图片
          wx.canvasToTempFilePath({
            canvas: canvas,
            success: (tempRes) => {
              console.log('运势卡片生成成功:', tempRes.tempFilePath);
              wx.hideLoading();

              // ✅ 调用微信的图片分享菜单（带小程序入口）
              wx.showShareImageMenu({
                path: tempRes.tempFilePath,
                needShowEntrance: true,  // ✅ 显示小程序入口
                entrancePath: '/subpage1/pages/horoscope/index',  // ✅ 点击入口后跳转的路径
                success: () => {
                  console.log('图片分享菜单打开成功');
                },
                fail: (err) => {
                  console.error('打开图片分享菜单失败:', err);
                  wx.showToast({
                    title: '分享失败，请重试',
                    icon: 'none'
                  });
                },
                complete: () => {
                  this.setData({ isSharingHoroscope: false });
                }
              });
            },
            fail: (err) => {
              console.error('生成运势卡片失败:', err);
              wx.hideLoading();
              wx.showToast({ title: '生成失败', icon: 'none' });
              this.setData({ isSharingHoroscope: false });
            }
          });
        } catch (error) {
          console.error('绘制运势卡片出错:', error);
          wx.hideLoading();
          wx.showToast({ title: '生成异常', icon: 'none' });
          this.setData({ isSharingHoroscope: false });
        }
      });
  },

  // 绘制星空背景
  drawStarfield(ctx, width, height) {
    // 绘制大星星
    ctx.globalAlpha = 0.6;
    ctx.fillStyle = '#FFD700';
    
    for (let i = 0; i < 30; i++) {
      const x = Math.random() * width;
      const y = Math.random() * height;
      const size = Math.random() * 4 + 2;
      
      ctx.beginPath();
      ctx.arc(x, y, size, 0, 2 * Math.PI);
      ctx.fill();
    }

    // 绘制小星星
    ctx.globalAlpha = 0.3;
    ctx.fillStyle = '#FFFFFF';
    
    for (let i = 0; i < 80; i++) {
      const x = Math.random() * width;
      const y = Math.random() * height;
      const size = Math.random() * 2 + 0.5;
      
      ctx.beginPath();
      ctx.arc(x, y, size, 0, 2 * Math.PI);
      ctx.fill();
    }

    ctx.globalAlpha = 1.0;
  },

  // 绘制运势卡片内容
  drawHoroscopeCardContent(ctx, width, height, constellationName, constellationIcon, data) {
    // 绘制标题区域
    ctx.fillStyle = '#FFD700';
    ctx.font = 'bold 48px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(`${constellationIcon} ${constellationName}运势`, width / 2, 80);

    // 绘制日期
    const dateStr = data.date || new Date().toLocaleDateString('zh-CN');
    ctx.font = '28px sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.fillText(dateStr, width / 2, 130);

    // 绘制分隔线
    ctx.strokeStyle = 'rgba(255, 215, 0, 0.3)';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(80, 160);
    ctx.lineTo(width - 80, 160);
    ctx.stroke();

    let currentY = 220;

    // 绘制核心指引
    if (data.content) {
      ctx.fillStyle = '#FFD700';
      ctx.font = 'bold 36px sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText('🌟 今日指引', 80, currentY);
      currentY += 50;

      ctx.font = '28px sans-serif';
      ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
      
      const contentLines = this.wrapTextForCanvas(ctx, data.content, width - 160, 28);
      contentLines.forEach((line, index) => {
        ctx.fillText(line, 80, currentY + index * 40);
      });

      currentY += contentLines.length * 40 + 50;
    }

    // 绘制三维运势
    ctx.fillStyle = '#FFD700';
    ctx.font = 'bold 36px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('📊 三维运势', 80, currentY);
    currentY += 50;

    // 情感运势
    this.drawFortuneBar(ctx, 80, currentY, width - 160, '❤️ 情感', data.loveFortune);
    currentY += 80;

    // 财富运势
    this.drawFortuneBar(ctx, 80, currentY, width - 160, '💰 财富', data.wealthFortune);
    currentY += 80;

    // 事业运势
    this.drawFortuneBar(ctx, 80, currentY, width - 160, '💼 事业', data.careerFortune);
    currentY += 100;

    // 绘制AI暖心建议
    if (data.aiAdvice) {
      ctx.fillStyle = '#FFD700';
      ctx.font = 'bold 36px sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText('💝 AI暖心建议', 80, currentY);
      currentY += 50;

      ctx.font = '26px sans-serif';
      ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
      
      const adviceLines = this.wrapTextForCanvas(ctx, data.aiAdvice, width - 160, 26);
      adviceLines.forEach((line, index) => {
        ctx.fillText(line, 80, currentY + index * 38);
      });

      currentY += adviceLines.length * 38 + 60;
    }

    // 绘制宜忌事项
    if (data.dosAndDonts) {
      const parsed = this.parseDosAndDonts(data.dosAndDonts);
      if (parsed) {
        // 绘制分隔线
        ctx.strokeStyle = 'rgba(255, 215, 0, 0.3)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(80, currentY);
        ctx.lineTo(width - 80, currentY);
        ctx.stroke();
        currentY += 60;
    
        // 左右分栏布局
        const halfWidth = (width - 160) / 2;
        const titleY = currentY; // 标题在同一行
            
        // 绘制宜标题（左侧）
        ctx.fillStyle = '#4CAF50';
        ctx.font = 'bold 40px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText('宜', 80, titleY);
            
        // 绘制忌标题（右侧）
        ctx.fillStyle = '#FF5252';
        ctx.fillText('忌', width / 2 + 40, titleY);
            
        // 绘制宜内容
        currentY += 50;
        ctx.font = '28px sans-serif';
        ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
        const dosText = parsed.dos.join('、');
        const dosLines = this.wrapTextForCanvas(ctx, dosText, halfWidth - 20, 28);
        const dosStartY = currentY;
        dosLines.forEach((line, index) => {
          ctx.fillText(line, 80, dosStartY + index * 42);
        });
            
        // 绘制忌内容
        const dontsText = parsed.donts.join('、');
        const dontsLines = this.wrapTextForCanvas(ctx, dontsText, halfWidth - 20, 28);
        dontsLines.forEach((line, index) => {
          ctx.fillText(line, width / 2 + 40, dosStartY + index * 42);
        });
            
        // 计算最大高度
        const maxLines = Math.max(dosLines.length, dontsLines.length);
        currentY = dosStartY + maxLines * 42 + 60;
      }
    }

    // 底部品牌标识
    const bottomY = height - 80;
    ctx.fillStyle = 'rgba(255, 215, 0, 0.8)';
    ctx.font = 'bold 28px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('—— 情绪解压卡 ——', width / 2, bottomY);
  },

  // 绘制运势进度条
  drawFortuneBar(ctx, x, y, maxWidth, label, score) {
    // 标签和分数
    ctx.font = '28px sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.textAlign = 'left';
    ctx.fillText(label, x, y);

    ctx.font = 'bold 32px sans-serif';
    ctx.textAlign = 'right';
    
    // 根据分数设置颜色
    if (score >= 71) {
      ctx.fillStyle = '#FFD700';
    } else if (score >= 41) {
      ctx.fillStyle = '#F9A8C5';
    } else {
      ctx.fillStyle = '#8B5CF6';
    }
    
    ctx.fillText(`${score}`, x + maxWidth, y);

    // 进度条背景
    const barY = y + 15;
    const barHeight = 14;
    ctx.fillStyle = 'rgba(255, 255, 255, 0.2)';
    ctx.fillRect(x, barY, maxWidth, barHeight);

    // 进度条填充
    const fillWidth = (score / 100) * maxWidth;
    
    if (score >= 71) {
      ctx.fillStyle = '#FFD700';
    } else if (score >= 41) {
      ctx.fillStyle = '#F9A8C5';
    } else {
      ctx.fillStyle = '#8B5CF6';
    }
    
    ctx.fillRect(x, barY, fillWidth, barHeight);
  },

  // Canvas 文字换行工具函数
  wrapTextForCanvas(ctx, text, maxWidth, fontSize) {
    const chars = text.split('');
    let line = '';
    const lines = [];

    for (let i = 0; i < chars.length; i++) {
      const testLine = line + chars[i];
      const metrics = ctx.measureText(testLine);
      
      if (metrics.width > maxWidth && i > 0) {
        lines.push(line);
        line = chars[i];
      } else {
        line = testLine;
      }
    }
    lines.push(line);

    return lines;
  }
});
