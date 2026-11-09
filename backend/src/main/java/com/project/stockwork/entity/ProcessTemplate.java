package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("process_template")
public class ProcessTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;

    private Integer processSeq;

    private String processName;

    private String requirement;
}
