<template>
  <div id="searchPicturePage">
    <div class="search-picture-container">
      <h2 class="page-title">以图搜图</h2>

      <div class="original-picture-section">
        <h3 class="section-title">原图</h3>
        <a-card class="original-picture-card">
          <template #cover>
            <img
              class="original-picture-img"
              :alt="picture.name"
              :src="picture.thumbnailUrl ?? picture.url"
            />
          </template>
        </a-card>
      </div>

      <a-divider />

      <div class="search-results-section">
        <h3 class="section-title">识图结果</h3>
        <a-list
          class="results-list"
          :grid="{ gutter: 16, xs: 2, sm: 2, md: 3, lg: 3, xl: 4, xxl: 4 }"
          :data-source="dataList"
          :loading="loading"
        >
          <template #renderItem="{ item }">
            <a-list-item class="result-item">
              <a :href="item.fromUrl" target="_blank">
                <a-card class="result-card" hoverable>
                  <template #cover>
                    <img class="result-img" :src="item.thumbUrl" />
                  </template>
                </a-card>
              </a>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPictureVoByIdUsingGet, searchPictureByPictureUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'

const route = useRoute()

// 图片 id
const pictureId = computed(() => {
  return route.query?.pictureId
})

const picture = ref<API.PictureVO>({})

// 以图搜图结果
const dataList = ref<API.ImageSearchResult[]>([])
const loading = ref<boolean>(true)

// 获取搜图结果
const fetchData = async () => {
  loading.value = true
  const res = await searchPictureByPictureUsingPost({
    pictureId: pictureId.value,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 获取老数据
const getOldPicture = async () => {
  // 获取数据
  const id = route.query?.pictureId
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id: id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
    }
  }
}

onMounted(() => {
  getOldPicture()
})
</script>



<style scoped>
.search-picture-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  margin-bottom: 24px;
  text-align: center;
}

.section-title {
  margin: 24px 0 16px 0;
  font-size: 18px;
  font-weight: 600;
}

.original-picture-section {
  text-align: center;
}

.original-picture-card {
  display: inline-block;
  width: 240px;
}

.original-picture-img {
  height: 180px;
  object-fit: cover;
}

.results-list {
  margin-top: 16px;
}

.result-item {
  padding: 0;
}

.result-card {
  transition: transform 0.3s ease;
}

.result-card:hover {
  transform: translateY(-4px);
}

.result-img {
  height: 180px;
  object-fit: cover;
  width: 100%;
}
</style>
