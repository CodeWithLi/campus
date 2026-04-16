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
    }
  },
  data: {
    towxmlNodes: {}
  },
  observers: {
    'content,role,thinking': function (content, role, thinking) {
      if (thinking || role !== 'assistant') {
        this.setData({ towxmlNodes: {} })
        return
      }
      this.setData({
        towxmlNodes: towxml(content || '', 'markdown')
      })
    }
  }
})
