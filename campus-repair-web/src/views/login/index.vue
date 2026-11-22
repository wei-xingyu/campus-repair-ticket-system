<script setup>
/* 表单 v-model → loginApi → 保存当前用户 → router.push 跳转。模板 @submit.prevent 阻止浏览器默认刷新提交。 */
import {ref} from 'vue';
import {useRouter} from 'vue-router';
import {loginApi} from '@/api/auth';
import {user, setToken} from '@/utils/session';

// useRouter 用来跳转页面；form 绑定账号密码输入框，loading 控制按钮加载状态。
const router = useRouter(), form = ref({username: '', password: ''}), loading = ref(false), error = ref('');
// 点击登录或按回车提交都会进入这里；账号去首尾空格，密码保留原样。
const login = async () => {
  if (!form.value.username.trim() || !form.value.password) {
    error.value = '请输入账号和密码';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
// 保存登录凭证，用户资料用于页面展示。
    const data = (await loginApi({...form.value, username: form.value.username.trim()})).data;
    if (!data?.token) throw new Error('登录响应缺少 Token');
    setToken(data.token);
    const {token, ...profile} = data;
    user.value = profile;
    router.push('/');
  } catch (e) {
    error.value = e.response?.data?.msg || e.message;
// finally 无论成功或失败都会执行，确保请求结束后按钮恢复可点击。
  } finally {
    loading.value = false;
  }
};
</script>
<!-- 模板中：v-model 双向绑定输入值；:属性 传入变量；@事件 调用方法；v-if 按条件决定是否渲染。 -->
<template>
  <div class="login">
    <section><span class="eyebrow">CAMPUS CARE · 校园服务</span>
      <h1>让每一次报修，<br>都有回应。</h1>
      <p>从提交到解决，连接同学、维修人员与校园管理。</p></section>
    <el-card class="login-card"><h2>欢迎登录</h2>
      <p class="muted">校园报修与工单协作平台</p>
      <el-form @submit.prevent="login">
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号" size="large" autocomplete="username"/>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" type="password" show-password size="large"
                    autocomplete="current-password"/>
        </el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false"/>
        <el-button native-type="submit" type="primary" size="large" :loading="loading"
                   style="width:100%;margin-top:20px">登录平台
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>