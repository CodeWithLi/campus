<template>
  <div class="rag-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">RAG 调试</h2>
        <p class="toolbar-desc">对齐后端 `POST /admin/kb/rag/debug`，返回召回片段 + 上下文（可选）+ 最终回答</p>
      </div>
      <el-button :loading="submitLoading" type="primary" @click="handleSubmit">
        {{ submitLoading ? '调试中...' : '开始调试' }}
      </el-button>
    </div>

    <div class="content-grid">
      <div class="panel-card">
        <div class="panel-title">参数</div>
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" class="form-body">
          <el-form-item label="问题" prop="question">
            <el-input v-model="formData.question" type="textarea" :rows="3" placeholder="请输入用户问题" />
          </el-form-item>
          <el-form-item label="模型编码">
            <el-select v-model="formData.modelCode" filterable clearable class="full-width" placeholder="可选，不选则使用默认模型">
              <el-option label="默认模型（不传）" value="" />
              <el-option
                v-for="item in modelOptions"
                :key="item.modelCode"
                :label="`${item.modelName || item.modelCode} (${item.modelCode})`"
                :value="item.modelCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分类ID列表">
            <el-select v-model="formData.categoryIds" multiple filterable class="full-width" placeholder="可选，不选则全库检索">
              <el-option
                v-for="item in categoryOptions"
                :key="item.id"
                :label="`${item.categoryName} (${item.id})`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="TopK">
            <el-input-number v-model="formData.topK" :min="1" :max="20" :step="1" class="full-width" />
          </el-form-item>
          <el-form-item label="返回上下文">
            <el-switch v-model="formData.returnRagContext" />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel-card">
        <div class="panel-title">最终回答</div>
        <div class="answer-box" v-loading="submitLoading">
          <div v-if="ragResult.answer" class="answer-text">{{ ragResult.answer }}</div>
          <div v-else class="answer-empty">暂无结果</div>
        </div>
      </div>

      <div class="panel-card span-2">
        <div class="panel-title">召回片段（hits）</div>
        <div class="hits-table">
          <el-table :data="ragResult.hits" height="260">
            <el-table-column prop="documentId" label="文档ID" min-width="90" />
            <el-table-column prop="documentTitle" label="文档标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="categoryId" label="分类ID" min-width="90" />
            <el-table-column prop="chunkIndex" label="切片序号" min-width="90" />
            <el-table-column prop="distance" label="距离" min-width="90" />
            <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <div v-if="formData.returnRagContext" class="panel-card span-2">
        <div class="panel-title">RAG 上下文（ragContext）</div>
        <el-input v-model="ragResult.ragContext" type="textarea" :rows="8" readonly placeholder="未返回或暂无上下文" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ragDebug } from '@/api/rag'
import { getKbCategoryList } from '@/api/kb'
import { getModelConfigList } from '@/api/model'

const formRef = ref()
const submitLoading = ref(false)
const categoryOptions = ref([])
const modelOptions = ref([])

const formData = reactive({
  question: '',
  modelCode: '',
  categoryIds: [],
  topK: 5,
  returnRagContext: true
})

const formRules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }]
}

const ragResult = reactive({
  modelCode: '',
  question: '',
  answer: '',
  hits: [],
  ragContext: ''
})

const loadCategoryOptions = async () => {
  const res = await getKbCategoryList()
  categoryOptions.value = res.data || []
}

const loadModelOptions = async () => {
  const res = await getModelConfigList()
  const list = res.data || []
  modelOptions.value = list
    .filter((item) => item?.enabled === 1)
    .sort((a, b) => (a?.sortNo ?? 0) - (b?.sortNo ?? 0))
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const res = await ragDebug({
      question: formData.question,
      modelCode: formData.modelCode || null,
      categoryIds: formData.categoryIds?.length ? formData.categoryIds : null,
      topK: formData.topK,
      returnRagContext: formData.returnRagContext
    })

    ragResult.modelCode = res.data?.modelCode || ''
    ragResult.question = res.data?.question || ''
    ragResult.answer = res.data?.answer || ''
    ragResult.hits = res.data?.hits || []
    ragResult.ragContext = res.data?.ragContext || ''
    ElMessage.success('调试完成')
  } finally {
    submitLoading.value = false
  }
}

loadCategoryOptions()
loadModelOptions()
</script>

<style scoped>
.rag-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}

.toolbar-card {
  flex-shrink: 0;
  min-height: 76px;
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.25);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 24px rgba(123, 150, 219, 0.16);
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-left { min-width: 0; }
.toolbar-title {
  margin: 0;
  color: #243456;
  font-size: 18px;
  font-weight: 700;
}
.toolbar-desc {
  margin: 8px 0 0;
  color: rgba(84, 105, 152, 0.76);
  font-size: 13px;
}

.content-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-auto-rows: minmax(0, auto);
  gap: 14px;
  overflow: hidden;
}

.panel-card {
  min-height: 0;
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.24);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16);
  padding: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.span-2 { grid-column: span 2; }

.panel-title {
  color: #243456;
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 10px;
}

.form-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.answer-box {
  flex: 1;
  min-height: 0;
  border-radius: 12px;
  border: 1px solid rgba(133, 160, 230, 0.18);
  background: rgba(246, 250, 255, 0.9);
  padding: 12px;
  overflow: auto;
}

.answer-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #2c3f6c;
}

.answer-empty {
  color: rgba(84, 105, 152, 0.6);
  font-size: 13px;
}

.hits-table {
  flex: 1;
  min-height: 0;
}

.hits-table :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(126, 159, 241, 0.1);
  --el-table-border-color: rgba(133, 160, 230, 0.2);
  --el-table-row-hover-bg-color: rgba(126, 159, 241, 0.12);
  --el-table-text-color: #31446d;
  --el-table-header-text-color: #3d5384;
}

.full-width { width: 100%; }
</style>

