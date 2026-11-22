<script setup>
/* 四种基础数据共用页面；结合 api/basic.js 看 type 如何变成实际请求路径。 */
import {ref, computed, onMounted} from 'vue';
import {useRoute} from 'vue-router';
import {ElMessage} from 'element-plus';
import {adminListApi, adminDetailApi, saveApi, statusApi} from '@/api/basic';

// 同一页面通过路由参数 type 复用四种管理界面，例如 /basic/categories。layout 的路由 key 让切换模块时重建页面。
const route = useRoute(), type = route.params.type,
    labels = {categories: '故障类别', buildings: '楼宇', assets: '设备', repairers: '维修人员'}, title = labels[type],
    rows = ref([]), buildings = ref([]), buildingId = ref(null), keyword = ref(''), busy = ref(false), error = ref(''),
    visible = ref(false), form = ref({}), saving = ref(false), statusFilter = ref('all'), detail = ref(null);
// 对象的 key 是接口字段名，value 是中文标签；模板遍历它生成输入框和表格列，避免写四套相似页面。
const fields = computed(() => ({
  categories: {name: '类别名称', description: '说明', sortOrder: '排序'},
  buildings: {name: '楼宇名称', address: '地址'},
  assets: {name: '设备名称', assetCode: '设备编号', location: '位置'},
  repairers: {username: '账号', password: '初始密码', name: '姓名', phone: '手机号'}
}[type] || {}));
const search = async () => {
  rows.value = [];
  error.value = '';

  busy.value = true;
  try {
    const params = {
      enabled: statusFilter.value === 'all' ? undefined : statusFilter.value === 'enabled',
      buildingId: type === 'assets' ? buildingId.value ?? undefined : undefined,
      keyword: type === 'repairers' ? keyword.value.trim() || undefined : undefined
    };
    rows.value = (await adminListApi(type, params)).data;
  } catch (e) {
    error.value = e.response?.data?.msg || e.message;
  } finally {
    busy.value = false;
  }
};
// 编辑时用 {...row} 浅拷贝一份表单，避免还没保存就直接改掉列表里的原对象。当前字段都是简单值，浅拷贝够用。
const edit = row => {
  form.value = row ? {...row} : {name: '', sortOrder: 0, buildingId: buildingId.value};
  visible.value = true;
};
const openRecord = async row => {
  if (type === 'categories') {
    const {id, name, description, sortOrder} = row;
    edit({id, name, description, sortOrder});
    return;
  }
  try {
    const {data} = await adminDetailApi(type, row.id);
    if (type === 'repairers') detail.value = data;
    else edit(data);
  } catch {
    // 请求封装已提示错误。
  }
};
// 先校验必填字段。API 根据有没有 id 决定新增或修改，成功后重新查询，让列表以服务端数据为准。
const save = async () => {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请填写名称');
    return;
  }
  if (type === 'assets' && !form.value.buildingId) {
    ElMessage.warning('请选择楼宇');
    return;
  }
  if (type === 'repairers' && (!form.value.username || !form.value.password || form.value.password.length < 8)) {
    ElMessage.warning('请填写账号与至少8位初始密码');
    return;
  }
  saving.value = true;
  try {
    await saveApi(type, form.value);
    visible.value = false;
    ElMessage.success('保存成功');
    search();
  } catch {
    // 请求封装已提示错误；此处不执行成功操作，保留页面供用户重试。
  } finally {
    saving.value = false;
  }
};
// enabled 是布尔值：true 启用、false 停用。这是状态修改，不是物理删除。
const changeStatus = async (id, enabled) => {
  try {
    await statusApi(type, id, enabled);
    ElMessage.success('状态更新成功');
    search();
  } catch {
    // 请求封装已提示错误；此处不执行成功操作，保留页面供用户重试。
  }
};
// 设备页面先加载楼宇选项，包含停用楼宇，便于查找关联设备。
onMounted(async () => {
  if (type === 'assets') try {
    buildings.value = (await adminListApi('buildings')).data;
  } catch {
    // 请求封装已提示错误；此处不执行成功操作，保留页面供用户重试。
  }
  search();
});
</script>
<!-- 模板中：v-model 双向绑定输入值；:属性 传入变量；@事件 调用方法；v-if 按条件决定是否渲染。 -->
<template>
  <div class="heading">
    <div><span class="eyebrow">BASIC DATA</span>
      <h1>{{ title }}管理</h1><span class="muted">维护校园服务基础信息</span></div>
    <el-button type="primary" @click="edit()">＋ 新增{{ title }}</el-button>
  </div>
  <div class="panel">
    <div class="filters">
      <el-select v-if="type==='assets'" v-model="buildingId" placeholder="全部楼宇" clearable @change="search">
        <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id"/>
      </el-select>
      <el-input v-if="type==='repairers'" v-model="keyword" placeholder="姓名 / 账号"/>
      <el-select v-model="statusFilter" @change="search">
        <el-option label="全部状态" value="all"/>
        <el-option label="已启用" value="enabled"/>
        <el-option label="已停用" value="disabled"/>
      </el-select>
      <el-button @click="search">刷新列表</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false"/>
    <el-table :data="rows" v-loading="busy" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="90"/>
      <el-table-column
          v-for="(label,key) in Object.fromEntries(Object.entries(fields).filter(([key])=>key!=='password'))" :key="key"
          :prop="key" :label="label" min-width="120">
        <template #default="{row}">{{ key === 'password' ? '不显示' : row[key] }}</template>
      </el-table-column>
      <el-table-column v-if="type==='repairers'" prop="activeTicketCount" label="在办工单"/>
      <el-table-column label="状态" width="100">
        <template #default="{row}">{{ row.enabled ? '已启用' : '已停用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button link type="primary" @click="openRecord(row)">{{ type === 'repairers' ? '详情' : '编辑' }}</el-button>
          <el-button v-if="row.enabled === false" link type="primary" @click="changeStatus(row.id,true)">启用</el-button>
          <el-popconfirm v-else title="确认停用此记录？" @confirm="changeStatus(row.id,false)">
            <template #reference>
              <el-button link type="danger">停用</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
  <el-dialog :model-value="!!detail" title="维修人员详情" @close="detail = null">
    <el-descriptions v-if="detail" :column="1">
      <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
      <el-descriptions-item label="账号">{{ detail.username }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ detail.phone || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="在办工单">{{ detail.activeTicketCount }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detail.enabled ? '已启用' : '已停用' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
  <el-dialog v-model="visible" :title="(form.id?'编辑':'新增')+title" width="min(540px,95vw)">
    <el-form label-width="90px">
      <el-form-item v-if="type==='assets'" label="楼宇" required>
        <el-select v-model="form.buildingId">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id"/>
        </el-select>
      </el-form-item>
      <el-form-item v-for="(label,key) in fields" :key="key" :label="label"
                    :required="['name','username','password'].includes(key)">
        <el-input-number v-if="key==='sortOrder'" v-model="form[key]"/>
        <el-input v-else v-model="form[key]" :type="key==='password'?'password':'text'"
                  :maxlength="key==='description'||key==='address'?200:key==='name'&&type==='categories'?50:key==='password'?72:100"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>
