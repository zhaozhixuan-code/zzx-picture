<template>
  <div class="picture-list">
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 2, sm: 2, md: 3, lg: 3, xl: 4, xxl: 4 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <!-- 单张图片 -->
          <div class="picture-card-wrapper" @click="doClickPicture(picture)">
            <div class="image-container">
              <img
                class="picture-image"
                :alt="picture.name"
                :src="picture.thumbnailUrl ?? picture.url"
                loading="lazy"
              />
              <div class="overlay">
                <div class="overlay-content">
                  <!-- 标题放在顶部 -->
                  <div class="overlay-title">
                    <h3 class="picture-overlay-title">{{ picture.name }}</h3>
                  </div>

                  <!-- 分类和标签放在底部 -->
                  <div class="overlay-bottom">
                    <div class="tags-container">
                      <a-tag color="green">
                        {{ picture.category ?? '默认' }}
                      </a-tag>
                      <a-tag v-for="tag in picture.tags" :key="tag" color="blue">
                        {{ tag }}
                      </a-tag>
                    </div>

                    <!-- 操作按钮也放在底部 -->
                    <div v-if="showOp" class="action-buttons-overlay">
                      <a-button
                        size="small"
                        @click="(e) => doSearch(picture, e)"
                        class="action-btn-overlay"
                      >
                        <search-outlined /> 搜索
                      </a-button>
                      <a-button
                        size="small"
                        v-if="canEdit"
                        @click="(e) => doEdit(picture, e)"
                        class="action-btn-overlay"
                      >
                        <edit-outlined /> 编辑
                      </a-button>
                      <a-button
                        size="small"
                        @click="(e) => doShare(picture, e)"
                        class="action-btn-overlay"
                      >
                        <ShareAltOutlined /> 分享
                      </a-button>
                      <a-button
                        size="small"
                        v-if="canDelete"
                        @click="(e) => doDelete(picture, e)"
                        class="action-btn-overlay"
                      >
                        <delete-outlined /> 删除
                      </a-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-list-item>
      </template>
    </a-list>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import ShareModal from '@/components/ShareModal.vue'
import { ref } from 'vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
})

// 跳转至图片详情
const router = useRouter()
const doClickPicture = (picture) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 搜索
const doSearch = (picture, e) => {
  e.stopPropagation()
  window.open(`/search_picture?pictureId=${picture.id}`)
}

// 编辑
const doEdit = (picture, e) => {
  e.stopPropagation()
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除
const doDelete = async (picture, e) => {
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  const deleteRequest = { id: id }
  const res = await deletePictureUsingPost(deleteRequest)
  if (res.data.code === 0) {
    message.success('删除成功')
    // 让外层刷新
    props?.onReload()
  } else {
    message.error('删除失败')
  }
}

// 分享弹窗引用
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()

// 分享
const doShare = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
.picture-card-wrapper {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.3s ease;
  cursor: pointer;
  background: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.picture-card-wrapper:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.image-container {
  position: relative;
  height: 180px;
  overflow: hidden;
  flex-shrink: 0;
}

.picture-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.image-container:hover .picture-image {
  transform: scale(1.05);
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: white;
}

.image-container:hover .overlay {
  opacity: 1;
}

.overlay-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 12px;
}

.overlay-title {
  text-align: center;
}

.picture-overlay-title {
  margin: 0;
  font-size: 16px;
  color: white;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.overlay-bottom {
  width: 100%;
}

.tags-container {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.action-buttons-overlay {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-btn-overlay {
  flex: 1;
  min-width: calc(50% - 4px);
  font-size: 12px;
  justify-content: center;
}

@media (max-width: 576px) {
  .action-btn-overlay {
    min-width: 100%;
  }
}
</style>
