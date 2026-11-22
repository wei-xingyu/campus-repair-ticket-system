import {createRouter, createWebHashHistory} from 'vue-router';
import {user, getToken, clearSession} from '@/utils/session';
import {meApi} from '@/api/auth';

// hash 路由的地址带 #；component: () => import(...) 表示进入该页面时再加载组件。
// children 在 layout 的 router-view 中显示；:type 是动态参数，meta.admin 是自定义权限标记。
const router = createRouter({
    history: createWebHashHistory(),
    routes: [{path: '/login', component: () => import('@/views/login/index.vue')}, {
        path: '/',
        component: () => import('@/views/layout/index.vue'),
        children: [{path: '', redirect: '/tickets'}, {
            path: 'tickets',
            component: () => import('@/views/ticket/index.vue')
        }, {
            path: 'basic/:type',
            component: () => import('@/views/basic/index.vue'),
            meta: {admin: true}
        }, {
            path: 'statistics',
            component: () => import('@/views/statistics/index.vue'),
            meta: {admin: true}
        }, {
            path: 'operation-logs',
            component: () => import('@/views/operation-log/index.vue'),
            meta: {admin: true}
        }]
    }, {path: '/:pathMatch(.*)*', redirect: '/'}]
});
// 全局前置守卫：每次跳转前执行，返回路径表示改去该页面，返回 true 或不返回值表示放行。
router.beforeEach(async to => {
    if (to.path === '/login') return true;
    if (!getToken()) {
        clearSession();
        return '/login';
    }
// 刷新后使用保存的 Token 调用 /auth/me 恢复用户资料。
    // 恢复失败就回登录页；这里的用户角色来自服务端，不让用户自己选择身份。
    if (!user.value) {
        try {
            user.value = (await meApi()).data;
        } catch {
            return '/login';
        }
    }
    if (to.meta.admin && user.value.role !== 'ADMIN') return '/tickets';
});
export default router;
