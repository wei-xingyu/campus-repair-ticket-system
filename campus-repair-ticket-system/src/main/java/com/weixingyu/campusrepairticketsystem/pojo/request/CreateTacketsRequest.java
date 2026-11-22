package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/** 创建报修工单时客户端提交的请求参数。 */
public class CreateTacketsRequest {
    /** 工单标题，例如“宿舍灯不亮”。 */
    @NotBlank(message="标题不能为空") @Size(min=2,max=100,message="标题需为2–100字")
    private String title;
    /** 报修问题的详细描述。 */
    @NotBlank(message="故障描述不能为空") @Size(min=5,max=2000,message="故障描述需为5–2000字")
    private String description;
    /** 报修类别 ID。 */
    @NotNull(message="类别ID不能为空") @Positive(message="类别ID必须为正数")
    private Long categoryId;
    /** 报修地点所属楼栋 ID。 */
    @NotNull(message="楼宇ID不能为空") @Positive(message="楼宇ID必须为正数")
    private Long buildingId;
    /** 具体报修位置，例如房间号。 */
    @NotBlank(message="报修位置不能为空") @Size(max=100,message="报修位置最多100字")
    private String room;
    /** 关联设备 ID，没有关联设备时可以为空。 */
    @Positive(message="设备ID必须为正数")
    private Long assetId;
    /** 工单优先级，例如 LOW、NORMAL、HIGH、URGENT。 */
    private String priority;
    /** 报修时上传的附件地址列表。 */
    @Size(max=5,message="附件最多5个")
    private String [] attachmentUrls;
}
