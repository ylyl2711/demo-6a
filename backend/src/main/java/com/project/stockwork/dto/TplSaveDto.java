package com.project.stockwork.dto;

import lombok.Data;

import java.util.List;

@Data
public class TplSaveDto {

    private Long materialId;

    private List<Item> items;

    @Data
    public static class Item {
        private Integer processSeq;
        private String processName;
        private String requirement;
    }
}
