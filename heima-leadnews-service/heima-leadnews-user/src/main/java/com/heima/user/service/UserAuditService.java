package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.dtos.ApUserAuditDto;
import com.heima.model.admin.dtos.UserPageDto;
import com.heima.model.admin.pojos.ApUserRealname;
import com.heima.model.common.dtos.ResponseResult;

public interface UserAuditService extends IService<ApUserRealname> {

    /**
     * admin端分页查询用户
     * @param dto
     * @return
     */
    public ResponseResult listUsers(UserPageDto dto);

    /**
     * 审核失败
     * @param dto
     * @return
     */
    public ResponseResult auditFailed(ApUserAuditDto dto);

    /**
     * 审核成功
     * @param dto
     * @return
     */
    public ResponseResult auditPassed(ApUserAuditDto dto);
}
