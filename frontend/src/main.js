// 项目入口：创建 Vue 应用，注册 Element Plus（中文）、路由，再挂载到 index.html 的 #app。
import {createApp} from 'vue';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';
import './assets/main.css';
import App from './App.vue';
import router from './router';

createApp(App).use(ElementPlus, {locale: zhCn}).use(router).mount('#app');