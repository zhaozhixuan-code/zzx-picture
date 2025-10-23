<template>
  <div id="homePage">
    <!-- 搜索框 -->
    <div class="search-bar">
      <a-input-search
        placeholder="从海量图片中搜索"
        v-model:value="searchParams.searchText"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>
    <!-- 分类和标签 -->
    <a-tabs v-model:active-key="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
    </a-tabs>
    <div class="tag-bar">
      <span style="margin-right: 8px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>
    <!-- 瀑布流图片列表 -->
    <div class="waterfall-container">
      <div class="waterfall-column" v-for="(column, columnIndex) in columns" :key="columnIndex">
        <div
          class="picture-card"
          v-for="picture in column"
          :key="picture.id"
          @click="doClickPicture(picture)"
        >
          <div class="image-container">
            <img
              :alt="picture.name"
              :src="picture.thumbnailUrl ?? picture.url"
              loading="lazy"
            />
            <div class="overlay">
              <div class="overlay-content">
                <h3 class="picture-title">{{ picture.name }}</h3>
                <div class="picture-info">
                  <span class="category-tag">{{ picture.category ?? '默认' }}</span>
                  <span v-for="tag in picture.tags" :key="tag" class="info-tag">{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 分页 -->
    <a-pagination
      style="text-align: right; margin-top: 20px;"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 瀑布流列数
const columnCount = ref(4)
const columns = ref<API.PictureVO[][]>([])

// 更新列数的函数
const updateColumnCount = () => {
  const width = window.innerWidth
  if (width < 576) {
    columnCount.value = 1
  } else if (width < 768) {
    columnCount.value = 2
  } else if (width < 992) {
    columnCount.value = 3
  } else {
    columnCount.value = 4
  }
  distributeImages()
}

// 分发图片到各列
const distributeImages = () => {
  // 初始化列
  columns.value = Array.from({ length: columnCount.value }, () => [])

  // 将图片分配到各列（按顺序循环分配）
  dataList.value.forEach((picture, index) => {
    const columnIndex = index % columnCount.value
    columns.value[columnIndex].push(picture)
  })
}

const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 20,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    onChange: (page: number, pageSize: number) => {
      searchParams.current = page
      searchParams.pageSize = pageSize
      fetchData()
    },
  }
})

// 获取标签和分类选项
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    // 转换成下拉选项组件接受的格式
    categoryList.value = res.data.data.categoryList ?? []
    tagList.value = res.data.data.tagList ?? []
    selectedTagList.value = new Array(tagList.value.length).fill(false)
  } else {
    message.error('加载分类标签失败，' + res.data.message)
  }
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })

  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
    distributeImages()
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.current = 1
  fetchData()
}

const router = useRouter()
// 跳转到图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 监听窗口大小变化
let resizeTimer: number
const handleResize = () => {
  clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => {
    updateColumnCount()
  }, 100)
}

// 页面加载时请求一次
onMounted(() => {
  updateColumnCount()
  fetchData()
  getTagCategoryOptions()
  window.addEventListener('resize', handleResize)
})

// 组件销毁时移除事件监听器
// 注意：在 Vue 3 Composition API 中，需要在适当的生命周期钩子中清理
// 这里为了简化没有添加，实际项目中建议添加

// 监听数据变化重新分配图片
watch(dataList, distributeImages)
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
  margin-left: 16px;
  margin-right: 16px;
}

#homePage .search-bar {
  max-width: 480px;
  margin: 0 auto;
  margin-bottom: 16px;
}

#homePage .tag-bar {
  margin-bottom: 16px;
}

/* 瀑布流容器 */
.waterfall-container {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -8px;
}

.waterfall-column {
  flex: 1;
  min-width: 250px;
  padding: 0 8px;
}

.picture-card {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.3s ease;
  margin-bottom: 16px;
  background: #fff;
}

.picture-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.image-container {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
}

.image-container img {
  width: 100%;
  height: auto;
  display: block;
  transition: transform 0.3s ease;
}

.image-container:hover img {
  transform: scale(1.05);
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  color: white;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-container:hover .overlay {
  opacity: 1;
}

.overlay-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px;
  background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);
}

.picture-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: white;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picture-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-tag,
.info-tag {
  font-size: 12px;
  color: white;
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
}

.category-tag {
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 576px) {
  .waterfall-column {
    min-width: 100%;
  }
}

@media (min-width: 576px) and (max-width: 768px) {
  .waterfall-column {
    min-width: calc(50% - 16px);
  }
}

@media (min-width: 768px) and (max-width: 992px) {
  .waterfall-column {
    min-width: calc(33.333% - 16px);
  }
}

@media (min-width: 992px) {
  .waterfall-column {
    min-width: calc(25% - 16px);
  }
}
</style>
