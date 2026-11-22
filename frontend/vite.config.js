// 开发配置：@ 指向 src；/api 请求转发后端并去掉 /api 前缀；/uploads 代理用于预览图片。server.proxy 只在开发服务器生效。
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
export default defineConfig({ plugins:[vue()], resolve:{alias:{'@':fileURLToPath(new URL('./src',import.meta.url))}},server:{proxy:{'/api':{target:process.env.VITE_API_TARGET || 'http://localhost:8080',changeOrigin:true,rewrite:path=>path.replace(/^\/api/,'')},'/uploads':{target:process.env.VITE_API_TARGET || 'http://localhost:8080',changeOrigin:true}}}});