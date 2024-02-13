package com.heima.behavior.controller;

import com.heima.behavior.service.UserBehaviorService;
import com.heima.model.behavior.dtos.BehaviorLikeDto;
import com.heima.model.behavior.dtos.BehaviorReadDto;
import com.heima.model.behavior.dtos.BehaviorUnLikeDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserBehaviorController {
    @Autowired
    private UserBehaviorService userBehaviorService;
    @PostMapping("/likes_behavior")
    public ResponseResult behaviorLikes(@RequestBody BehaviorLikeDto dto){
       return userBehaviorService.behaviorLikes(dto);
    }
    @PostMapping("/un_likes_behavior")
    public ResponseResult behaviorUnLikes(@RequestBody BehaviorUnLikeDto dto){
        return userBehaviorService.behaviorUnLikes(dto);
    }
    @PostMapping("/read_behavior")
    public ResponseResult behaviorRead(@RequestBody BehaviorReadDto dto){
        return userBehaviorService.behaviorRead(dto);
    }
}
