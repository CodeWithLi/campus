const toast = ({ title = '数据加载中', icon = 'none', mask = true, duration = 3000 } = {}) => {
  wx.showToast({
    title,
    icon,
    mask,
    duration
  })
}


const modal = (options = {}) => {
  // 使用 Promise 处理 wx.showModal 的返回结果
  return new Promise((resolve) => {

    // 默认的参数
    const defaultOpt = {
      title: '提示',
      content: '您确定执行该操作吗?',
      confirmColor: '#f3514f',
    }

    // 将传入的参数和默认的参数进行合并
    const opts = Object.assign({}, defaultOpt, options)

    wx.showModal({
      // 将合并的参数赋值传递给 showModal 方法
      ...opts,
      complete({ confirm, cancel }) {
        // 如果用户点击了确定，通过 resolve 抛出 true
        // 如果用户点击了取消，通过 resolve 抛出 false
        confirm && resolve(true)
        cancel && resolve(false)
      }
    })
  })
}

wx.toast = toast
wx.modal = modal

export { toast,modal }