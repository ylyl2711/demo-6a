package com.project.stockwork.dto;

import lombok.Data;

@Data
public class UserSaveDto {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private Integer status;
}
