<!-- UserInfoPage.vue -->
<template>
  <div id="userInfoPage">
    <a-spin :spinning="loading">
      <a-card title="个人信息" style="max-width: 800px; margin: 20px auto;">
        <template #extra>
          <a-button type="primary" @click="goToEditPage">编辑信息</a-button>
        </template>
        <a-descriptions bordered :column="{ xs: 1, sm: 1, md: 1 }">
          <a-descriptions-item label="用户ID">
            {{ userInfo.id || '暂无' }}
          </a-descriptions-item>
          <a-descriptions-item label="用户名">
            {{ userInfo.userName || '暂无' }}
          </a-descriptions-item>
          <a-descriptions-item label="用户账号">
            {{ userInfo.userAccount || '暂无' }}
          </a-descriptions-item>
          <a-descriptions-item label="用户头像">
            <a-avatar :src="userInfo.userAvatar" size="large" v-if="userInfo.userAvatar" />
            <span v-else>暂无头像</span>
          </a-descriptions-item>
          <a-descriptions-item label="用户角色">
            <div v-if="userInfo.userRole === 'admin'">
              <a-tag color="green">管理员</a-tag>
            </div>
            <div v-else>
              <a-tag color="blue">普通用户</a-tag>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="个人简介">
            {{ userInfo.userProfile || '暂无' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDate(userInfo.createTime) || '暂无' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </a-spin>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue'
import { getLoginUserUsingGet, getUserVoByIdUsingGet } from '@/api/userController'
import { message } from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router'

// 用户信息数据
const userInfo = ref<API.LoginUserVO>({});
const loading = ref<boolean>(true);
const router = useRouter();

/**
 * 格式化日期时间
 * @param dateStr 日期字符串
 * @returns 格式化后的日期字符串
 */
const formatDate = (dateStr: string | undefined): string => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return isNaN(date.getTime()) ? '' : date.toLocaleString();
};

/**
 * 获取登录用户信息
 */
const fetchUserInfo = async () => {
  try {
    loading.value = true;
    // 1. 先获取当前登录用户的ID（如果本地没有缓存ID，可通过原接口获取）
    let userId: number | undefined;
    const loginUserRes = await getLoginUserUsingGet({ t: new Date().getTime() }); // 加时间戳防缓存
    if (loginUserRes.data.code === 0 && loginUserRes.data.data) {
      userId = loginUserRes.data.data.id;
    } else {
      message.error('获取用户ID失败');
      return;
    }

    // 2. 通过用户ID调用新接口，获取最新信息（强制获取最新数据）
    const res = await getUserVoByIdUsingGet(
      { id: userId, t: new Date().getTime() }, // 传入用户ID + 时间戳防缓存
      { refresh: true } // 部分请求库可加此参数强制刷新
    );

    if (res.data.code === 0 && res.data.data) {
      userInfo.value = res.data.data; // 更新为最新数据
    } else {
      message.error(res.data.message || '获取用户信息失败');
    }
  } catch (error) {
    message.error('请求失败，请稍后再试');
  } finally {
    loading.value = false;
  }
};

// 跳转到编辑页面
const goToEditPage = () => {
  router.push('/user/infoEdit');
};

// 页面加载时获取用户信息
onMounted(() => {
  fetchUserInfo();
});

const route = useRoute(); // 获取当前路由实例

// 监听路由变化，每次进入当前页面都重新请求数据
watch(
  () => route.path, // 监听路由路径
  (newPath) => {
    if (newPath === '/user/info') { // 确认当前页面是个人信息页
      fetchUserInfo(); // 重新获取用户信息
    }
  },
  { immediate: true } // 初始化时立即执行一次
);
</script>

<style scoped>
#userInfoPage {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 64px);
}

.ant-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-preview {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
