package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("material_bom")
public class MaterialBom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productMaterialId;

    private Long componentMaterialId;

    private BigDecimal quantity;

    private String remark;
}
