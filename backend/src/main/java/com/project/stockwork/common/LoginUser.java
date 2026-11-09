package com.project.stockwork.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
    private String ip;
}
