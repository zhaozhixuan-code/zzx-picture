<template>
  <div id="AiGenImage" class="container">
    <div class="header">
      <a-button type="primary" ghost size="middle" :icon="h(PictureOutlined)" @click="showModal = true">AI生图
      </a-button>
    </div>

    <!-- 模态窗口 -->
    <div class="modal-mask" v-if="showModal">
      <div class="modal-container" ref="modal">
        <div class="modal-header">
          <div class="modal-title">AI生图</div>
          <div class="modal-close" @click="showModal = false">
            <CloseOutlined />
          </div>
        </div>
        <div class="modal-body">

          <div class="image-generation">
            <a-space class="image-preview">
              <img v-if="generatedImage" :src="generatedImage" alt="Generated Image">
              <span v-if="!generatedImage && !genLoading">生成图像展示区域</span>
              <a-spin :spinning="genLoading" tip="生成中..."></a-spin>
            </a-space>
            <div class="prompt-editor">

              <div class="editor-row">
                <a-textarea
                  v-model:value="promptText"
                  placeholder="请输入图像描述..."
                  :rows="4"
                />
              </div>
              <div class="editor-row">
                <a-button type="primary" @click="generateImage" :loading="genLoading">图像生成</a-button>
              </div>
            </div>
          </div>

        </div>
        <div class="resize-handle" @mousedown="startResize"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { message } from 'ant-design-vue'
import { PictureOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { createTextToImageUsingPost } from '@/api/pictureController.ts'
// 导入API请求函数

const showModal = ref(false)
// 生成图像url
const generatedImage = ref('')
// 用户输入的文本
const promptText = ref('')
// 生成加载状态
const genLoading = ref(false)

// 生成图像方法
// 修改 generateImage 方法中的响应处理逻辑
const generateImage = async () => {
  if (!promptText.value.trim()) {
    message.warning('请输入图像描述内容')
    return
  }

  try {
    genLoading.value = true
    generatedImage.value = ''

    const response = await createTextToImageUsingPost({
      userPrompt: promptText.value.trim()
    })

    console.log('完整响应数据:', response) // 确认响应结构

    // 关键修改：访问层级从 response.data.output 改为 response.data.data.output
    if (response?.data?.data?.output?.choices?.length > 0) {
      const choice = response.data.data.output.choices[0]
      if (choice?.message?.content?.length > 0) {
        const imageItem = choice.message.content.find((item: any) => item.image)
        if (imageItem?.image) {
          generatedImage.value = imageItem.image
          message.success('图像生成成功')
          return
        }
      }
    }

    message.warning('未获取到生成的图像')
  } catch (error) {
    console.error('图像生成出错:', error)
    message.error('请求失败，请稍后重试')
  } finally {
    genLoading.value = false
  }
}


// 占位：处理窗口 resize 逻辑
const startResize = (e: MouseEvent) => {
  // 如需实现窗口调整大小功能，可以在这里补充逻辑
  console.log('开始调整大小', e)
}
</script>

<style scoped>
/* 保持原样式不变 */
.container {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  text-align: center;
}

/* 模态窗口样式 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-container {
  position: relative;
  width: 80%;
  height: 70%;
  min-width: 800px;
  min-height: 500px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  padding: 16px;
  background: #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: move;
  border-bottom: 1px solid #d9d9d9;
}

.modal-title {
  font-weight: 500;
  color: #262626;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.3s;
}

.modal-close:hover {
  color: #1890ff;
}

.modal-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 15px;
  height: 15px;
  cursor: nwse-resize;
  z-index: 1002;
}

/* 左侧图像生成区域 */
.image-generation {
  width: 60%;
  height: 100%;
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 16px;
  border-right: 1px solid #f0f0f0;
}

.image-preview {
  flex: 1;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  overflow: hidden;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
}

.prompt-editor {
  display: flex;
  flex-direction: column;
}

.editor-row {
  display: flex;
  margin-bottom: 12px;
  align-items: center;
}

.editor-row:first-child {
  justify-content: flex-end;
}

.editor-row:last-child {
  margin-bottom: 0;
  justify-content: flex-end;
}
</style>
