package com.heima.user.controller.v1;


import com.heima.model.admin.dtos.ApUserAuditDto;
import com.heima.model.admin.dtos.UserPageDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.user.service.UserAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class UserAuditController {
    @Autowired
    private UserAuditService userAuditService;
    @PostMapping("/list")
    public ResponseResult listUsers(@RequestBody UserPageDto dto){
        return userAuditService.listUsers(dto);
    }
    @PostMapping("/authFail")
    public ResponseResult auditFailed(@RequestBody ApUserAuditDto dto){
        return userAuditService.auditFailed(dto);
    }
    @PostMapping("/authPass")
    public ResponseResult auditPassed(@RequestBody ApUserAuditDto dto){
        return userAuditService.auditPassed(dto);
    }
}
