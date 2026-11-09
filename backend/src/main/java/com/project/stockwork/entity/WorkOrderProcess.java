package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("work_order_process")
public class WorkOrderProcess {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;

    private Integer processSeq;

    private String processName;

    private String requirement;

    private Integer processStatus;
}
