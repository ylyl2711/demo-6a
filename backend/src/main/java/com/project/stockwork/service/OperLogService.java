package com.project.stockwork.service;

import com.project.stockwork.common.UserContext;
import com.project.stockwork.entity.SysOperLog;
import com.project.stockwork.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class OperLogService {

    private final SysOperLogMapper operLogMapper;

    public void log(String module, String content) {
        logAs(UserContext.name(), module, content);
    }

    public void logAs(String userName, String module, String content) {
        SysOperLog log = new SysOperLog();
        log.setOperUser(userName);
        log.setOperTime(new Date());
        log.setOperModule(module);
        log.setOperContent(content);
        log.setIpAddress(UserContext.get() == null ? "" : UserContext.get().getIp());
        operLogMapper.insert(log);
    }
}
