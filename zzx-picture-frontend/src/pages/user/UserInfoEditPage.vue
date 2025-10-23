<!-- UserInfoEditPage.vue -->
<template>
  <div id="userInfoEditPage">
    <a-spin :spinning="loading">
      <a-card title="编辑个人信息" style="max-width: 800px; margin: 20px auto;">
        <a-form :model="form" :rules="rules" ref="formRef" layout="vertical">
          <a-form-item label="用户头像" name="userAvatar">
            <div class="avatar-upload">
              <a-upload
                name="file"
                list-type="picture-card"
                class="avatar-uploader"
                :show-upload-list="false"
                :custom-request="handleAvatarUpload"
              >
                <img v-if="form.userAvatar" :src="form.userAvatar" alt="avatar" style="width: 100%" />
                <div v-else>
                  <loading-outlined v-if="uploading"></loading-outlined>
                  <plus-outlined v-else></plus-outlined>
                  <div class="ant-upload-text">上传头像</div>
                </div>
              </a-upload>
<!--              <p class="upload-tip">支持 JPG、PNG 格式，大小不超过 2MB</p>-->
            </div>
          </a-form-item>

          <a-form-item label="用户名" name="userName">
            <a-input v-model:value="form.userName" placeholder="请输入用户名" />
          </a-form-item>

          <a-form-item label="个人简介" name="userProfile">
            <a-textarea
              v-model:value="form.userProfile"
              placeholder="请输入个人简介"
              :rows="4"
            />
          </a-form-item>

          <a-form-item>
            <a-button type="primary" @click="handleSubmit" :loading="submitting">
              保存信息
            </a-button>
            <a-button style="margin-left: 10px" @click="handleCancel">
              取消
            </a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </a-spin>
  </div>
</template>

<!-- UserInfoEditPage.vue -->
<!-- UserInfoEditPage.vue -->
<script lang="ts" setup>
import { ref, onMounted, watch, onActivated } from 'vue'
import { message } from 'ant-design-vue';
import {
  editUserUsingPost,
  getLoginUserUsingGet,
  getUserVoByIdUsingGet,
  updateUserUsingPost
} from '@/api/userController'
import { uploadPictureUsingPost } from '@/api/pictureController';
import { useRoute, useRouter } from 'vue-router'
import type { UploadProps } from 'ant-design-vue';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons-vue';
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts';
import { listSpaceVoByPageUsingPost } from '@/api/spaceController';

// 初始化 store
const loginUserStore = useLoginUserStore();

// 表单数据
const form = ref<API.UserUpdateRequest>({
  id: undefined,
  userName: '',
  userProfile: '',
  userAvatar: '',
  userRole: ''
});

const loading = ref<boolean>(true);
const submitting = ref<boolean>(false);
const uploading = ref<boolean>(false);
const formRef = ref();
const router = useRouter();
const route = useRoute();

// 用户空间列表
const userSpaces = ref<API.SpaceVO[]>([]);

// 表单验证规则
const rules = {
  userName: [
    { required: true, message: '请输入用户名' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符' }
  ],
  userProfile: [
    { max: 500, message: '个人简介不能超过500个字符' }
  ]
};

/**
 * 获取用户空间列表
 */
const fetchUserSpaces = async () => {
  try {
    const res = await listSpaceVoByPageUsingPost({
      current: 1,
      pageSize: 100,
      userId: form.value.id
    });

    if (res.data.code === 0 && res.data.data) {
      userSpaces.value = res.data.data.records || [];
    }
  } catch (error) {
    console.error('获取用户空间列表失败', error);
  }
};

/**
 * 获取登录用户信息
 */
const fetchUserInfo = async () => {
  try {
    loading.value = true;
    // 添加随机时间戳，避免GET请求缓存
    const res = await getLoginUserUsingGet({ t: new Date().getTime() });

    if (res.data.code === 0 && res.data.data) {
      const userData = res.data.data;
      // 强制覆盖表单数据
      form.value = {
        id: userData.id,
        userName: userData.userName || '',
        userProfile: userData.userProfile || '',
        userAvatar: userData.userAvatar || '',
        userRole: userData.userRole || ''
      };

      // 获取用户空间列表
      await fetchUserSpaces();
    } else {
      message.error(res.data.message || '获取用户信息失败');
    }
  } catch (error) {
    message.error('请求失败，请稍后再试');
  } finally {
    loading.value = false;
  }
};

/**
 * 处理头像上传
 */
const handleAvatarUpload = async ({ file }: any) => {
  // 检查用户是否有空间
  if (userSpaces.value.length === 0) {
    message.warning('请先创建一个空间再上传头像');
    return;
  }

  uploading.value = true;
  try {
    // 使用用户第一个空间作为默认空间
    const defaultSpaceId = userSpaces.value[0].id || 1;

    // 上传图片到后端
    const res = await uploadPictureUsingPost(
      { spaceId: defaultSpaceId },
      {},
      file
    );

    if (res.data.code === 0 && res.data.data) {
      // 设置返回的图片地址
      form.value.userAvatar = res.data.data.url || res.data.data.thumbnailUrl || '';
      message.success('头像上传成功');
    } else {
      message.error(res.data.message || '头像上传失败');
    }
  } catch (error) {
    message.error('头像上传失败');
  } finally {
    uploading.value = false;
  }
};
const handleSubmit = async () => {
  try {
    submitting.value = true;
    await formRef.value.validateFields();

    // 第一步：提交更新请求
    const updateRes = await editUserUsingPost(form.value);
    if (updateRes.data.code !== 0) {
      message.error(updateRes.data.message || '更新失败');
      return;
    }

    // 第二步：更新成功后，用 getUserVoByIdUsingGet 获取最新完整用户信息
    if (!form.value.id) {
      message.error('用户ID不存在，无法同步信息');
      return;
    }
    const latestUserRes = await getUserVoByIdUsingGet({
      id: form.value.id,
      t: new Date().getTime() // 加时间戳，避免缓存旧数据
    });
    if (latestUserRes.data.code !== 0 || !latestUserRes.data.data) {
      message.error('获取最新用户信息失败，头部信息可能未更新');
      return;
    }

    // 第三步：同步最新数据到全局状态（关键步骤）
    loginUserStore.setLoginUser(latestUserRes.data.data);

    // 第四步：提示并跳转
    message.success('信息更新成功');
    router.push('/user/info');

  } catch (error) {
    message.error('更新失败，请重试');
  } finally {
    submitting.value = false;
  }
};


/**
 * 取消编辑
 */
const handleCancel = () => {
  router.push('/user/info');
};

// 首次挂载时请求
onMounted(() => {
  fetchUserInfo();
});

// 组件被激活时请求
onActivated(() => {
  fetchUserInfo();
});

// 监听路由变化
watch(
  () => route.path,
  (newPath) => {
    if (newPath === '/user/infoEdit') {
      fetchUserInfo();
    }
  }
);
</script>



<style scoped>
#userInfoEditPage {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 64px);
}

.ant-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-upload {
  text-align: center;
}

.avatar-uploader > .ant-upload {
  width: 128px;
  height: 128px;
}

.upload-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #999;
}
</style>
