package com.project.stockwork.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BomSaveDto {

    private Long productMaterialId;

    private List<Item> items;

    @Data
    public static class Item {
        private Long componentMaterialId;
        private BigDecimal quantity;
        private String remark;
    }
}
