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
    }
  }
})
