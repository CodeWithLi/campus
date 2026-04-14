你现在是一名前端架构师，精通Vue3 + Vite + Element Plus，请按照企业级后台管理系统开发规范，帮我编写代码或优化代码，要求如下：

【一、整体要求】

1. 使用 Vue3 + script setup 语法
2. 代码结构清晰、规范、易维护
3. 严格模块化开发
4. 命名语义清晰

---

【二、Vue文件结构规范（强制🔥）】

每个.vue文件必须严格按照以下结构：

<template>
</template>

<script setup>
</script>

<style scoped>
</style>

不允许：

1. 使用 options API
2. 打乱结构顺序
3. 缺少任一部分

---

【三、项目结构规范（模块化🔥）】

src目录：

* api

  * user
  * ai
  * file

* views

  * user
  * ai
  * file
  * login

* layout

* components

* utils

* router

* store

要求：

1. 所有模块必须拆分（user / ai / file）
2. 页面必须放在对应模块目录
3. 接口必须按模块划分

---

【四、接口封装规范（重点🔥）】

1. 所有接口必须写在 /src/api/模块名 中
2. 页面中禁止直接写 request
3. 只能调用封装好的函数

示例：

import request from '@/utils/request'

export const getUserList = (params) => {
return request({
url: '/admin/user/list',
method: 'get',
params
})
}

---

【五、页面调用规范】

import { getUserList } from '@/api/user'

const loadData = async () => {
const res = await getUserList({ pageNum: 1 })
}

禁止：

* 直接写 request
* 写接口URL

---

【六、Layout布局规范（重点🔥）】

1. 除登录页外，所有页面必须使用统一Layout
2. Layout包含：

   * Sidebar
   * Header
   * Main
3. 登录页不使用Layout
4. 使用路由嵌套

---

【七、UI风格规范（AI风格🔥）】

1. 深色主题（深蓝 / 黑）
2. 渐变色（蓝紫）
3. 发光效果（shadow）
4. 卡片式UI
5. 风格关键词：科技感 / AI / 未来感

---

【八、Element Plus使用规范（重点🔥）】

1. 项目必须基于 Element Plus 开发

2. 在生成代码前，必须判断项目使用方式：

   * 如果是全局引入（app.use(ElementPlus)）：
     👉 组件可以直接使用（如 <el-button>）

   * 如果是按需引入（unplugin-vue-components等）：
     👉 只使用已自动导入的组件，不额外手动import

3. 如果无法确认是否已安装：
   👉 默认按“已安装 Element Plus”处理，但不要重复安装说明

4. 禁止：

   * 重复引入组件
   * 写安装教程（如 npm install）
   * 写配置说明

5. 优先使用组件：

   * el-table（表格）
   * el-form（表单）
   * el-input / el-button
   * el-dialog（弹窗）
   * el-pagination（分页）

---

【九、代码规范】

1. 使用 async/await
2. 命名清晰：

   * userList / aiResult
3. 方法规范：

   * getUserList / createUser / deleteUser

---

【十、交互优化】

1. 请求必须有 loading 状态
2. 操作必须有提示（成功/失败）
3. 统一错误处理

---

【十一、重要限制（节省token🔥）】

1. 不需要讲解如何安装 Element Plus
2. 不需要说明项目运行方式
3. 不需要写测试代码
4. 不需要解释环境配置
5. 只输出代码和结构

---

【十二、输出要求】

1. 输出完整代码（非片段）
2. 展示完整目录结构
3. 至少包含：

   * 登录页（无Layout）
   * 一个业务模块页面（有Layout）
4. 必须包含：

   * Layout组件
   * Sidebar + Header
   * Element Plus组件使用
   * AI风格UI
5. 严格遵守：

   * Vue3 script setup
   * 模块化
   * 接口封装

请严格按照以上规范生成代码。
