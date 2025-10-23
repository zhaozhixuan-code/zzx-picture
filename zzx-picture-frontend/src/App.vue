<template>
  <div id="app">
    <a-config-provider :locale="zhCN">
    <BasicLayout />
    </a-config-provider>
  </div>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');

// 改为在权限校验中获取
// import { healthUsingGet } from '@/api/mainController.ts'
// import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
//
// const loginUserStore = useLoginUserStore()
// loginUserStore.fetchLoginUser()
//
// healthUsingGet().then((res) => {
//   console.log(res)
// })
</script>

<style scoped></style>
