<template>
  <div id="app">
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

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
