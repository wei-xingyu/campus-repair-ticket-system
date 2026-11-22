import request from '@/utils/request';
// 三元表达式：维修人员查询走 /admin/repairers，其余走 /categories、/buildings。
export const queryAllApi=(type,params)=>request.get(type==='repairers'?'/admin/repairers':`/${type}`,{params});
// 设备列表按楼宇查询，文档没有全量设备列表接口。
export const assetsApi=id=>request.get(`/buildings/${id}/assets`);
// request[方法名] 动态选方法：有 id 用 put 修改，无 id 用 post 新增；路径同时补上 id。
export const saveApi=(type,data)=>request[data.id?'put':'post'](`/admin/${type}${data.id?'/'+data.id:''}`,data);
// 启停请求只发送 {enabled}，不发送整个表单。
export const statusApi=(type,id,enabled)=>request.put(`/admin/${type}/${id}/status`,{enabled});
// 统计筛选通过 Query 参数传递。
export const statisticsApi=params=>request.get('/admin/statistics/tickets',{params});
// 管理列表包含启用和停用记录，原查询仍用于业务选择器。
export const adminListApi=(type,params)=>request.get(type==='repairers'?'/admin/repairers/manage':`/admin/${type}`,{params});
export const adminDetailApi=(type,id)=>request.get(`/admin/${type}/${id}`);
