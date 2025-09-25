<template>
  <div id="basicLayout">
    <!--使用组件-->
    <a-layout style="min-height: 100vh">
      <!--头部-->
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout>
        <!-- 侧边栏 -->
        <GlobalSider class="sider"/>
        <!--内容-->
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
      <!-- 页脚 -->
      <a-layout-footer class="footer">作者：赵志轩</a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'
</script>

<style scoped>
#basicLayout .header {
  padding-inline: 20px;
  margin-bottom: 1px;
  color: unset;
  background: white;
}

#basicLayout .sider {
  background: #fff;
  border-right: 0.5px solid #eee;
  padding-top: 20px;
}

#basicLayout :deep(.ant-menu-root){
  border-bottom: none !important;
  border-inline-end: none !important;
}

#basicLayout .content {
  background: linear-gradient(to right, #fefefe, #fff);
  margin-bottom: 28px;
  padding: 28px;
}

#basicLayout .footer {
  background-color: #efefef;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
}
</style>
