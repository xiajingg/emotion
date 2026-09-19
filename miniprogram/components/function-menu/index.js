// components/function-menu/index.js
Component({
  properties: {
    show: {
      type: Boolean,
      value: false
    },
    userConstellation: {
      type: String,
      value: ''
    },
    dailyBonusUnread: {  // 每日补给未读数
      type: Number,
      value: 0
    },
    horoscopeUnread: {   // 星座运势未读数
      type: Number,
      value: 0
    }
  },

  data: {
    constellationSymbol: '✨',  // 默认图标
    constellationText: '星象解读',  // 默认文字
    selectedPage: ''
  },

  observers: {
    'userConstellation': function(constellation) {
      // 当星座变化时，更新图标和文字
      const symbol = this.getConstellationSymbol(constellation);
      const text = constellation ? `${constellation}解读` : '星象解读';
      this.setData({ 
        constellationSymbol: symbol,
        constellationText: text
      });
    }
  },

  methods: {
    // 获取星座符号
    getConstellationSymbol(constellation) {
      const symbolMap = {
        '白羊座': '♈',
        '金牛座': '♉',
        '双子座': '♊',
        '巨蟹座': '♋',
        '狮子座': '♌',
        '处女座': '♍',
        '天秤座': '♎',
        '天蝎座': '♏',
        '射手座': '♐',
        '摩羯座': '♑',
        '水瓶座': '♒',
        '双鱼座': '♓'
      };
      return symbolMap[constellation] || '✨';
    },
    // 点击遮罩层关闭
    onMaskTap() {
      this.triggerEvent('close');
    },

    // 点击关闭按钮
    onClose() {
      this.triggerEvent('close');
    },

    // 阻止事件冒泡
    stopPropagation() {
      // 空函数，用于阻止点击内容区域时关闭弹窗
    },

    // 点击功能项
    onMenuItemTap(e) {
      if (this.data.selectedPage) return;
      const page = e.currentTarget.dataset.page;
      this.setData({ selectedPage: page });
      this.triggerEvent('select', { page });
      setTimeout(() => {
        this.setData({ selectedPage: '' });
      }, 800);
    }
  }
});
