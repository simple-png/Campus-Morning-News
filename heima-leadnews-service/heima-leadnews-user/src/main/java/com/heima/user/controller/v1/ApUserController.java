package com.heima.user.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.FollowDto;
import com.heima.user.service.ApUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@Slf4j
public class ApUserController {

    @Autowired
    private ApUserService apUserService;

    @PostMapping("/user_follow")
    public ResponseResult userFollow(@RequestBody FollowDto dto) {
        return apUserService.userFollow(dto);
    }
}
