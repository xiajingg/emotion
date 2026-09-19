// utils/request.js
const baseURL = 'https://www.onekey-ai.top';

let unauthorizedCount = 0;
let unauthorizedTimeout = null;

// 401 自动登录相关
let isLoggingIn = false;  // 防止并发登录
let loginPromise = null;  // 登录 Promise，供多个请求等待

/**
 * 封装微信请求
 * @param {Object} options - { url, method, data, header, autoLogin: boolean }
 * @param {Boolean} options.autoLogin - 是否自动处理401并刷新页面（默认true）
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = {
      'content-type': 'application/json',
      ...options.header,
    };

    // 非登录接口自动注入 token
    if (options.url !== '/user/api/v2/getOpenId' && token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    wx.request({
      url: `${baseURL}${options.url}`,
      method: options.method || 'GET',
      header: header,
      data: (options.method === 'POST' || options.method === 'PUT') ? options.data : {},
      timeout: 120000,  // ✅ 增加到120秒，适应Ollama大模型推理时间
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data);
        } else if (res.statusCode === 401) {
          // 401 自动处理
          if (options.autoLogin !== false) {
            handle401AutoLogin().then(() => {
              // 登录成功后刷新当前页面
              refreshCurrentPage();
            }).catch(err => {
              console.error('自动登录失败:', err);
            });
          }
          reject(new Error('401'));
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '请求失败', icon: 'none' });
          reject(new Error(`HTTP Error ${res.statusCode}`));
        }
      },
      fail(err) {
        const errMsg = err.errMsg || '';
        if (errMsg.includes('timeout')) {
          console.error(`[request timeout] URL: ${options.url}`, err);
          wx.showToast({ title: '请求超时，请稍后重试', icon: 'none', duration: 2000 });
          reject(new Error('timeout'));
        } else if (errMsg.includes('401')) {
          // 401 自动处理
          if (options.autoLogin !== false) {
            handle401AutoLogin().then(() => {
              refreshCurrentPage();
            }).catch(err => {
              console.error('自动登录失败:', err);
            });
          }
          reject(new Error('401'));
        } else if (errMsg.includes('500')) {
          wx.showToast({ title: '系统休息中，请稍后再试', icon: 'none', duration: 2000 });
          reject(new Error('系统休息中'));
        } else {
          wx.showToast({ title: '网络异常，请检查网络连接', icon: 'none' });
          reject(new Error(`请求失败: ${errMsg}`));
        }
      }
    });
  });
};

/**
 * 处理 401 自动登录（防止并发）
 */
const handle401AutoLogin = async () => {
  // 如果正在登录，等待当前的登录完成
  if (isLoggingIn && loginPromise) {
    console.log('[401] 检测到正在登录，等待登录完成...');
    return loginPromise;
  }

  // 开始登录
  isLoggingIn = true;
  console.log('[401] 开始自动登录...');
  
  wx.showLoading({ title: '登录中...', mask: true });
  
  loginPromise = loginWithWechat()
    .then(token => {
      console.log('[401] 自动登录成功');
      wx.hideLoading();
      isLoggingIn = false;
      loginPromise = null;
      
      // ✅ 新增：401 自动登录成功后，立即获取用户资料
      loadUserProfileAfterLogin();
      
      return token;
    })
    .catch(err => {
      console.error('[401] 自动登录失败:', err);
      wx.hideLoading();
      wx.showToast({ title: '登录失败，请重新启动小程序', icon: 'none', duration: 3000 });
      isLoggingIn = false;
      loginPromise = null;
      throw err;
    });

  return loginPromise;
};

/**
 * 登录后获取用户资料（供 401 自动登录和 app.js 调用）
 */
const loadUserProfileAfterLogin = () => {
  console.log('[401] 开始获取用户资料...');
  request({
    url: '/user/api/v1/profile',
    method: 'GET'
  }).then(res => {
    if (res.data) {
      console.log('[401] 获取用户资料成功:', res.data);
      const nickname = res.data.nickname || '';
      const constellation = res.data.constellation || '';
      
      // 更新全局数据
      const app = getApp();
      if (app && app.globalData) {
        app.globalData.nickname = nickname;
        app.globalData.constellation = constellation;
        app.globalData.constellationUpdated = true;
        console.log('[401] 已更新全局用户资料 - 昵称:', nickname, '星座:', constellation);
      }
      
      // 同步到本地缓存
      const existingUserInfo = wx.getStorageSync('userInfo') || {};
      wx.setStorageSync('userInfo', {
        ...existingUserInfo,
        nickname: nickname,
        constellation: constellation
      });
    }
  }).catch(err => {
    console.error('[401] 获取用户资料失败:', err);
  });
};

