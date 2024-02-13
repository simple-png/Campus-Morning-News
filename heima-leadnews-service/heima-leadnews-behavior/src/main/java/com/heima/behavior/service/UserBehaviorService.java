package com.heima.behavior.service;

import com.heima.model.behavior.dtos.BehaviorLikeDto;
import com.heima.model.behavior.dtos.BehaviorReadDto;
import com.heima.model.behavior.dtos.BehaviorUnLikeDto;
import com.heima.model.common.dtos.ResponseResult;

public interface UserBehaviorService {
    /**
     * 用户点赞
     * @param dto
     * @return
     */
    public ResponseResult behaviorLikes(BehaviorLikeDto dto);

    /**
     * 用户不喜欢
     * @param dto
     * @return
     */
    ResponseResult behaviorUnLikes(BehaviorUnLikeDto dto);

    /**
     * 用户阅读行为
     * @param dto
     * @return
     */
    ResponseResult behaviorRead(BehaviorReadDto dto);

}
