<template>
  <div class="globalHeader">
    <a-row :wrap="false">
      <!--左侧图标-->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" alt="logo" />
            <div class="title">理想智能图库</div>
          </div>
        </RouterLink>
      </a-col>
      <!--菜单-->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <!--右侧登录按钮-->
      <a-col flex="120px">
        <div class="user-login-status">
          <div class="user-login-status">
            <div v-if="loginUserStore.loginUser.id">
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </div>
            <div v-else>
              <a-button type="primary" href="/user/login">登录</a-button>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { h, ref } from 'vue'
import { HomeOutlined } from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const loginUserStore = useLoginUserStore()

const items = ref<MenuProps['items']>([
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
  },
  {
    key: '/other',
    label: h('a', { href: 'https://www.bilibili.com', target: '_blank' }, '其他'),
    title: '其他',
  },
])

const router = useRouter()
// 路由跳转事件
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key,
  })
}
// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新高亮项
router.afterEach((to, from, next) => {
  current.value = [to.path]
})
</script>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
}

.title-bar .title {
  color: #000000;
  font-size: 18px;
  margin-left: 16px;
}

.title-bar .logo {
  height: 48px;
}
</style>
