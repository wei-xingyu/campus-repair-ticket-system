<script setup>
import {ref} from 'vue';
import {ElMessage} from 'element-plus';
import {uploadApi} from '@/api/ticket';

// 父组件 <ImageUpload v-model="form.attachmentUrls" /> 会传入 modelValue，
// 子组件通过 update:modelValue 事件把新数组交回去，父组件对应的数组才会更新。
// 不直接修改 props；数组默认值用函数返回，避免多个组件实例共享同一个数组。
const props = defineProps({modelValue: {type: Array, default: () => []}});
const emit = defineEmits(['update:modelValue']);
const busy = ref(false);

const upload = async e => {
  // 原生文件输入框给的是 FileList；展开成普通数组后，就能用 some、forEach 等数组方法。
  const files = [...e.target.files];
  // 新选图片 + 已上传图片总共最多 5 张。字节换算：1MB = 1024 × 1024 字节。
  // some 只要发现一个不合格文件就返回 true；前端校验后，后端仍须再次检查真实文件内容。
  if (files.length + props.modelValue.length > 5 || files.some(f =>
      f.size > 5 * 1024 * 1024 || !['image/jpeg', 'image/png', 'image/webp'].includes(f.type))) {
    ElMessage.warning('最多5张图片，单张不超过5MB，支持 JPEG、PNG、WebP');
    e.target.value = '';
    return;
  }
  busy.value = true;
  try {
    // 上传二进制文件使用 FormData，而不是普通 JSON；字段名 files 必须和后端约定一致。
    const data = new FormData();
    files.forEach(f => data.append('files', f));
    const result = await uploadApi(data);
    // 工单只保存上传成功后返回的 URL，不把本地 File 对象提交到创建工单接口。
    emit('update:modelValue', [...props.modelValue, ...result.data.urls]);
  } catch {
    // request 拦截器已经弹出失败提示；失败时不把文件加入已上传列表。
  } finally {
    busy.value = false;
    // 清空原生选择框后，下次选同一文件也能触发 change 事件。
    e.target.value = '';
  }
};
</script>
<!-- 移除按钮通过 filter 生成新数组并通知父组件，只移除本次表单引用，不删除后端文件。 -->
<template><div v-loading="busy"><input type="file" multiple accept="image/jpeg,image/png,image/webp" @change="upload" :disabled="busy"/><div class="images"><div v-for="(url,i) in modelValue" :key="url"><el-image :src="url" :preview-src-list="modelValue" fit="cover"/><el-button text type="danger" @click="emit('update:modelValue',modelValue.filter((_,index)=>index!==i))">移除</el-button></div></div></div></template>