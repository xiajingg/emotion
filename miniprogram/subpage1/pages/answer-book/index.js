// pages/answer-book/index.js
const { request } = require('../../../utils/request.js');
const { withAntiDoubleClick } = require('../../../utils/util');

const SHARE_THEMES = [
  { key: 'purple', name: '紫色', colorStart: '#1a1a2e', colorMid: '#16213e', colorEnd: '#0f3460' },
  { key: 'pink', name: '粉红色', colorStart: '#9D174D', colorMid: '#F472B6', colorEnd: '#FBCFE8' }
];

Page({
  data: {
    question: '',
    remainingCount: 0,
    loading: false,
    showBookAnimation: false,  // 翻书动画显示状态
    showAnswer: false,
    randomAnswer: '',
    aiExplanation: '',
    aiLoading: false,  // AI解析加载状态
    recordId: null,  // 记录ID
    typingTimer: null,  // 打字机定时器
    isAskingQuestion: false,  // 防重复点击标志
    isSharingCard: false,
    selectedShareThemeKey: 'purple',
    isNavigatingHistory: false,
    bookAnimationText: '正在翻阅答案之书...'
  },

  onLoad() {
    // ===== 已禁用：加载剩余次数（推广期无限使用，2026-05-21）=====
    // this.loadRemainingCount();
  },

  onShow() {
    // ===== 已禁用：加载剩余次数（推广期无限使用，2026-05-21）=====
    // this.loadRemainingCount();
  },

  // ===== 已禁用：加载剩余次数（推广期无限使用，2026-05-21）=====
  // loadRemainingCount() {
  //   request({
  //     url: '/user/getRemaining',
  //     method: 'GET'
  //   }).then(res => {
  //     if (res.data) {
  //       const total = res.data.total || 0;
  //       const daily = res.data.daily || 0;
  //       // 优先显示每日剩余，如果每日用完则显示总剩余
  //       this.setData({
  //         remainingCount: daily > 0 ? daily : total
  //       });
  //     }
  //   }).catch(err => {
  //     console.error('获取剩余次数失败', err);
  //   });
  // },

  // 问题输入
  onQuestionInput(e) {
    this.setData({
      question: e.detail.value
    });
  },

  // 提问
  onAskQuestion() {
    const { question, loading } = this.data;

    // 校验
    if (!question || question.trim().length === 0) {
      wx.showToast({
        title: '请输入问题',
        icon: 'none'
      });
      return;
    }

    if (question.length < 5) {
      wx.showToast({
        title: '问题至少5个字',
        icon: 'none'
      });
      return;
    }

    if (loading) {
      return;
    }

    // 使用防重复点击装饰器包装
    const handler = async () => {
      try {
        // 开始提问 - 显示翻书动画
        this.setData({ 
          loading: true,
          showBookAnimation: true,
          showAnswer: false,
          randomAnswer: '',
          aiExplanation: '',
          aiLoading: false,
          bookAnimationText: '心里默念问题，书页正在回应...'
        });

        // 第一步：快速获取预设答案（1秒超时）
        await this.fetchRandomAnswer(question.trim());
      } catch (error) {
        console.error('[答案之书] 提问失败:', error);
        throw error;
      }
    };

    // 应用防重复点击
    withAntiDoubleClick(handler, this, 'isAskingQuestion')();
  },

  // 第一步：获取预设答案
  fetchRandomAnswer(question) {
    const animationStart = Date.now();
    return request({
      url: '/answer-book/api/v1/get-random-answer',
      method: 'POST',
      data: {
        question: question
      }
    }).then(async res => {
      if (res.data && res.data.randomAnswer) {
        this.setData({
          aiLoading: true,
          bookAnimationText: '答案已经出现，正在补上一点解释...'
        });
        this.fetchAiExplanation(question, res.data.randomAnswer, res.data.id);

        const elapsed = Date.now() - animationStart;
        if (elapsed < 3000) {
          await this.delay(Math.max(0, 2400 - elapsed));
        }

        // 隐藏翻书动画
        this.setData({ showBookAnimation: false });

        // 立即显示预设答案
        this.setData({
          showAnswer: true,
          randomAnswer: res.data.randomAnswer,
          loading: false,
          recordId: res.data.id  // 保存记录ID，用于第二步
        });

        // 滚动到答案区域
        wx.pageScrollTo({
          selector: '.answer-section',
          duration: 300
        });
      } else {
        throw new Error('暂无答案，请重试');
      }
    }).catch(err => {
      this.setData({ 
        showBookAnimation: false,
        loading: false 
      });

      wx.showToast({
        title: err.message || '获取答案失败，请重试',
        icon: 'none',
        duration: 2000
      });
    });
  },

  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  },

  // 第二步：获取AI解读
  fetchAiExplanation(question, randomAnswer, recordId) {
    request({
      url: '/answer-book/api/v1/get-ai-explanation',
      method: 'POST',
      data: {
        id: recordId,
        question: question,
        randomAnswer: randomAnswer
      }
    }).then(res => {
      // 隐藏加载状态
      this.setData({ aiLoading: false });
      
      if (res.data) {
        // 以打字机效果显示AI解读
        this.startTypingEffect(res.data);
      }
    }).catch(err => {
      console.error('获取AI解读失败', err);
      // 隐藏加载状态
      this.setData({ aiLoading: false });
      // AI失败不影响主流程，显示默认提示
      this.setData({
        aiExplanation: '这句话在告诉你，相信自己的内心，答案就在其中。无论前方如何，都要保持信心和勇气。'
      });
    });
  },

  // 打字机效果
  startTypingEffect(text) {
    let index = 0;
    const speed = 50; // 每个字符的显示速度（毫秒）
    
    // 清除之前的定时器
    if (this.data.typingTimer) {
      clearInterval(this.data.typingTimer);
    }

    const timer = setInterval(() => {
      // 检查页面是否还存在
      if (!this.setData) {
        clearInterval(timer);
        return;
      }

      if (index < text.length) {
        this.setData({
          aiExplanation: text.substring(0, index + 1)
        });
        index++;
      } else {
        clearInterval(timer);
        this.setData({ typingTimer: null });
      }
    }, speed);

    this.setData({ typingTimer: timer });
  },

  // 查看历史记录
  onShowHistory() {
    if (this.data.isNavigatingHistory) return;
    this.setData({ isNavigatingHistory: true });

    wx.navigateTo({
      url: '/subpage1/pages/answer-book/history',
      complete: () => {
        this.setData({ isNavigatingHistory: false });
      }
    });
  },

  // 页面隐藏时清理定时器
  onHide() {
    if (this.data.typingTimer) {
      clearInterval(this.data.typingTimer);
      this.setData({ typingTimer: null });
    }
  },

  // 页面卸载时清理定时器
  onUnload() {
    if (this.data.typingTimer) {
      clearInterval(this.data.typingTimer);
      this.setData({ typingTimer: null });
    }
  },

  // 分享给朋友
  onShareAppMessage() {
    return {
      title: '答案之书 - 给压力和焦虑一个温柔提示',
      path: '/subpage1/pages/answer-book/index',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: '答案之书：情绪低落时的一句解压提示',
      query: '',
      imageUrl: '' // 可以设置自定义分享图片
    };
  },

  // ---- 分享答案卡片功能 ----

  // 处理分享卡片按钮点击（核心逻辑）
  handleShareCard() {
    if (this.data.isSharingCard) return;
    this.setData({ isSharingCard: true });

    // 判断是否已经有答案
    if (!this.data.randomAnswer) {
      // 未提问：滚动到输入框 + 提示
      this.scrollToInput(() => {
        this.setData({ isSharingCard: false });
      });
      setTimeout(() => {
        wx.showToast({
          title: '请先翻开答案之书，再生成专属卡片',
          icon: 'none',
          duration: 2500
        });
      }, 800);
    } else {
      // 已有答案：直接生成并分享
      this.chooseShareTheme((themeKey) => {
        if (!themeKey) {
          this.setData({ isSharingCard: false });
          return;
        }
        this.setData({ selectedShareThemeKey: themeKey });
        this.shareAnswerCard(() => {
          this.setData({ isSharingCard: false });
        });
      });
    }
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

  // 滚动到输入框
  scrollToInput(done) {
    wx.pageScrollTo({
      selector: '.input-section',
      duration: 300,
      success: () => {
        setTimeout(() => {
          wx.showToast({
            title: '请在上方写下你的困惑',
            icon: 'none',
            duration: 2000
          });
        }, 400);
      },
      complete: () => {
        if (typeof done === 'function') done();
      }
    });
  },

  // 保存到相册（结果区域底部按钮）
  saveAnswerCard() {
    // 检查是否有答案数据
    if (!this.data.randomAnswer) {
      wx.showToast({
        title: '请先翻开答案之书',
        icon: 'none'
      });
      return;
    }
    
    // 调用现有的生成和保存逻辑
    this.generateAndSaveImage();
  },

  // 生成并保存图片到相册
  generateAndSaveImage() {
    console.log('开始生成答案卡片...');
    wx.showLoading({ title: '生成中...' });

    const { question, randomAnswer, aiExplanation } = this.data;

    if (!randomAnswer) {
      wx.hideLoading();
      wx.showToast({ title: '暂无答案可保存', icon: 'none' });
      return;
    }

    // 使用 canvas 生成图片
    const query = wx.createSelectorQuery().in(this);
    query.select('#answerShareCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) {
          wx.hideLoading();
          wx.showToast({ title: '生成失败', icon: 'none' });
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

          // 绘制神秘风格背景
          const theme = this.getSelectedShareTheme();
          const gradient = ctx.createLinearGradient(0, 0, width, height);
          gradient.addColorStop(0, theme.colorStart);
          gradient.addColorStop(0.5, theme.colorMid);
          gradient.addColorStop(1, theme.colorEnd);
          ctx.fillStyle = gradient;
          ctx.fillRect(0, 0, width, height);

          // 绘制装饰星星
          this.drawStars(ctx, width, height);

          // 绘制内容
          this.drawAnswerCardContent(ctx, width, height, question, randomAnswer, aiExplanation);

          // 转换为图片
          wx.canvasToTempFilePath({
            canvas: canvas,
            success: (tempRes) => {
              console.log('答案卡片生成成功:', tempRes.tempFilePath);
              
              // ✅ 修复：检查权限并保存图片
              this.checkAndSavePhoto(tempRes.tempFilePath);
            },
            fail: (err) => {
              console.error('生成答案卡片失败:', err);
              wx.hideLoading();
              wx.showToast({ title: '生成失败', icon: 'none' });
            }
          });
        } catch (error) {
          console.error('绘制答案卡片出错:', error);
          wx.hideLoading();
          wx.showToast({ title: '生成异常', icon: 'none' });
        }
      });
  },

  // 检查权限并保存图片
  checkAndSavePhoto(filePath) {
    wx.getSetting({
      success: (settingRes) => {
        const hasWritePhotosAlbumAuth = settingRes.authSetting['scope.writePhotosAlbum'];
        
        if (hasWritePhotosAlbumAuth === true) {
          // 已授权，直接保存
          this.savePhotoToAlbum(filePath);
        } else if (hasWritePhotosAlbumAuth === false) {
          // 用户明确拒绝过，引导去设置
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
                      wx.showLoading({ title: '保存中...' });
                      this.savePhotoToAlbum(filePath);
                    } else {
                      // 用户未开启权限，再次引导
                      wx.showModal({
                        title: '权限未开启',
                        content: '您尚未开启相册权限，无法保存图片。请点击“去设置”开启权限',
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
          // 从未请求过权限，直接引导去设置页面
          console.log('[答案之书] 从未请求过权限，引导去设置');
          wx.hideLoading();
          wx.showModal({
            title: '需要相册权限',
            content: '保存图片需要访问您的相册权限，请点击“去设置”开启',
            confirmText: '去设置',
            cancelText: '取消',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting({
                  success: (settingRes) => {
                    if (settingRes.authSetting['scope.writePhotosAlbum']) {
                      console.log('[答案之书] 用户在设置中开启了权限，重新保存');
                      wx.showLoading({ title: '保存中...' });
                      this.savePhotoToAlbum(filePath);
                    } else {
                      // 用户未开启权限，再次引导
                      wx.showModal({
                        title: '权限未开启',
                        content: '您尚未开启相册权限，无法保存图片。请点击“去设置”开启权限',
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
                    console.error('[答案之书] 打开设置失败:', err);
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
        this.savePhotoToAlbum(filePath);
      }
    });
  },

  // 执行保存到相册
  savePhotoToAlbum(filePath) {
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
        
        if (err.errMsg.includes('auth deny')) {
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
                    } else {
                      // 用户未开启权限，再次引导
                      wx.showModal({
                        title: '权限未开启',
                        content: '您尚未开启相册权限，无法保存图片。请点击“去设置”开启权限',
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
          wx.showToast({ title: '保存失败: ' + (err.errMsg || '未知错误'), icon: 'none', duration: 3000 });
        }
      }
    });
  },

  // ✅ 检查权限并保存图片
  checkAndSavePhoto(filePath) {
    console.log('[答案之书] 开始检查相册权限...');
    
    wx.getSetting({
      success: (settingRes) => {
        console.log('[答案之书] 当前权限设置:', settingRes);
        
        const hasWritePhotosAlbumAuth = settingRes.authSetting['scope.writePhotosAlbum'];
        
        if (hasWritePhotosAlbumAuth === true) {
          // ✅ 已授权，直接保存
          console.log('[答案之书] 已有相册权限，直接保存');
          this.savePhotoToAlbum(filePath);
        } else if (hasWritePhotosAlbumAuth === false) {
          // ❌ 用户明确拒绝过，引导去设置
          console.log('[答案之书] 用户已拒绝相册权限，引导去设置');
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
                      console.log('[答案之书] 用户在设置中开启了权限，重新保存');
                      wx.showLoading({ title: '保存中...' });
                      this.savePhotoToAlbum(filePath);
                    } else {
                      // 用户未开启权限，再次引导
                      wx.showModal({
                        title: '权限未开启',
                        content: '您尚未开启相册权限，无法保存图片。请点击“去设置”开启权限',
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
          // ⚠️ 从未请求过权限，主动请求授权
          console.log('[答案之书] 首次请求，调用 wx.authorize');
          wx.authorize({
            scope: 'scope.writePhotosAlbum',
            success: () => {
              console.log('[答案之书] 授权成功，开始保存');
              this.savePhotoToAlbum(filePath);
            },
            fail: (err) => {
              console.error('[答案之书] 用户拒绝授权:', err);
              wx.hideLoading();
              wx.showModal({
                title: '需要相册权限',
                content: '保存图片需要访问您的相册，请点击"去设置"开启权限',
                confirmText: '去设置',
                cancelText: '取消',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    wx.openSetting({
                      success: (settingRes) => {
                        if (settingRes.authSetting['scope.writePhotosAlbum']) {
                          console.log('[答案之书] 用户在设置中开启了权限，重新保存');
                          wx.showLoading({ title: '保存中...' });
                          this.savePhotoToAlbum(filePath);
                        } else {
                          // 用户未开启权限，再次引导
                          wx.showModal({
                            title: '权限未开启',
                            content: '您尚未开启相册权限，无法保存图片。请点击“去设置”开启权限',
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
            }
          });
        }
      },
      fail: (err) => {
        console.error('[答案之书] 获取权限设置失败:', err);
        // 如果获取权限设置失败，直接尝试保存（兼容处理）
        this.savePhotoToAlbum(filePath);
      }
    });
  },

  // ✅ 执行保存到相册
  savePhotoToAlbum(filePath) {
    console.log('[答案之书] 执行保存到相册:', filePath);
    
    wx.saveImageToPhotosAlbum({
      filePath: filePath,
      success: () => {
        console.log('[答案之书] 保存到相册成功');
        wx.hideLoading();
        wx.showToast({ title: '已保存到相册', icon: 'success', duration: 2000 });
      },
      fail: (err) => {
        console.error('[答案之书] 保存到相册失败:', err);
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

  // 分享答案卡片
  shareAnswerCard(done) {
    console.log('开始生成答案卡片...');
    wx.showLoading({ title: '生成中...' });

    const { question, randomAnswer, aiExplanation } = this.data;

    if (!randomAnswer) {
      wx.hideLoading();
      wx.showToast({ title: '暂无答案可分享', icon: 'none' });
      if (typeof done === 'function') done();
      return;
    }

    // 使用 canvas 生成图片
    const query = wx.createSelectorQuery().in(this);
    query.select('#answerShareCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) {
          wx.hideLoading();
          wx.showToast({ title: '生成失败', icon: 'none' });
          if (typeof done === 'function') done();
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

          // 绘制神秘风格背景
          const theme = this.getSelectedShareTheme();
          const gradient = ctx.createLinearGradient(0, 0, width, height);
          gradient.addColorStop(0, theme.colorStart);
          gradient.addColorStop(0.5, theme.colorMid);
          gradient.addColorStop(1, theme.colorEnd);
          ctx.fillStyle = gradient;
          ctx.fillRect(0, 0, width, height);

          // 绘制装饰星星
          this.drawStars(ctx, width, height);

          // 绘制内容
          this.drawAnswerCardContent(ctx, width, height, question, randomAnswer, aiExplanation);

          // 转换为图片
          wx.canvasToTempFilePath({
            canvas: canvas,
            success: (tempRes) => {
              console.log('答案卡片生成成功:', tempRes.tempFilePath);
              wx.hideLoading();

              // ✅ 调用微信的图片分享菜单（带小程序入口）
              wx.showShareImageMenu({
                path: tempRes.tempFilePath,
                needShowEntrance: true,  // ✅ 显示小程序入口
                entrancePath: '/subpage1/pages/answer-book/index',  // ✅ 点击入口后跳转的路径
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
                  if (typeof done === 'function') done();
                }
              });
            },
            fail: (err) => {
              console.error('生成答案卡片失败:', err);
              wx.hideLoading();
              wx.showToast({ title: '生成失败', icon: 'none' });
              if (typeof done === 'function') done();
            }
          });
        } catch (error) {
          console.error('绘制答案卡片出错:', error);
          wx.hideLoading();
          wx.showToast({ title: '生成异常', icon: 'none' });
          if (typeof done === 'function') done();
        }
      });
  },

  // 绘制装饰星星
  drawStars(ctx, width, height) {
    ctx.globalAlpha = 0.3;
    ctx.fillStyle = '#FFD700';

    for (let i = 0; i < 50; i++) {
      const x = Math.random() * width;
      const y = Math.random() * height;
      const size = Math.random() * 3 + 1;

      ctx.beginPath();
      ctx.arc(x, y, size, 0, 2 * Math.PI);
      ctx.fill();
    }

    ctx.globalAlpha = 1.0;
  },

  // 绘制答案卡片内容
  drawAnswerCardContent(ctx, width, height, question, randomAnswer, aiExplanation) {
    // 绘制标题区域
    ctx.fillStyle = '#FFD700';
    ctx.font = 'bold 48px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('🔮 答案之书 🔮', width / 2, 80);

    // 绘制日期
    const date = new Date();
    const dateStr = `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
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

    // 绘制问题（如果有）
    let currentY = 220;
    if (question) {
      ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
      ctx.font = 'italic 32px sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText('Q：' + question, 80, currentY);
      currentY += 80;
    }

    // 绘制答案之书的答案（重点突出）
    ctx.fillStyle = '#FFD700';
    ctx.font = 'bold 40px sans-serif';
    ctx.textAlign = 'center';
    
    // 计算答案文本的换行
    const answerLines = this.wrapTextForCanvas(ctx, randomAnswer, width - 160, 40);
    const answerStartY = currentY + 40;
    
    answerLines.forEach((line, index) => {
      ctx.fillText(line, width / 2, answerStartY + index * 50);
    });

    currentY = answerStartY + answerLines.length * 50 + 60;

    // 绘制分隔线
    ctx.strokeStyle = 'rgba(255, 215, 0, 0.3)';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(80, currentY);
    ctx.lineTo(width - 80, currentY);
    ctx.stroke();

    currentY += 60;

    // 绘制AI解读（如果有）
    if (aiExplanation) {
      ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
      ctx.font = 'bold 36px sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText('💫 心灵解读', 80, currentY);
      currentY += 50;

      ctx.font = '28px sans-serif';
      ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
      
      const explanationLines = this.wrapTextForCanvas(ctx, aiExplanation, width - 160, 28);
      explanationLines.forEach((line, index) => {
        ctx.fillText(line, 80, currentY + index * 40);
      });

      currentY += explanationLines.length * 40 + 60;
    }

    // 底部品牌标识
    const bottomY = height - 80;
    ctx.fillStyle = 'rgba(255, 215, 0, 0.8)';
    ctx.font = 'bold 28px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('—— 情绪解压卡 ——', width / 2, bottomY);
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
  },
});
