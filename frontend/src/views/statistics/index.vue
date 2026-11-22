<script setup>
/* 统计页只负责筛选和展示，统计计算由后端完成。 */
import {ref, onMounted} from 'vue';
import {statisticsApi, queryAllApi} from '@/api/basic';

// data 初始为 null，表示还没有统计结果，不能把请求失败误显示成全为 0。
const dates = ref([]), buildingId = ref(null), buildings = ref([]), data = ref(null), busy = ref(false),
    error = ref('');
// 后端要本地日期时间字符串。toISOString 默认使用 UTC，这里先补偿时区偏移，再截取到秒，避免时间差 8 小时。
const local = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 19);
const now = new Date();
// 默认查询本月 1 日零点到现在；月份从 0 开始，因此直接使用 getMonth()。
dates.value = [local(new Date(now.getFullYear(), now.getMonth(), 1)), local(now)];
// 日期数组拆成后端的 from/to；没有选择楼宇时不传 buildingId。统计值全部使用后端响应。
const search = async () => {
  if (!dates.value?.length) return;
  busy.value = true;
  data.value = null;
  error.value = '';
  try {
    data.value = (await statisticsApi({
      from: dates.value[0],
      to: dates.value[1],
      buildingId: buildingId.value || undefined
    })).data;
  } catch (e) {
    error.value = e.response?.data?.msg || e.message;
  } finally {
    busy.value = false;
  }
};
onMounted(async () => {
  search();
  try {
    buildings.value = (await queryAllApi('buildings')).data;
  } catch {
    // 请求封装已提示错误；此处不执行成功操作，保留页面供用户重试。
  }
});
</script>
<!-- 模板中：v-model 双向绑定输入值；:属性 传入变量；@事件 调用方法；v-if 按条件决定是否渲染。 -->
<template>
  <div class="heading">
    <div><span class="eyebrow">OVERVIEW</span>
      <h1>校园服务概览</h1><span class="muted">了解报修进展与服务效率</span></div>
  </div>
  <div class="panel filters">
    <el-date-picker v-model="dates" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始时间"
                    end-placeholder="结束时间"/>
    <el-select v-model="buildingId" clearable placeholder="全部楼宇">
      <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id"/>
    </el-select>
    <el-button type="primary" @click="search">查询统计</el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false"/>
  <div v-loading="busy">
    <template v-if="data">
      <div class="stats">
        <div class="stat"
             v-for="(label,key) in {totalCount:'工单总量',pendingAssignmentCount:'待派单',processingCount:'处理中',pendingConfirmationCount:'待确认',completedCount:'已完成',overdueCount:'已超时',averageCompletionMinutes:'平均完成时长 / 分钟'}"
             :key="key"><span class="muted">{{ label }}</span><strong>{{ data[key] ?? '—' }}</strong></div>
      </div>
      <div class="panel" style="margin-top:24px"><h3>故障类别分布</h3>
        <el-table :data="data.categoryCounts||[]">
          <el-table-column prop="categoryName" label="故障类别"/>
          <el-table-column prop="count" label="工单数量"/>
        </el-table>
      </div>
    </template>
    <el-empty v-else-if="!busy" description="暂无统计数据"/>
  </div>
</template>