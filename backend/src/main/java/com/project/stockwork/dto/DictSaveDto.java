package com.project.stockwork.dto;

import lombok.Data;

@Data
public class DictSaveDto {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
}
