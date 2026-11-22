import request from '@/utils/request';

export const operationLogsApi = params => request.get('/admin/operation-logs', {params});
