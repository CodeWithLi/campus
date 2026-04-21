Component({
  properties: {
    loading: {
      type: Boolean,
      value: false
    }
  },
  data: {
    value: ''
  },
  methods: {
    onInput(event) {
      this.setData({ value: event.detail.value })
    },
    onSend() {
      const message = (this.data.value || '').trim()
      if (!message || this.properties.loading) {
        return
      }
      this.triggerEvent('send', { message })
      this.setData({ value: '' })
    },
    onVoiceTap() {
      this.triggerEvent('voice')
    },
    onImageTap() {
      if (this.properties.loading) {
        return
      }
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = res?.tempFiles?.[0]?.tempFilePath || ''
          if (!filePath) return
          this.triggerEvent('image', { filePath })
        }
      })
    }
  }
})
