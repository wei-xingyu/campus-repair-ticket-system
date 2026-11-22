import request from '@/utils/request';
// GET 的 {params} 会生成 ?page=1&pageSize=10 等查询字符串。
export const queryPageApi=params=>request.get('/tickets',{params});
// 模板字符串 ${id} 把工单 ID 放进路径；注意这里使用反引号。
export const queryByIdApi=id=>request.get(`/tickets/${id}`);
// POST 的第二个参数 data 是 JSON 请求体。
export const addApi=data=>request.post('/tickets',data);
// 复用工单动作请求，例如 action 为 accept 时访问 /tickets/123/accept。
export const actionApi=(id,action,data)=>request.post(`/tickets/${id}/${action}`,data);
export const historyApi=id=>request.get(`/tickets/${id}/history`);
// 这里传 FormData，浏览器会自动设置 multipart/form-data 及 boundary，不要手动写死 Content-Type。
export const uploadApi=data=>request.post('/files/ticket-images',data);
