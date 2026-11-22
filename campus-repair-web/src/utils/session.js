// 模块导出的 ref 由多个页面共享；只存当前用户资料，页面刷新后通过 /auth/me 恢复，不存密码。
import {ref} from 'vue';
export const user=ref(null);
export const roles={STUDENT:'报修人',REPAIRER:'维修人员',ADMIN:'管理员'};

// Token 仅在当前标签页会话中保存，用户资料仍由后端确认。
const TOKEN_KEY = 'campus:token';
export const getToken = () => sessionStorage.getItem(TOKEN_KEY);
export const setToken = token => sessionStorage.setItem(TOKEN_KEY, token);
export const clearSession = () => {
    sessionStorage.removeItem(TOKEN_KEY);
    user.value = null;
};
