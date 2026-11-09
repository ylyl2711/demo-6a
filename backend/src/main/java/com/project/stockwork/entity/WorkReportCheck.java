package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("work_report_check")
public class WorkReportCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;

    private Long processId;

    private String processName;

    private BigDecimal goodNum;

    private BigDecimal badNum;

    private Long badReasonDictId;

    private String photoUrl;

    private Date reportTime;

    private Integer checkResult;

    private String checkUser;

    private Date checkTime;

    private String checkRemark;

    private Long checkBadReasonId;
}
