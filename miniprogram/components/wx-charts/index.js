class WxCharts {
  constructor(options) {
    this.options = options;
    this.canvasId = options.canvasId;
    // 支持新 Canvas 2D API
    if (options.context) {
      this.ctx = options.context;
      this.isNewCanvas = true;
    } else {
      this.ctx = wx.createCanvasContext(options.canvasId);
      this.isNewCanvas = false;
    }
    this.width = options.width;
    this.height = options.height;
    this.data = options.data || [];
    this.categories = options.categories || [];
    this.yAxis = options.yAxis || { min: 0, max: 100 };
    this.title = options.title || '';
    this.colors = options.colors || ['#F48FB1', '#00B894', '#FDCB6E'];
    this.series = options.series || [];
    
    this.padding = 35;  // 减小padding，增加图表显示范围
    this.chartHeight = this.height - this.padding * 2;
    this.chartWidth = this.width - this.padding * 2;
    
    // 动画配置
    this.animationDuration = options.animationDuration || 800; // 默认800ms动画
    this.animationEnabled = options.animationEnabled !== false; // 默认启用动画
  }

  // 绘制虚线
  drawDashedLine(ctx, x1, y1, x2, y2, dashLength = 5, gapLength = 3) {
    const dx = x2 - x1;
    const dy = y2 - y1;
    const distance = Math.sqrt(dx * dx + dy * dy);
    const dashCount = Math.floor(distance / (dashLength + gapLength));
    
    const stepX = dx / dashCount;
    const stepY = dy / dashCount;
    
    for (let i = 0; i < dashCount; i++) {
      const startX = x1 + stepX * i;
      const startY = y1 + stepY * i;
      const endX = x1 + stepX * (i + 0.6);
      const endY = y1 + stepY * (i + 0.6);
      
      ctx.beginPath();
      ctx.moveTo(startX, startY);
      ctx.lineTo(endX, endY);
      ctx.stroke();
    }
  }

  // 兼容新 Canvas API 的 setFontStyle
  setFontStyle(size, color) {
    if (this.isNewCanvas) {
      this.ctx.font = `${size}px sans-serif`;
      this.ctx.fillStyle = color;
    } else {
      this.ctx.setFontSize(size);
      this.ctx.setFillStyle(color);
    }
  }

  // ✅ 将8位十六进制颜色转换为 rgba 格式
  convertColor(color) {
    // 如果是8位颜色（如 #F48FB180），转换为 rgba
    if (color.length === 9 && color.startsWith('#')) {
      const hex = color.slice(1);
      const r = parseInt(hex.substr(0, 2), 16);
      const g = parseInt(hex.substr(2, 2), 16);
      const b = parseInt(hex.substr(4, 2), 16);
      const a = parseInt(hex.substr(6, 2), 16) / 255;
      return `rgba(${r},${g},${b},${a.toFixed(2)})`;
    }
    return color;
  }
  
  // 兼容新 Canvas API 的 setLineStyle
  setLineStyle(width, color) {
    // ✅ 转换颜色格式
    const convertedColor = this.convertColor(color);
    
    if (this.isNewCanvas) {
      this.ctx.lineWidth = width;
      this.ctx.strokeStyle = convertedColor;
    } else {
      this.ctx.setLineWidth(width);
      this.ctx.setStrokeStyle(convertedColor);
    }
  }

  draw() {
    const ctx = this.ctx;
    const padding = this.padding;
    const chartWidth = this.chartWidth;
    const chartHeight = this.chartHeight;
    
    console.log('[wx-charts] draw() 调用', {
      yAxis: this.yAxis,
      yAxisMin: this.yAxis ? this.yAxis.min : 'undefined',
      yAxisMax: this.yAxis ? this.yAxis.max : 'undefined'
    });
    
    // 画标题
    if (this.title) {
      this.setFontStyle(14, '#333');
      ctx.fillText(this.title, padding, 30);
    }
    
    // 计算Y轴刻度
    const yMin = this.yAxis && this.yAxis.min !== undefined ? this.yAxis.min : 0;
    const yMax = this.yAxis && this.yAxis.max !== undefined ? this.yAxis.max : 100;
    const yRange = yMax - yMin;
    const ySteps = 5;
    
    console.log('[wx-charts] draw() Y轴计算:', { yMin, yMax, yRange, ySteps });
    console.log('[wx-charts] 画布尺寸:', { padding, chartHeight, chartWidth });
    
    // 画网格线和Y轴刻度
    this.setLineStyle(1, '#E8E8ED');
    this.setFontStyle(10, '#666'); // ✅ 加深颜色：从 #B2BEC3 改为 #666
    
    for (let i = 0; i <= ySteps; i++) {
      const y = padding + chartHeight - (chartHeight / ySteps) * i;
      const value = Math.round(yMin + (yRange / ySteps) * i);
      
      console.log(`[wx-charts] Y轴刻度 ${i}: position=${y}, value=${value}`);
      
      ctx.beginPath();
      ctx.moveTo(padding, y);
      ctx.lineTo(padding + chartWidth, y);
      ctx.stroke();
      
      ctx.textAlign = 'right';
      ctx.fillText(value.toString(), padding - 10, y + 4);
    }
    
    // 画X轴标签
    ctx.textAlign = 'center';
    this.setFontStyle(10, '#666'); // ✅ 修复：设置X轴标签颜色，从 #999 改为 #666
    const xStep = chartWidth / (this.categories.length - 1 || 1);
    this.categories.forEach((cat, i) => {
      const x = padding + xStep * i;
      ctx.fillText(cat, x, padding + chartHeight + 20); // ✅ 调整位置避免超出画布
    });
    
    // 画折线
    this.series.forEach((series, seriesIndex) => {
      const data = series.data || [];
      const color = this.colors[seriesIndex] || this.colors[0];
      
      // 过滤出有效数据点（score >= 0，且不为 null/undefined）
      const validPoints = [];
      data.forEach((value, i) => {
        // ✅ 修复：明确检查 null/undefined，避免 null >= 0 返回 true
        if (value !== null && value !== undefined && value >= 0) {
          const x = padding + xStep * i;
          const y = padding + chartHeight - (chartHeight * (value - yMin) / yRange);
          validPoints.push({ x, y, index: i, value });
        }
      });
      
      // 绘制实线连接有效数据点
      if (validPoints.length > 0) {
        ctx.beginPath();
        this.setLineStyle(2, color);
        
        ctx.moveTo(validPoints[0].x, validPoints[0].y);
        for (let i = 1; i < validPoints.length; i++) {
          ctx.lineTo(validPoints[i].x, validPoints[i].y);
        }
        
        ctx.stroke();
        
        // 绘制虚线连接被跳过的数据点
        this.setLineStyle(1.5, color + '80'); // 半透明颜色
        for (let i = 0; i < validPoints.length - 1; i++) {
          const current = validPoints[i];
          const next = validPoints[i + 1];
          
          // 如果两个有效点之间有被跳过的点，绘制虚线
          if (next.index - current.index > 1) {
            this.drawDashedLine(ctx, current.x, current.y, next.x, next.y);
          }
        }
      }
      
      // 画数据点
      this.setLineStyle(2, color);
      ctx.fillStyle = color;
      data.forEach((value, i) => {
        // ✅ 修复：过滤 null、undefined 和负数
        if (value === null || value === undefined || value < 0) return;
        
        const x = padding + xStep * i;
        const y = padding + chartHeight - (chartHeight * (value - yMin) / yRange);
        
        ctx.beginPath();
        ctx.arc(x, y, 4, 0, 2 * Math.PI);
        ctx.fill();
        
        // 数据点白色填充
        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath();
        ctx.arc(x, y, 2, 0, 2 * Math.PI);
        ctx.fill();
        ctx.fillStyle = color;
      });
      
      // 面积填充已关闭，只画线
    });
    
    if (!this.isNewCanvas) {
      ctx.draw();
    }
  }

  // 带动画的绘制方法
  drawWithAnimation() {
    if (!this.animationEnabled) {
      this.draw();
      return;
    }

    const ctx = this.ctx;
    const padding = this.padding;
    const chartWidth = this.chartWidth;
    const chartHeight = this.chartHeight;
    const duration = this.animationDuration;
    
    // 先绘制背景（网格、坐标轴等）
    this.drawBackground();
    
    // 准备数据点
    const allSeriesPoints = [];
    const yMin = this.yAxis && this.yAxis.min !== undefined ? this.yAxis.min : 0;
    const yMax = this.yAxis && this.yAxis.max !== undefined ? this.yAxis.max : 100;
    const yRange = yMax - yMin;
    const xStep = chartWidth / (this.categories.length - 1 || 1);
    
    this.series.forEach((series, seriesIndex) => {
      const data = series.data || [];
      const color = this.colors[seriesIndex] || this.colors[0];
      
      const validPoints = [];
      data.forEach((value, i) => {
        // ✅ 修复：明确检查 null/undefined，避免 null >= 0 返回 true
        if (value !== null && value !== undefined && value >= 0) {
          const x = padding + xStep * i;
          const y = padding + chartHeight - (chartHeight * (value - yMin) / yRange);
          validPoints.push({ x, y, index: i, value });
        }
      });
      
      allSeriesPoints.push({ points: validPoints, color, seriesIndex });
    });
    
    // 动画绘制折线（使用 setTimeout 模拟动画帧）
    let startTime = null;
    const frameDuration = 16; // 约60fps
    
    const animate = () => {
      const currentTime = Date.now();
      if (!startTime) startTime = currentTime;
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // 缓动函数（ease-out）
      const easeProgress = 1 - Math.pow(1 - progress, 3);
      
      // 重新绘制背景
      this.drawBackground();
      
      // 根据进度绘制折线
      allSeriesPoints.forEach(({ points, color }) => {
        if (points.length === 0) return;
        
        // 计算当前应该绘制到哪个点
        const maxIndex = Math.floor(easeProgress * (points.length - 1));
        const partialProgress = (easeProgress * (points.length - 1)) % 1;
        
        ctx.beginPath();
        this.setLineStyle(2, color);
        ctx.moveTo(points[0].x, points[0].y);
        
        // 绘制完整的线段
        for (let i = 1; i <= maxIndex && i < points.length; i++) {
          ctx.lineTo(points[i].x, points[i].y);
        }
        
        // 如果有部分进度，绘制最后一小段
        if (maxIndex < points.length - 1 && partialProgress > 0) {
          const currentPoint = points[maxIndex];
          const nextPoint = points[maxIndex + 1];
          const partialX = currentPoint.x + (nextPoint.x - currentPoint.x) * partialProgress;
          const partialY = currentPoint.y + (nextPoint.y - currentPoint.y) * partialProgress;
          ctx.lineTo(partialX, partialY);
        }
        
        ctx.stroke();
        
        // 绘制已显示的数据点
        ctx.fillStyle = color;
        for (let i = 0; i <= maxIndex && i < points.length; i++) {
          const point = points[i];
          ctx.beginPath();
          ctx.arc(point.x, point.y, 4, 0, 2 * Math.PI);
          ctx.fill();
          
          // 数据点白色填充
          ctx.fillStyle = '#FFFFFF';
          ctx.beginPath();
          ctx.arc(point.x, point.y, 2, 0, 2 * Math.PI);
          ctx.fill();
          ctx.fillStyle = color;
        }
      });
      
      if (!this.isNewCanvas) {
        ctx.draw();
      }
      
      if (progress < 1) {
        setTimeout(animate, frameDuration);
      }
    };
    
    setTimeout(animate, frameDuration);
  }
  
  // 绘制背景（网格、坐标轴等）
  drawBackground() {
    const ctx = this.ctx;
    const padding = this.padding;
    const chartWidth = this.chartWidth;
    const chartHeight = this.chartHeight;
    
    // 画标题
    if (this.title) {
      this.setFontStyle(14, '#333');
      ctx.fillText(this.title, padding, 30);
    }
    
    // 计算Y轴刻度
    const yMin = this.yAxis && this.yAxis.min !== undefined ? this.yAxis.min : 0;
    const yMax = this.yAxis && this.yAxis.max !== undefined ? this.yAxis.max : 100;
    const yRange = yMax - yMin;
    const ySteps = 5;
    
    // 画网格线和Y轴刻度
    this.setLineStyle(1, '#E8E8ED');
    this.setFontStyle(10, '#666'); // ✅ 加深颜色：从 #B2BEC3 改为 #666
    
    for (let i = 0; i <= ySteps; i++) {
      const y = padding + chartHeight - (chartHeight / ySteps) * i;
      const value = Math.round(yMin + (yRange / ySteps) * i);
      
      ctx.beginPath();
      ctx.moveTo(padding, y);
      ctx.lineTo(padding + chartWidth, y);
      ctx.stroke();
      
      ctx.textAlign = 'right';
      ctx.fillText(value.toString(), padding - 10, y + 4);
    }
    
    // 画X轴标签
    ctx.textAlign = 'center';
    this.setFontStyle(10, '#666'); // ✅ 加深颜色：从 #B2BEC3 改为 #666
    const xStep = chartWidth / (this.categories.length - 1 || 1);
    this.categories.forEach((cat, i) => {
      const x = padding + xStep * i;
      ctx.fillText(cat, x, padding + chartHeight + 20); // ✅ 减小偏移量，避免超出画布
    });
  }
}

module.exports = WxCharts;
