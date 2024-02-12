package com.heima.model.admin.dtos;

import lombok.Data;

@Data
public class ApUserAuditDto {
    private Integer id;
    //失败原因
    private String msg;
}