/**
 * 刷新当前页面
 */
const refreshCurrentPage = () => {
  const pages = getCurrentPages();
  if (pages.length > 0) {
    const currentPage = pages[pages.length - 1];
    console.log('[401] 刷新页面:', currentPage.route);
    
    // 延迟一下再刷新，确保登录完成
    setTimeout(() => {
      // 重新执行 onLoad 方法，传入原有参数
      // 这样每个页面都会按照自己的逻辑重新加载数据
      if (typeof currentPage.onLoad === 'function') {
        console.log('[401] 重新执行 onLoad');
        currentPage.onLoad(currentPage.options || {});
      }
    }, 500);
  }
};

/**
 * 处理 401 未授权
 */
const handle401 = () => {
  unauthorizedCount++;
  wx.setStorageSync('unauthorizedCount', unauthorizedCount);
  console.log('401错误计数:', unauthorizedCount);
  wx.removeStorageSync('token');

  if (unauthorizedCount >= 5) {
    if (unauthorizedTimeout) clearTimeout(unauthorizedTimeout);
    unauthorizedTimeout = setTimeout(() => {
      unauthorizedCount = 0;
      wx.setStorageSync('unauthorizedCount', 0);
    }, 3600000); // 1小时清零
  }
  // 不再自动跳转，由调用方处理
  return Promise.reject(new Error('401'));
};

/**
 * 微信登录获取 code，再换取 token
 */
const loginWithWechat = () => {
  return new Promise((resolve, reject) => {
    wx.login({
      success(loginRes) {
        const code = loginRes.code;
        if (!code) {
          wx.showToast({ title: '未获取到code', icon: 'error' });
          reject(new Error('未获取到code'));
          return;
        }
        console.log('wx.login code:', code);
        // 用 code 换 token
        request({
          url: '/user/api/v2/getOpenId?code=' + code,
          method: 'GET',
          header: {}
        }).then(res => {
          const token = res.data.token;
          const id = res.data.id;
          wx.setStorageSync('token', token);
          wx.setStorageSync('id', id);
          wx.setStorageSync('unauthorizedCount', 0);
          console.log('登录成功, token:', token);
          wx.showToast({ title: '登录成功', icon: 'success', duration: 1500 });
          resolve(token);
        }).catch(err => {
          console.error('登录失败:', err);
          wx.showToast({ title: '登录失败，请稍后重试', icon: 'none' });
          reject(err);
        });
      },
      fail(err) {
        console.error('调用微信登录接口失败:', err);
        wx.showToast({ title: '调用微信登录失败', icon: 'error' });
        reject(err);
      }
    });
  });
};

/**
 * 带登录检查的请求（确保有 token 再发请求）
 */
const requestWithLogin = async (options) => {
  try {
    let token = wx.getStorageSync('token');
    if (!token) {
      await loginWithWechat();
      token = wx.getStorageSync('token');
    }
    if (!token) throw new Error('获取token失败');

    try {
      const res = await request({
        ...options,
        header: {
          ...options.header,
          'Authorization': `Bearer ${token}`
        }
      });
      return res;
    } catch (err) {
      if (err.toString().includes('401')) {
        await loginWithWechat();
        return await request({
          ...options,
          header: {
            ...options.header,
            'Authorization': `Bearer ${wx.getStorageSync('token')}`
          }
        });
      }
      throw err;
    }
  } catch (err) {
    console.error('请求失败:', err);
    throw err;
  }
};

const uploadFileWithLogin = async (filePath, options = {}) => {
  let token = wx.getStorageSync('token');
  if (!token) {
    await loginWithWechat();
    token = wx.getStorageSync('token');
  }
  if (!token) throw new Error('获取token失败');

  return new Promise((resolve, reject) => {
    let retried = false;
    const doUpload = () => {
      wx.uploadFile({
        url: `${baseURL}${options.url || '/files/upload'}`,
        filePath,
        name: options.name || 'file',
        formData: options.formData || {},
        header: {
          ...options.header,
          'Authorization': `Bearer ${token}`
        },
        timeout: options.timeout || 120000,
        success(res) {
          if (res.statusCode === 200) {
            try {
              const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
              resolve(data);
            } catch (err) {
              reject(new Error('上传响应解析失败'));
            }
          } else if (res.statusCode === 401 && !retried) {
            retried = true;
            loginWithWechat().then(() => {
              token = wx.getStorageSync('token');
              doUpload();
            }).catch(reject);
          } else {
            wx.showToast({ title: '图片上传失败', icon: 'none' });
            reject(new Error(`HTTP Error ${res.statusCode}`));
          }
        },
        fail(err) {
          wx.showToast({ title: '图片上传失败，请重试', icon: 'none' });
          reject(err);
        }
      });
    };
    doUpload();
  });
};

