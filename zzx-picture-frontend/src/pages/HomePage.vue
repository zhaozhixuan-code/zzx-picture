<template>
  <div id="homePage">
    <!-- 搜索 -->
    <div class="search-bar">
      <a-input-search
        placeholder="从海量图片中搜索"
        v-model:value="searchParams.searchText"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>

    <!-- 分类 -->
    <a-tabs v-model:activeKey="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane v-for="c in categoryList" :key="c" :tab="c" />
    </a-tabs>

    <!-- 标签 -->
    <div class="tag-bar">
      <span style="margin-right: 8px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, idx) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[idx]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>

    <!-- 瀑布流 -->
    <div v-if="loading" class="loading-wrap">
      <a-spin size="large" />
    </div>
    <div v-else class="masonry">
      <div
        v-for="p in dataList"
        :key="p.id"
        class="brick"
        @click="doClickPicture(p)"
      >
        <div class="picture-card">
          <img :src="p.url" :alt="p.name" />
          <div class="overlay">
            <h3 class="picture-title">{{ p.name }}</h3>
            <div class="picture-info">
              <span class="category-tag">{{ p.category ?? '默认' }}</span>
              <span v-for="t in p.tags" :key="t" class="info-tag">
                {{ t }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <a-pagination
      class="pagination"
      v-model:current="searchParams.current"
      :page-size="searchParams.pageSize"
      :total="total"
      show-size-changer
      @change="pagination.onChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

/* ================== 数据 ================== */
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 50,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const categoryList = ref<string[]>([])
const selectedCategory = ref('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

/* ================== 分页 ================== */
const pagination = computed(() => ({
  current: searchParams.current ?? 1,
  pageSize: searchParams.pageSize ?? 50,
  total: total.value,
  onChange: (page: number, pageSize: number) => {
    searchParams.current = page
    searchParams.pageSize = pageSize
    fetchData()
  },
}))

/* ================== 接口 ================== */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    categoryList.value = res.data.data.categoryList ?? []
    tagList.value = res.data.data.tagList ?? []
    selectedTagList.value = new Array(tagList.value.length).fill(false)
  } else {
    message.error('加载分类标签失败，' + res.data.message)
  }
}

const fetchData = async () => {
  loading.value = true
  const params = { ...searchParams, tags: [] as string[] }
  if (selectedCategory.value !== 'all') params.category = selectedCategory.value
  selectedTagList.value.forEach((use, i) => {
    if (use) params.tags.push(tagList.value[i])
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

/* ================== 跳转 ================== */
const router = useRouter()
const doClickPicture = (p: API.PictureVO) => {
  router.push(`/picture/${p.id}`)
}

/* ================== 挂载 ================== */
onMounted(() => {
  fetchData()
  getTagCategoryOptions()
})
</script>

<style scoped>
/* ---------- 整体边距 ---------- */
#homePage {
  max-width: 1600px;        /* 不让无限宽，防止列太散 */
  margin: 0 auto;
  padding: 24px;            /* 电脑端只有 24px 边距 */
}
@media (max-width: 768px) {
  #homePage {
    padding: 16px;          /* 移动端再小一点 */
  }
}

/* ---------- 搜索 ---------- */
.search-bar {
  max-width: 480px;
  margin: 0 auto 24px;
}

/* ---------- 标签 ---------- */
.tag-bar {
  margin-bottom: 16px;
}

/* ---------- 加载 ---------- */
.loading-wrap {
  text-align: center;
  padding: 60px 0;
}

/* ---------- 瀑布流：电脑端默认 4 列 ---------- */
.masonry {
  column-gap: 16px;
  column-count: 4;
}
.brick {
  break-inside: avoid;
  margin-bottom: 16px;
}

/* ---------- 卡片 ---------- */
.picture-card {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: box-shadow 0.3s;
}
.picture-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}
.picture-card img {
  width: 100%;
  height: auto;
  display: block;
  transition: transform 0.4s;
}
.picture-card:hover img {
  transform: scale(1.05);
}

/* ---------- 遮罩 ---------- */
.overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7) 0%, transparent 50%);
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 12px;
}
.picture-card:hover .overlay {
  opacity: 1;
}
.picture-title {
  margin: 0 0 6px 0;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picture-info {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.category-tag,
.info-tag {
  font-size: 12px;
  color: #fff;
}
.category-tag {
  font-weight: bold;
}

/* ---------- 分页 ---------- */
.pagination {
  margin-top: 32px;
  text-align: center;
}

/* ---------- 响应式：只改列数，边距保持 ---------- */
@media (max-width: 1200px) {
  .masonry {
    column-count: 3;
  }
}
@media (max-width: 768px) {
  .masonry {
    column-count: 2;
  }
}
@media (max-width: 480px) {
  .masonry {
    column-count: 1;
  }
}
</style>
