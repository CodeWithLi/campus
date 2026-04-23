const towxml = require('../../towxml/index')

Component({
  properties: {
    role: {
      type: String,
      value: 'assistant'
    },
    content: {
      type: String,
      value: ''
    },
    attachments: {
      type: Array,
      value: []
    },
    thinking: {
      type: Boolean,
      value: false
    },
    streaming: {
      type: Boolean,
      value: false
    }
  },
  data: {
    towxmlNodes: {}
  },
  observers: {
    'content,role,thinking,streaming': function (content, role, thinking, streaming) {
      if (thinking || streaming || role !== 'assistant') {
        this.setData({ towxmlNodes: {} })
        return
      }
      this.setData({
        towxmlNodes: towxml(content || '', 'markdown')
      })
    }
  }
})
