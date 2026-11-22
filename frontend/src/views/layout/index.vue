<script setup>
/* 布局页：左侧菜单 + 顶部用户信息 + 子路由内容。退出成功后清空共享 user，再返回登录页。 */
import {useRouter} from 'vue-router';
import {user, roles, clearSession} from '@/utils/session';
import {logoutApi} from '@/api/auth';

const router = useRouter();
const logout = async () => {
  try {
    await logoutApi();
    clearSession();
    router.push('/login');
  } catch {
    // 请求封装已提示错误；此处不执行成功操作，保留页面供用户重试。
  }
};
</script>
<template>
  <el-container class="shell">
    <el-aside width="220px">
      <div class="brand">◈ 校园报修<span>CAMPUS CARE</span></div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/tickets">工单工作台</el-menu-item>
        <template v-if="user?.role==='ADMIN'">
          <el-menu-item index="/statistics">统计看板</el-menu-item>
          <el-menu-item
              v-for="(label,key) in {categories:'故障类别',buildings:'楼宇管理',assets:'设备管理',repairers:'维修人员'}"
              :key="key" :index="'/basic/'+key">{{ label }}
          </el-menu-item>
          <el-menu-item index="/operation-logs">操作日志</el-menu-item>
        </template>
      </el-menu>
      <div class="aside-note">校园设施 · 用心守护</div>
    </el-aside>

    <el-container>
      <el-header><span>校园报修与工单协作平台</span>
        <div>{{ user?.name }}
          <el-tag size="small">{{ roles[user?.role] }}</el-tag>
          <el-button text @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view :key="$route.path"/>
      </el-main>
    </el-container>
  </el-container>
</template>
