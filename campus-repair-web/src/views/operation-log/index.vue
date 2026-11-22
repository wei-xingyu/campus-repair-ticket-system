<script setup>
import {onMounted, reactive, ref} from 'vue';
import {operationLogsApi} from '@/api/operationLog';

const filters = reactive({userId: '', status: null, dates: null});
const page = ref(1);
const pageSize = ref(10);
const rows = ref([]);
const total = ref(0);
const busy = ref(false);
const error = ref('');
const filterError = ref('');
let appliedFilters = {};
let requestId = 0;

async function loadLogs() {
  const id = ++requestId;
  busy.value = true;
  error.value = '';
  try {
    const {data} = await operationLogsApi({...appliedFilters, page: page.value, pageSize: pageSize.value});
    if (id === requestId) {
      rows.value = data.rows;
      total.value = data.total;
    }
  } catch (exception) {
    if (id === requestId) {
      rows.value = [];
      error.value = exception.response?.data?.msg || exception.message;
    }
  } finally {
    if (id === requestId) busy.value = false;
  }
}

function search() {
  const userId = filters.userId.trim();
  filterError.value = '';
  if (userId && !/^[1-9]\d*$/.test(userId)) {
    filterError.value = '用户 ID 必须为正整数';
    return;
  }
  const [startTime, endTime] = filters.dates ?? [];
  if (startTime && endTime && startTime >= endTime) {
    filterError.value = '开始时间必须早于结束时间';
    return;
  }
  appliedFilters = {userId: userId || undefined, status: filters.status ?? undefined, startTime, endTime};
  page.value = 1;
  loadLogs();
}

function reset() {
  Object.assign(filters, {userId: '', status: null, dates: null});
  search();
}

function changePage(nextPage) {
  if (nextPage === page.value) return;
  page.value = nextPage;
  loadLogs();
}

function changePageSize(nextSize) {
  pageSize.value = nextSize;
  page.value = 1;
  loadLogs();
}

onMounted(loadLogs);
</script>

<template>
  <div class="heading">
    <h1>操作日志</h1>
  </div>
  <section class="panel">
    <form class="filters" @submit.prevent="search">
      <el-input v-model="filters.userId" placeholder="操作人用户 ID" aria-label="操作人用户 ID"
                inputmode="numeric" clearable/>
      <el-select v-model="filters.status" placeholder="执行结果" aria-label="执行结果" clearable>
        <el-option label="成功" :value="1"/>
        <el-option label="失败" :value="0"/>
      </el-select>
      <el-date-picker v-model="filters.dates" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss"
                      start-placeholder="开始时间" end-placeholder="结束时间" range-separator="至"/>
      <el-button type="primary" native-type="submit">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </form>

    <el-alert v-if="filterError || error" class="log-alert" :title="filterError || error"
              type="error" :closable="false" show-icon/>
    <el-table v-loading="busy" :data="rows" :empty-text="error ? '查询失败' : '暂无操作日志'">
      <el-table-column prop="createdAt" label="记录时间" width="175">
        <template #default="{row}">{{ row.createdAt?.replace('T', ' ') || '—' }}</template>
      </el-table-column>
      <el-table-column prop="operation" label="操作" min-width="120"/>
      <el-table-column label="操作人" min-width="120" show-overflow-tooltip>
        <template #default="{row}">
          {{ row.username ?? '—' }}
          <span v-if="row.userId != null" class="muted">#{{ row.userId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="执行结果" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="90" align="right">
        <template #default="{row}">{{ row.durationMs }} ms</template>
      </el-table-column>
      <el-table-column label="操作对象" min-width="125" show-overflow-tooltip>
        <template #default="{row}">
          {{ row.targetType ?? '—' }}<span v-if="row.targetId != null"> #{{ row.targetId }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="methodName" label="方法名" min-width="200" show-overflow-tooltip/>
      <el-table-column label="错误码" min-width="135" show-overflow-tooltip>
        <template #default="{row}">{{ row.errorCode ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="id" label="日志 ID" width="90"/>
    </el-table>
    <el-pagination :current-page="page" :page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
                   :total="total" :disabled="busy" layout="total, sizes, prev, pager, next"
                   @update:current-page="changePage" @update:page-size="changePageSize"/>
  </section>
</template>

<style scoped>
.log-alert {
  margin-bottom: 16px;
}

.filters :deep(.el-date-editor) {
  max-width: 100%;
}
</style>
