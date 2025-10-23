import { createRouter, createWebHistory } from 'vue-router'
import SpaceUserManagePage from '@/pages/admin/SpaceUserManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: () => import('../pages/HomePage.vue'),
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('../pages/user/UserLoginPage.vue'),
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('../pages/user/UserRegisterPage.vue'),
    },
    {
      path: '/user/info',
      name: '用户个人资料',
      component: () => import('../pages/user/UserInfoPage.vue'),
    },
    {
      path: '/user/infoEdit',
      name: '编辑用户个人资料',
      component: () => import('../pages/user/UserInfoEditPage.vue'),
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: () => import('../pages/admin/UserManagePage.vue'),
    },
    {
      path: '/add_picture',
      name: '新建图片',
      component: () => import('../pages/AddPicturePage.vue'),
    },
    {
      path: '/add_space',
      name: '创建空间',
      component: () => import('../pages/AddSpacePage.vue'),
    },
    {
      path: '/my_space',
      name: '我的空间',
      component: () => import('../pages/MySpacePage.vue'),
    },
    {
      path: '/add_picture/batch',
      name: '批量新建图片',
      component: () => import('../pages/AddPictureBatchPage.vue'),
    },
    {
      path: '/admin/pictureManage',
      name: '图片管理',
      component: () => import('../pages/admin/PictureManagePage.vue'),
    },
    {
      path: '/picture/:id',
      name: '图片详情',
      component: () => import('../pages/PictureDetailPage.vue'),
      props: true,
    },
    {
      path: '/admin/spaceManage',
      name: '空间管理',
      component: () => import('../pages/admin/SpaceManagePage.vue'),
    },
    {
      path: '/space/:id',
      name: '空间详情',
      component: () => import('../pages/SpaceDetailPage.vue'),
      props: true,
    },{
      path: '/space_analyze',
      name: '空间分析',
      component: () => import('../pages/SpaceAnalyzePage.vue'),
    },
    {
      path: '/spaceUserManage/:id',
      name: '空间成员管理',
      component: SpaceUserManagePage,
      props: true,
    },
    {
      path: '/search_picture',
      name: '以图搜图',
      component: () => import('../pages/SearchPicturePage.vue'),
    },
  ],
})

export default router
