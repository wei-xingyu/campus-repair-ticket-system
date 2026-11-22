<script setup>
/* 阅读顺序：筛选数据 → search 查询 → create/save 新建 → show 详情 → actions/openAction/submitAction 状态流转。 */
import {ref, onMounted, computed} from 'vue';
import {ElMessage} from 'element-plus';
import {user} from '@/utils/session';
import {statuses, priorities} from '@/utils/enums';
import {queryPageApi, queryByIdApi, addApi, actionApi, historyApi} from '@/api/ticket';
import {queryAllApi, assetsApi} from '@/api/basic';
import ImageUpload from '@/components/ImageUpload.vue';

const historyActionLabels = new Map(Object.entries({
  CREATE: '提交报修',
  ASSIGN: '派单',
  REASSIGN: '重新派单',
  ACCEPT: '接单',
  REJECT: '退回工单',
  START: '开始维修',
  SUBMIT_RESOLUTION: '提交处理结果',
  CONFIRM: '确认完成',
  REOPEN: '重新打开工单',
  CANCEL: '取消工单',
  CLOSE: '关闭工单',
}));

const historyDescription = ({action, remark}) => {
  const text = remark?.trim();
  if (!text || text === action || /^联调测试\s*[:：]\s*[A-Z_]+$/.test(text)) {
    return historyActionLabels.get(action) || '工单流转';
  }
  return remark;
};

// ref 保存会影响页面显示的数据：rows 是当前页记录，total 是总条数；busy 控制查询遮罩，error 显示失败原因。
// 在 JS 中读写 ref 要加 .value；在模板里 Vue 会自动解包，所以直接写 rows、busy。
const rows = ref([]), total = ref(0), busy = ref(false), error = ref(''), categories = ref([]), buildings = ref([]),
    assets = ref([]), repairers = ref([]);
