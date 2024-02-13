package com.heima.model.behavior.dtos;

import lombok.Data;

@Data
public class BehaviorLikeDto {
    private Long articleId;
    private Short operation;
    private Short type;
}
