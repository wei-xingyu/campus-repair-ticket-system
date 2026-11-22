import request from '@/utils/request';
// POST 把账号密码放在请求体；登录成功后由浏览器保存后端 Cookie。
export const loginApi=data=>request.post('/auth/login',data);
// GET 查询已有会话，不需要重新提交密码。
export const meApi=()=>request.get('/auth/me');
// 通知后端销毁会话，不只是清空前端用户资料。
export const logoutApi=()=>request.post('/auth/logout');