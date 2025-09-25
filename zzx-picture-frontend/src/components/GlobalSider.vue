<template>
  <div class="globalSider">
    <div id="globalSider">
      <a-layout-sider class="sider" width="200" breakpoint="lg" collapsed-width="0">
        <a-menu
          mode="inline"
          v-model:selectedKeys="current"
          :items="menuItems"
          @click="doMenuClick"
        />
      </a-layout-sider>
    </div>

  </div>
</template>
<script lang="ts" setup>
import { PictureOutlined ,UserOutlined} from '@ant-design/icons-vue'
import { type MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import { h, ref, computed } from 'vue'


const loginUserStore = useLoginUserStore()

// 菜单列表
const menuItems = [
  {
    key: '/',
    label: '公共图库',
    icon: () => h(PictureOutlined),
  },
  {
    key: '/my_space',
    label: '我的空间',
    icon: () => h(UserOutlined),
  },
]

const router = useRouter()

// 当前选中菜单
const current = ref<string[]>([])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, failure) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key,
  })
}

</script>

<style scoped>
#globalSider .ant-layout-sider{
  background: none;
}

</style>
