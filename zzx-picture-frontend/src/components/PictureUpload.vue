<template>
  <div class="picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :custom-request="handleUpload"
      :before-upload="beforeUpload"
    >
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">点击或拖拽上传</div>
      </div>
    </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { UploadChangeParam, UploadProps } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  onSuccess?: (picture: API.PictureVO) => void
}

const props = defineProps<Props>()
/**
 * 上传文件
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params = props.picture ? { id: props.picture.id } : {}
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('上传图片成功')
      props.onSuccess?.(res.data.data)
    } else {
      message.error('上传图片失败:' + res.data.message)
    }
  } catch (error) {
    console.error('上传图片失败' + error)
    message.error('上传图片失败:' + res.data.message)
  }
  loading.value = false
}

const loading = ref<boolean>(false)

/**
 * 上传前的校验
 * @param file
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('不支持上传该格式的文件，推荐 jpg 或 png')
  }
  // 校验图片大小，不能超过20MB
  const isLt2M = file.size / 1024 / 1024 < 20
  if (!isLt2M) {
    message.error('图片不能超过20MB')
  }
  return isJpgOrPng && isLt2M
}
</script>
<style scoped>
.picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-height: 152px;
  min-width: 152px;
}

.picture-upload img {
  max-width: 100%;
  max-height: 480px;
}

.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}

</style>
