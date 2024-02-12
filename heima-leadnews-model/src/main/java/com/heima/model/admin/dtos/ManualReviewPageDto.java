package com.heima.model.admin.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

@Data
public class ManualReviewPageDto extends PageRequestDto {
    private Integer status;
    private String title;
}
