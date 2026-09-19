// components/step-list/index.js
Component({
  properties: {
    steps: {
      type: Array,
      value: []
    },
    activeIndex: {
      type: Number,
      value: 0
    }
  },

  methods: {
    onContentTap(e) {
      if (!this.triggerEvent) return;
      const stepIdx = e.currentTarget.dataset.stepIdx;
      this.triggerViewDetail(stepIdx);
    },

    // 点击图片进入详情页
    onImageTap(e) {
      if (!this.triggerEvent) return;

      const stepIdx = e.currentTarget.dataset.stepIdx;
      this.triggerViewDetail(stepIdx);
    },

    triggerViewDetail(stepIdx) {
      const step = this.data.steps && this.data.steps[stepIdx];
      if (!step) return;
      this.triggerEvent('viewdetail', { stepIdx, id: step.id, step });
    }
  }
});
