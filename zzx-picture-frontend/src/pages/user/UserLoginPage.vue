<template>
  <div id="userLoginPage">
    <h2 class="title">智联云图 - 用户登录</h2>

    <a-form :model="formState" autocomplete="off" name="basic" @finish="handleSubmit">
      <a-form-item
        label="用户账号"
        name="userAccount"
        :rules="[{ required: true, message: '请输入账号' }]"
      >
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>

      <a-form-item
        label="用户密码"
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码长度不能小于8位' },
          { max: 16, message: '密码长度不能大于16位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <div class="tips">
        没有账号？
        <router-link to="/user/register">去注册</router-link>
      </div>

      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

// 用于接受表单输入的值
const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})


const loginUserStore = useLoginUserStore()
const router = useRouter()
/**
 * 表单提交
 * @param values
 */
const handleSubmit = async (values: any) => {
  // console.log('Success:', values)
  const res = await userLoginUsingPost(values)
  // 登录成功，把登录态保存全局状态
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登录失败' + res.data.message)
  }
}
</script>

<style scoped>
#userLoginPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin-bottom: 36px;
  font-size: 20px;
}

.tips {
  text-align: right;
  margin-top: 10px;
  color: #bbb;
  margin-bottom: 16px;
  font-size: 13px;
}
</style>
