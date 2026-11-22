// 后端枚举值 → 界面中文的映射。例如请求仍发送 PROCESSING，页面显示“处理中”。
export const statuses={PENDING_ASSIGNMENT:'待派单',PENDING_ACCEPTANCE:'待接单',ACCEPTED:'已接单',PROCESSING:'处理中',PENDING_CONFIRMATION:'待确认',COMPLETED:'已完成',CANCELLED:'已取消',CLOSED:'已关闭'};
export const priorities={LOW:'低',NORMAL:'普通',HIGH:'高',URGENT:'紧急'};