// filters 对应后端 Query 参数；dates 是日期组件的两个时间值，发请求时再拆成起止时间。
const filters = ref({page: 1, pageSize: 10}), dates = ref([]);
// 查询按钮传 true，先回第一页；翻页调用 search() 保留页码，避免每次翻页都回到第一页。
const search = async (reset = false) => {
  if (reset) filters.value.page = 1;
  busy.value = true;
  error.value = '';
  try {
// await 等接口响应后再赋值。...filters.value 把筛选字段展开到一个新对象，不修改原筛选对象。
    // ?. 是可选链：日期被清空时，取不到值就得到 undefined，避免直接读取空值报错。
    const result = await queryPageApi({...filters.value, createdFrom: dates.value?.[0], createdTo: dates.value?.[1]});
    rows.value = result.data.rows;
    total.value = result.data.total;
  } catch (e) {
    rows.value = [];
    total.value = 0;
    error.value = e.response?.data?.msg || e.message;
  } finally {
    busy.value = false;
  }
};
// Visible 变量控制弹窗/抽屉；form 保存新建表单，detail 保存从后端查询到的完整工单。
const createVisible = ref(false), form = ref({}), saving = ref(false), detail = ref(null), detailVisible = ref(false);
// 每次打开新建窗口都重置表单，防止上次输入残留。此时还没有提交后端。
const create = () => {
  form.value = {
    title: '',
    description: '',
    categoryId: null,
    buildingId: null,
    room: '',
    assetId: null,
    priority: 'NORMAL',
    attachmentUrls: []
  };
  assets.value = [];
  createVisible.value = true;
};
// 楼宇和设备联动：换楼宇先清空旧设备，避免把 A 楼的设备提交到 B 楼。
const assetState = ref('');
const changeBuilding = async () => {
  const currentForm = form.value;
  const buildingId = currentForm.buildingId;
  assetState.value = buildingId ? '正在加载设备' : '';
  form.value.assetId = null;
  assets.value = [];
  if (form.value.buildingId) try {
    const result = await assetsApi(buildingId);
    if (form.value !== currentForm || currentForm.buildingId !== buildingId) return;
    assets.value = result.data;
    assetState.value = assets.value.length ? '' : '该楼宇暂无可选设备，可跳过';
  } catch {
    if (form.value === currentForm && currentForm.buildingId === buildingId) assetState.value = '设备加载失败，可重新选择楼宇或跳过';
  }
};
// 提交前做基础校验；真正的数据有效性仍由后端判断。成功后关闭弹窗并刷新第一页。
const save = async () => {
  const f = form.value;
  if (f.title.trim().length < 2 || f.description.trim().length < 5 || !f.categoryId || !f.buildingId || !f.room.trim()) {
    ElMessage.warning('请完整填写必填项，标题至少2字，描述至少5字');
    return;
  }
  saving.value = true;
  try {
    await addApi(f);
    ElMessage.success('报修已提交');
    createVisible.value = false;
    search(true);
  } catch {
    // 请求拦截器已显示错误，保留填写内容。
  } finally {
    saving.value = false;
  }
};
// 列表没有完整描述和版本号，打开详情时重新请求后端，而不是直接把列表行当作详情。
const show = async id => {
  try {
    detail.value = (await queryByIdApi(id)).data;
    detailVisible.value = true;
  } catch {
    // 请求拦截器已显示错误。
  }
};
// computed 会根据角色和工单状态自动计算允许显示的按钮。每项是 [接口动作名, 按钮文字]。
// 例如维修人员看到待接单工单时，显示接单和拒绝；前端隐藏按钮只改善体验，后端仍必须校验权限。
const statusLabel = status => {
  if (status === 'PENDING_CONFIRMATION') {
    return user.value?.role === 'STUDENT' ? '待你验收' : '待报修人验收';
  }
  return statuses[status];
};
const quickFilter = status => {
  filters.value.status = status;
  search(true);
};
const actions = computed(() => {
  const s = detail.value?.status, r = user.value?.role, result = [];
  if (r === 'ADMIN') {
    if (s === 'PENDING_ASSIGNMENT') result.push(['assignment', '派单']);
    if (['PENDING_ACCEPTANCE', 'ACCEPTED'].includes(s)) result.push(['reassignment', '转派']);
    if (!['COMPLETED', 'CANCELLED', 'CLOSED'].includes(s)) result.push(['close', '关闭工单']);
  }
  if (r === 'REPAIRER') {
    if (s === 'PENDING_ACCEPTANCE') result.push(['accept', '接单'], ['reject', '拒绝']);
    if (s === 'ACCEPTED') result.push(['start', '开始处理']);
    if (s === 'PROCESSING') result.push(['resolution', '提交处理结果']);
  }
  if (r === 'STUDENT') {
    if (['PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'ACCEPTED'].includes(s)) result.push(['cancel', '取消工单']);
    if (s === 'PENDING_CONFIRMATION') result.push(['confirm', '确认完成'], ['reopen', '要求重新处理']);
    if (s === 'COMPLETED' && !detail.value.rating) result.push(['rating', '评价']);
  }
  return result;
});
const action = ref(''), actionLabel = ref(''), actionVisible = ref(false), actionForm = ref({});
// 参数 [key, label] 是数组解构，等价于分别读取传入数组的第 0、1 项。多个操作共用一个弹窗。
const openAction = async ([key, label]) => {
  action.value = key;
  actionLabel.value = label;
// expectedVersion 像你手中的工单副本版本：打开时是 2，别人已改到 3，后端就拒绝用旧版本覆盖。
  // 不要自己加版本号；提交详情返回的 version，冲突后重新打开详情获取最新值。
  actionForm.value = {expectedVersion: detail.value.version, score: 5, attachmentUrls: []};
  if (['assignment', 'reassignment'].includes(key)) {
    try {
      repairers.value = (await queryAllApi('repairers')).data;
    } catch {
      return;
    }
  }
  actionVisible.value = true;
};
// 根据动作校验不同字段，再调用同一个 actionApi。评价只提交 score/comment，其余动作携带版本号。
const submitAction = async () => {
  const f = actionForm.value, a = action.value;
  if (['assignment', 'reassignment'].includes(a) && !f.assigneeId) {
    ElMessage.warning('请选择维修人员');
    return;
  }
  if (['reject', 'reopen', 'reassignment', 'close'].includes(a) && (!f.reason || f.reason.trim().length < 2)) {
    ElMessage.warning('原因至少2字');
    return;
  }
  if (a === 'resolution' && (!f.resolution || f.resolution.trim().length < 5)) {
    ElMessage.warning('处理说明至少5字');
    return;
  }
  saving.value = true;
  try {
    await actionApi(detail.value.id, a, a === 'rating' ? {score: f.score, comment: f.comment} : f);
    ElMessage.success('操作成功');
    actionVisible.value = false;
    detailVisible.value = false;
    search();
  } catch {
    // 请求拦截器已显示错误，保留操作弹窗。
  } finally {
    saving.value = false;
  }
};
// 独立刷新历史，不会执行状态修改；历史记录由后端返回。
const refreshHistory = async () => {
  try {
    detail.value.history = (await historyApi(detail.value.id)).data;
  } catch {
    // 请求拦截器已显示错误，保留已加载的历史记录。
  }
};
// 页面首次挂载后加载列表和下拉选项。Promise.allSettled 同时发出独立查询，某一个失败也能使用另一个成功结果。
onMounted(async () => {
  search();
  const results = await Promise.allSettled([queryAllApi('categories'), queryAllApi('buildings')]);
  if (results[0].status === 'fulfilled') categories.value = results[0].value.data;
  if (results[1].status === 'fulfilled') buildings.value = results[1].value.data;
});
</script>
<!-- 模板中：v-model 双向绑定输入值；:属性 传入变量；@事件 调用方法；v-if 按条件决定是否渲染。 -->
<template>
  <div class="heading">
    <div><span class="eyebrow">WORKSPACE</span>
      <h1>工单工作台</h1><span class="muted">跟踪每一次报修，让问题及时解决</span></div>
    <el-button v-if="user?.role === 'STUDENT'" type="primary" @click="create">＋ 新建报修</el-button>
  </div>
  <div class="panel">
    <div class="filters">
      <div v-if="user && user.role !== 'STUDENT'" class="ticket-queues">
        <el-button v-if="user?.role === 'ADMIN'" @click="quickFilter('PENDING_ASSIGNMENT')">待派单</el-button>
        <template v-else>
          <el-button @click="quickFilter('PENDING_ACCEPTANCE')">待我接单</el-button>
          <el-button @click="quickFilter('PROCESSING')">处理中</el-button>
          <el-button @click="quickFilter('PENDING_CONFIRMATION')">待报修人验收</el-button>
        </template>
      </div>
      <el-input v-model="filters.keyword" placeholder="编号 / 标题 / 位置" clearable maxlength="100"
                @keyup.enter="search(true)"/>
      <el-select v-model="filters.status" placeholder="全部状态" clearable>
        <el-option v-for="(v,k) in statuses" :key="k" :label="v" :value="k"/>
      </el-select>
      <el-select v-model="filters.categoryId" placeholder="故障类别" clearable>
        <el-option v-for="x in categories" :key="x.id" :label="x.name" :value="x.id"/>
      </el-select>
      <el-select v-model="filters.buildingId" placeholder="楼宇" clearable>
        <el-option v-for="x in buildings" :key="x.id" :label="x.name" :value="x.id"/>
      </el-select>
      <el-select v-model="filters.priority" placeholder="优先级" clearable>
        <el-option v-for="(v,k) in priorities" :key="k" :label="v" :value="k"/>
      </el-select>
      <el-date-picker v-model="dates" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss"
                      start-placeholder="创建开始时间" end-placeholder="结束时间"/>
      <template v-if="user?.role === 'ADMIN'">
        <el-input-number v-model="filters.reporterId" placeholder="报修人ID" :min="1" :controls="false"/>
        <el-input-number v-model="filters.assigneeId" placeholder="维修人员ID" :min="1" :controls="false"/>
      </template>
      <el-button type="primary" @click="search(true)">查询</el-button>
      <el-button @click="filters={page:1,pageSize:10};dates=[];search()">重置</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon/>
    <!-- :data 绑定当前页数组；#default="{row}" 取出当前行，方便显示中文状态和绑定该行操作。 -->
    <el-table :data="rows" v-loading="busy" empty-text="暂无工单">
      <el-table-column prop="ticketNo" label="工单编号" width="180"/>
      <el-table-column prop="title" label="报修内容" min-width="200"/>
      <el-table-column label="状态" width="110">
        <template #default="{row}">
          <el-tag>{{ statuses[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="90">
        <template #default="{row}">{{ priorities[row.priority] }}</template>
      </el-table-column>
      <el-table-column prop="buildingName" label="楼宇"/>
      <el-table-column prop="room" label="位置"/>
      <el-table-column prop="reporterName" label="报修人"/>
      <el-table-column prop="assigneeName" label="维修人员"/>
      <el-table-column label="操作" fixed="right" width="120">
        <template #default="{row}">
          <el-button link type="primary" @click="show(row.id)">{{
            user?.role === 'STUDENT' && row.status === 'PENDING_CONFIRMATION' ? '查看并验收' :
            user?.role === 'REPAIRER' && row.status === 'PENDING_ACCEPTANCE' ? '查看并接单' :
            user?.role === 'REPAIRER' && ['ACCEPTED', 'PROCESSING'].includes(row.status) ? '继续处理' :
            user?.role === 'ADMIN' && row.status === 'PENDING_ASSIGNMENT' ? '查看并派单' : '查看详情'
          }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 页码变化保留筛选条件重新查；每页条数变化回到第一页，total 使用后端总数。 -->
    <el-pagination v-model:current-page="filters.page" v-model:page-size="filters.pageSize" :total="total"
                   :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="search()"
                   @size-change="search(true)"/>
  </div>
  <el-dialog v-model="createVisible" title="新建报修" width="min(680px,95vw)">
    <el-form label-width="90px">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" placeholder="例如：卫生间水龙头关不紧" maxlength="100"/>
      </el-form-item>
      <el-form-item label="故障类别" required>
        <el-select v-model="form.categoryId">
          <el-option v-for="x in categories" :key="x.id" :label="x.name" :value="x.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="楼宇" required>
        <el-select v-model="form.buildingId" @change="changeBuilding">
          <el-option v-for="x in buildings" :key="x.id" :label="x.name" :value="x.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="具体位置" required>
        <el-input v-model="form.room" placeholder="例如：一层 101，进门右侧卫生间" maxlength="100"/>
      </el-form-item>
      <el-form-item label="设备（选填）">
        <el-select v-model="form.assetId" clearable :disabled="!form.buildingId || !assets.length" :placeholder="!form.buildingId ? '请先选择楼宇' : assetState || '请选择设备'">
          <el-option v-for="x in assets" :key="x.id" :label="x.name" :value="x.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-radio-group v-model="form.priority">
          <el-radio label="LOW">低</el-radio>
          <el-radio label="NORMAL">普通</el-radio>
          <el-radio label="HIGH">高</el-radio>
        </el-radio-group><small>一般故障选普通，持续大量漏水或影响安全时选高。</small>
      </el-form-item>
      <el-form-item label="问题描述" required>
        <el-input v-model="form.description" placeholder="例如：关紧后仍持续滴水，今天早上发现" type="textarea" :rows="4" maxlength="2000" show-word-limit/>
      </el-form-item>
      <el-form-item label="现场图片">
        <ImageUpload v-model="form.attachmentUrls"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible=false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">提交报修</el-button>
    </template>
  </el-dialog>
  <el-drawer v-model="detailVisible" title="工单详情" size="min(760px,96vw)">
    <template v-if="detail"><h2>{{ detail.title }}</h2>
      <p class="muted">{{ detail.ticketNo }} · {{ statusLabel(detail.status) }}</p>
      <el-descriptions :column="2" border>
        <el-descriptions-item
            v-for="(label,key) in {categoryName:'故障类别',buildingName:'楼宇',room:'位置',assetName:'设备',reporterName:'报修人',assigneeName:'维修人员',createdAt:'创建时间',slaDueAt:'要求处理时限'}"
            :key="key" :label="label">{{ detail[key] || '—' }}
        </el-descriptions-item>
      </el-descriptions>
      <h3>问题描述</h3>
      <p style="white-space:pre-wrap">{{ detail.description }}</p>
      <h3>处理结果</h3>
      <p v-if="detail.status === 'PENDING_CONFIRMATION'">
        {{ user?.role === 'STUDENT' ? '请检查维修结果，再确认完成或要求重新处理。' : '维修结果已提交，等待报修人验收。' }}
      </p>
      <p>{{ detail.resolution || '暂未提交' }}</p>
      <div class="images">
        <el-image v-for="x in detail.attachments||[]" :key="x.url" :src="x.url"
                  :preview-src-list="(detail.attachments||[]).map(x=>x.url)" fit="cover"/>
      </div>
      <h3>流转记录
        <el-button link @click="refreshHistory">刷新历史</el-button>
      </h3>
      <el-timeline>
        <el-timeline-item v-for="x in detail.history||[]" :key="x.id" :timestamp="x.createdAt">{{ x.operatorName }} ·
          {{ statuses[x.toStatus] || historyActionLabels.get(x.action) || '工单流转' }}<p>{{ historyDescription(x) }}</p></el-timeline-item>
      </el-timeline>
    </template>
    <template v-if="actions.length" #footer>
      <el-button v-for="a in actions" :key="a[0]" type="primary" @click="openAction(a)">{{ a[1] }}</el-button>
    </template>
  </el-drawer>
  <el-dialog v-model="actionVisible" :title="actionLabel" width="min(550px,95vw)">
    <el-form label-width="90px">
      <el-form-item v-if="['assignment','reassignment'].includes(action)" label="维修人员" required>
        <el-select v-model="actionForm.assigneeId">
          <el-option v-for="x in repairers" :key="x.id" :label="x.name+' · 在办 '+x.activeTicketCount" :value="x.id"/>
        </el-select>
      </el-form-item>
      <el-form-item v-if="action==='assignment'" label="派单说明">
        <el-input v-model="actionForm.remark" maxlength="500"/>
      </el-form-item>
      <el-form-item v-if="['reject','reassignment','reopen','close','cancel'].includes(action)" label="原因"
                    :required="action!=='cancel'">
        <el-input v-model="actionForm.reason" type="textarea" maxlength="500"/>
      </el-form-item>
      <template v-if="action==='resolution'">
        <el-form-item label="处理说明" required>
          <el-input v-model="actionForm.resolution" type="textarea" maxlength="2000" :rows="4"/>
        </el-form-item>
        <ImageUpload v-model="actionForm.attachmentUrls"/>
      </template>
      <template v-if="action==='rating'">
        <el-form-item label="评分">
          <el-rate v-model="actionForm.score"/>
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="actionForm.comment" type="textarea" maxlength="500"/>
        </el-form-item>
      </template>
      <p v-if="['accept','start','confirm'].includes(action)">确认执行“{{ actionLabel }}”？</p></el-form>
    <template #footer>
      <el-button @click="actionVisible=false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitAction">确认</el-button>
    </template>
  </el-dialog>
</template>
