{
  "pages": [
    "pages/home/home",
    "pages/my/my",
    "pages/systemMessage/systemMessage",
    "components/carouse/carouse",
    "pages/group/group",
    "pages/activityPage/activityPage",
    "pages/settingsSwitch/settingsSwitch",
    "pages/activityList/activityList",
    "components/joinin/joinin",
    "components/login/login",
    "components/selfSettings/selfSettings",
    "components/searchComponent/searchComponent"
  ],
  "window": {
    "backgroundTextStyle": "light",
    "backgroundColor": "#eee",
    "navigationBarBackgroundColor": "#f3514f",
    "navigationBarTitleText": "志愿者小程序",
    "navigationBarTextStyle": "white",
    "enablePullDownRefresh": true
  },
  "tabBar": {
    "color": "#252933",
    "selectedColor": "#FF734C",
    "backgroundColor": "#ffffff",
    "borderStyle": "black",
    "list": [
      {
        "text": "首页",
        "iconPath": "/assets/tabbar/home.png",
        "selectedIconPath": "/assets/tabbar/home-active.png",
        "pagePath": "pages/home/home"
      },
      {
        "text": "分类",
        "iconPath": "/assets/tabbar/group.png",
        "selectedIconPath": "/assets/tabbar/group-active.png",
        "pagePath": "pages/group/group"
      },
      {
        "text": "我的",
        "iconPath": "/assets/tabbar/my.png",
        "selectedIconPath": "/assets/tabbar/my-active.png",
        "pagePath": "pages/my/my"
      }
    ]
  },
  "componentFramework": "glass-easel",
  "sitemapLocation": "sitemap.json",
  "lazyCodeLoading": "requiredComponents",
  "usingComponents": {
    "van-button": "@vant/weapp/button/index",
    "van-search": "@vant/weapp/search/index",
    "van-icon": "@vant/weapp/icon/index",
    "van-card": "@vant/weapp/card/index",
    "van-image": "@vant/weapp/image/index",
    "van-popup": "@vant/weapp/popup/index",
    "van-field": "@vant/weapp/field/index",
    "van-sidebar": "@vant/weapp/sidebar/index",
    "van-sidebar-item": "@vant/weapp/sidebar-item/index",
    "van-index-bar": "@vant/weapp/index-bar/index",
    "van-index-anchor": "@vant/weapp/index-anchor/index",
    "van-cell": "@vant/weapp/cell/index",
    "van-cell-group": "@vant/weapp/cell-group/index",
    "van-empty": "@vant/weapp/empty/index",
    "van-submit-bar": "@vant/weapp/submit-bar/index",
    "van-tag": "@vant/weapp/tag/index",
    "van-notice-bar": "@vant/weapp/notice-bar/index",
    "van-swipe-cell": "@vant/weapp/swipe-cell/index",
    "van-dialog": "@vant/weapp/dialog/index",
    "van-notify": "@vant/weapp/notify/index",
    "van-radio": "@vant/weapp/radio/index",
    "van-radio-group": "@vant/weapp/radio-group/index",
    "van-toast": "@vant/weapp/toast/index",
    "van-switch": "@vant/weapp/switch/index",
    "van-checkbox": "@vant/weapp/checkbox/index",
    "van-checkbox-group": "@vant/weapp/checkbox-group/index"
  },
  "networkTimeout": {
    "enableHttp": true
  },
  "requiredPrivateInfos": [ 
    "getLocation"
  ],
  "permission": {
    "scope.userLocation": {
      "desc": "用于活动签到，获取用户当前位置与活动位置比对"
    }
  }
}