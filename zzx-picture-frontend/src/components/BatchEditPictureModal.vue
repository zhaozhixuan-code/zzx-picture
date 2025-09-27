<template>
  <a-modal v-model:visible="visible" title="批量编辑图片" :footer="false" @cancel="closeModal">
    <a-typography-paragraph type="secondary">* 只对当前页面的图片生效</a-typography-paragraph>
    <!-- 表单项 -->
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="分类" name="category">
        <a-auto-complete
          v-model:value="formData.category"
          :options="categoryOptions"
          placeholder="请输入分类"
          allowClear
        />
      </a-form-item>
      <a-form-item label="标签" name="tags">
        <a-select
          v-model:value="formData.tags"
          :options="tagOptions"
          mode="tags"
          placeholder="请输入标签"
          allowClear
        />
      </a-form-item>
      <a-form-item label="命名规则" name="nameRule">
        <a-input v-model:value="formData.nameRule" placeholder="请输入命名规则，输入 {序号} 可动态生成" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">提交</a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { defineProps, ref, withDefaults, defineExpose, reactive, onMounted } from 'vue'
import {
  editPictureByBatchUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController'
import { message } from 'ant-design-vue'

// 定义组件属性类型
interface Props {
  pictureList: API.PictureVO[]
  spaceId: number
  onSuccess: () => void
}

// 给组件指定初始值
const props = withDefaults(defineProps<Props>(), {})

// 控制弹窗可见性
const visible = ref(false)

// 打开弹窗
const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})

// 初始化表单数据
const formData = reactive({
  category: '', // 分类
  tags: [], // 标签
  nameRule: '', // 命名规则
})

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

// 获取标签和分类选项
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    // 转换成下拉选项组件接受的格式
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('加载选项失败，' + res.data.message)
  }
}

// 提交表单时处理
const handleSubmit = async (values: any) => {
  if (!props.pictureList) {
    return
  }
  const res = await editPictureByBatchUsingPost({
    pictureIdList: props.pictureList.map((picture) => picture.id),
    spaceId: props.spaceId,
    ...values,
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    closeModal()
    props.onSuccess?.()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>