/**
 * SSE流式请求（用于情绪分析等需要实时输出的场景）
 * @param {Object} options - { url, method, data, header }
 * @param {Function} onChunk - 接收到数据块时的回调函数
 * @param {Function} onComplete - 完成时的回调函数
 * @param {Function} onError - 错误时的回调函数
 * @returns {Object} - 返回包含 abort 方法的对象，用于手动中断请求
 */
const requestStream = (options, onChunk, onComplete, onError) => {
  let requestTask = null;
  let isAborted = false;

  const promise = new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = {
      'content-type': 'application/json',
      ...options.header,
    };

    // 非登录接口自动注入 token
    if (options.url !== '/user/api/v2/getOpenId' && token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    console.log('[SSE] 开始流式请求:', options.url);
    
    requestTask = wx.request({
      url: `${baseURL}${options.url}`,
      method: options.method || 'GET',
      header: header,
      data: (options.method === 'POST' || options.method === 'PUT') ? options.data : {},
      timeout: 120000, // 流式请求超时时间延长到2分钟
      enableChunked: true, // 启用分块传输（关键配置）
      success(res) {
        if (isAborted) return;

        console.log('[SSE] 收到响应, statusCode:', res.statusCode);
        
        if (res.statusCode === 200) {
          const responseText = res.data;
          
          if (typeof responseText === 'string') {
            const events = parseSSEEvents(responseText);
            
            events.forEach((event) => {
              if (isAborted) return;
              
              if (event.name === 'chunk' && onChunk) {
                onChunk(event.data);
              } else if (event.name === 'complete' && onComplete) {
                onComplete(event.data);
                resolve(event.data);
              } else if (event.name === 'error' && onError) {
                onError(event.data);
                reject(new Error(event.data));
              }
            });
          } else {
            resolve(res.data);
          }
        } else if (res.statusCode === 401) {
          if (!isAborted) reject(new Error('401'));
        } else {
          if (!isAborted) {
            wx.showToast({ title: (res.data && res.data.message) || '请求失败', icon: 'none' });
            reject(new Error(`HTTP Error ${res.statusCode}`));
          }
        }
      },
      fail(err) {
        if (isAborted) return;
        
        console.error('[SSE] 请求失败:', err);
        const errMsg = err.errMsg || '';
        if (errMsg.includes('timeout')) {
          wx.showToast({ title: '请求超时，请稍后重试', icon: 'none', duration: 2000 });
          reject(new Error('timeout'));
        } else if (errMsg.includes('abort')) {
          // 主动中断不报错
          reject(new Error('aborted'));
        } else {
          wx.showToast({ title: '网络异常，请检查网络连接', icon: 'none' });
          reject(new Error(`请求失败: ${errMsg}`));
        }
      }
    });
  });

  // 返回一个带有 abort 方法的对象
  return {
    then: promise.then.bind(promise),
    catch: promise.catch.bind(promise),
    finally: promise.finally.bind(promise),
    abort: () => {
      isAborted = true;
      if (requestTask) {
        requestTask.abort();
      }
    }
  };
};

/**
 * 解析SSE事件流
 * @param {String} text - SSE格式的文本
 * @returns {Array} - 解析后的事件数组
 */
const parseSSEEvents = (text) => {
  const events = [];
  const lines = text.split('\n');
  let currentEvent = { name: 'message', data: '' };
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];  // ✅ 不要trim，保留原始内容
    
    if (line.startsWith('event:')) {
      currentEvent.name = line.substring(6).trim();
    } else if (line.startsWith('data:')) {
      // ✅ 只去掉'data:'前缀，保留后面的空格
      currentEvent.data = line.substring(5);
    } else if (line.trim() === '' && currentEvent.data) {
      // 空行表示一个事件结束
      // ✅ trim data，去除首尾空白
      currentEvent.data = currentEvent.data.trim();
      if (currentEvent.data) {  // ✅ 只添加非空数据的事件
        events.push({ ...currentEvent });
      }
      currentEvent = { name: 'message', data: '' };
    }
  }
  
  // 添加最后一个事件
  if (currentEvent.data) {
    currentEvent.data = currentEvent.data.trim();
    if (currentEvent.data) {
      events.push(currentEvent);
    }
  }
  
  return events;
};

module.exports = {
  request,
  loginWithWechat,
  requestWithLogin,
  uploadFileWithLogin,
  requestStream
};
