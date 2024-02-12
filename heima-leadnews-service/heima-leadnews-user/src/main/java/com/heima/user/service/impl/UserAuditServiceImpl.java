package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.ApUserConstants;
import com.heima.model.admin.dtos.ApUserAuditDto;
import com.heima.model.admin.dtos.UserPageDto;
import com.heima.model.admin.pojos.ApUserRealname;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.user.mapper.UserAuditMapper;
import com.heima.user.service.UserAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserAuditServiceImpl extends ServiceImpl<UserAuditMapper, ApUserRealname> implements UserAuditService {
    @Override
    public ResponseResult listUsers(UserPageDto dto) {
        dto.checkParam();
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<ApUserRealname> apUserLambdaQueryWrapper = Wrappers.<ApUserRealname>lambdaQuery();
        if (dto.getStatus() != null) {
            apUserLambdaQueryWrapper.eq(ApUserRealname::getStatus, dto.getStatus());
        }
        page(page, apUserLambdaQueryWrapper);
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult auditFailed(ApUserAuditDto dto) {
        return auditApUser(dto.getId(), dto.getMsg(), (short) ApUserConstants.AUDIT_FAILED);
    }

    @Override
    public ResponseResult auditPassed(ApUserAuditDto dto) {
        return auditApUser(dto.getId(), dto.getMsg(), (short) ApUserConstants.AUDIT_PASSED);
    }

    private ResponseResult auditApUser(Integer id, String msg, Short status) {
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApUserRealname apUserRealname = new ApUserRealname();
        if (status == ApUserConstants.AUDIT_FAILED) {
            apUserRealname.setReason(msg);
        }
        apUserRealname.setId(id);
        apUserRealname.setStatus(status);
        updateById(apUserRealname);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
