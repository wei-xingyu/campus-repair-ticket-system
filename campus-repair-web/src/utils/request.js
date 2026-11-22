import axios from 'axios';
import {getToken, clearSession} from './session';
import {ElMessage} from 'element-plus';

// 统一 Axios 实例：页面请求 /api/tickets，Vite 代理后后端收到 /tickets；timeout 单位为毫秒。
// 统一从当前标签页读取 Token，附加到请求头。
const request = axios.create({baseURL: '/api', timeout: 15000});
request.interceptors.request.use(config => {
    const token = getToken();
    if (token && !config.url?.includes('/auth/login')) {
        config.headers.Authorization = token;
    }
    return config;
});
// 响应拦截器统一处理结果：HTTP 请求成功不代表业务成功，还要判断后端 code 是否为 1。
request.interceptors.response.use(response => {
    if (response.data.code !== 1) {
        const error = new Error(response.data.msg || '操作失败');
        ElMessage.error(error.message);
    // 把失败继续交给页面 catch，阻止失败请求进入后面的“成功提示、关闭弹窗”等逻辑。
    return Promise.reject(error);
    }
// 去掉 Axios 外层包装，所以页面收到的是 {code,msg,data}，页面 result.data 才是业务数据。
    return response.data;
// 第二个回调处理网络错误及非 2xx HTTP 响应。没有 error.response 通常表示没收到服务器响应。
}, error => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
        clearSession();
        location.hash = '/login';
    }
    ElMessage.error(error.response?.data?.msg || (error.response?.status === 404 ? '接口尚未实现或资源不存在' : error.message));
    return Promise.reject(error);
});
export default request;