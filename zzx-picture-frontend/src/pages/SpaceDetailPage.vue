<template>
  <div id="PictureDetailPage">
    <!-- 空间信息 -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}（{{ SPACE_TYPE_MAP[space.spaceType] }}）</h2>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <a-form layout="inline" class="custom-button-form">
      <a-form-item>
        <a-space>
          <a-button
            v-if="canUploadPicture"
            type="primary"
            :href="`/add_picture?spaceId=${id}`"
            target="_blank"
          >
            + 创建图片
          </a-button>
        </a-space>
      </a-form-item>
      <a-form-item>
        <a-space>
          <!-- 新增AI文生图按钮 -->
<!--          <a-button-->
<!--            v-if="canUploadPicture"-->
<!--            type="primary"-->
<!--            :icon="h(PictureOutlined)"-->
<!--            @click="openTextToImageModal"-->
<!--          >-->
<!--            AI 文生图-->
<!--          </a-button>-->
          <div class="ai-gen-image-wrapper">
            <AiGenImage/>
          </div>
        </a-space>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button
            v-if="canManageSpaceUser"
            type="primary"
            ghost
            :icon="h(TeamOutlined)"
            :href="`/spaceUserManage/${id}`"
            target="_blank"
          >
            成员管理
          </a-button>
        </a-space>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button
            v-if="canManageSpaceUser"
            type="primary"
            ghost
            :icon="h(BarChartOutlined)"
            :href="`/space_analyze?spaceId=${id}`"
          >
            空间分析
          </a-button>
        </a-space>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button v-if="canEditPicture" :icon="h(EditOutlined)" @click="doBatchEdit"> 批量编辑</a-button>
        </a-space>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-tooltip
            :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
          >
            <a-progress
              type="circle"
              :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
              :size="42"
            />
          </a-tooltip>
        </a-space>
      </a-form-item>
    </a-form>

    <div style="margin-bottom: 16px" />
    <!-- 搜索表单 -->
    <PictureSearchForm :onSearch="onSearch" />
    <div style="margin-bottom: 16px" />

    <!-- 按颜色搜索,跟其它搜索条件独立 -->
    <a-form-item label="按颜色搜索" style="margin-top: 16px">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>

    <!-- 图片列表 -->
    <PictureList
      :dataList="dataList"
      :loading="loading"
      :showOp="true"
      :canEdit="canEditPicture"
      :canDelete="canDeletePicture"
      :onReload="fetchData"
    />
    <!-- 分页 -->
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      :show-total="() => `图片总数 ${total} / ${space.maxCount}`"
      @change="onPageChange"
    />
    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="dataList"
      :onSuccess="onBatchEditPictureSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import {
  deletePictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost
} from '@/api/pictureController.ts'
import { onMounted, ref, h, computed, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { downloadImage, formatSize } from '../utils'
import {
  EditOutlined,
  DeleteOutlined,
  DownloadOutlined,
  BarChartOutlined,
  TeamOutlined,
  PictureOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import router from '@/router'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import PictureList from '@/pages/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_MAP } from '../constants/space.ts'
import AiGenImage from '@/components/AiGenImage.vue'

const props = defineProps<{
  id: string | number
}>()
const space = ref<API.SpaceVO>({})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// 分享弹窗引用
const batchEditPictureModalRef = ref()

// 批量编辑成功后，刷新数据
const onBatchEditPictureSuccess = () => {
  fetchData()
}

// 打开批量编辑弹窗
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

// 获取空间详情
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

// 按照颜色搜索
const onColorChange = async (color: string) => {
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id
  })
  if (res.data.code === 0 && res.data.data) {
    const data = res.data.data ?? []
    dataList.value = data
    total.value = data.length
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}
onMounted(() => {
  fetchSpaceDetail()
})

// 数据
const dataList = ref([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend'
})

// 分页参数
const onPageChange = (page, pageSize) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchData()
}

// 搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1
  }
  fetchData()
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    spaceId: props.id,
    ...searchParams.value
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 编辑
const doEdit = () => {
  router.push('/add_picture?id=' + picture.value.id)
}
// 删除
const doDelete = async () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  const deleteRequest = { id: id }
  const res = await deletePictureUsingPost(deleteRequest)
  if (res.data.code === 0) {
    message.success('删除成功')
    router.push('/')
  } else {
    message.error('删除失败')
  }
}

// 处理下载
const doDownload = () => {
  downloadImage(picture.value.url)
}

watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  }
)
</script>

<style scoped>
#PictureDetailPage {
  margin-bottom: 16px;
  margin-left: 32px;
  margin-right: 32px;
}

.picture :deep {
  margin-left: 15%;
}

.info-card :deep(*) {
  font-size: 16px;
}
/* 针对垂直排列状态的表单项设置间距 */
.custom-button-form {
  /* 覆盖 inline 布局在垂直排列时的默认样式 */
  &.ant-form-inline {
    /* 响应式断点：当屏幕宽度小于 768px 时垂直排列 */
    @media (max-width: 768px) {
      /* 调整每个表单项的底部间距 */
      .ant-form-item {
        margin-bottom: 16px !important; /* 按钮之间的垂直间隙，可根据需要调整 */
      }

      /* 最后一个表单项移除底部间距，避免多余空白 */
      .ant-form-item:last-child {
        margin-bottom: 0 !important;
      }
    }
  }
}
</style>
