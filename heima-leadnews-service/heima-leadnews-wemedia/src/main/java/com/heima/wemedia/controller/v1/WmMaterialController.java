package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/material")
@Slf4j
public class WmMaterialController {

    @Autowired
    private WmMaterialService wmMaterialService;

    @PostMapping("/upload_picture")
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        return wmMaterialService.uploadPicture(multipartFile);
    }

    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmMaterialDto dto){
        return wmMaterialService.findList(dto);
    }

    @GetMapping("/del_picture/{id}")
    public ResponseResult delPicture(@PathVariable("id") Integer id){
        return wmMaterialService.delPicture(id);
    }

    @GetMapping("/collect/{id}")
    public ResponseResult collectPicture(@PathVariable("id") Integer id){
        return wmMaterialService.collectPicture(id);
    }
    @GetMapping("/cancel_collect/{id}")
    public ResponseResult cancelCollectPicture(@PathVariable("id") Integer id){
        return wmMaterialService.cancelCollectPicture(id);
    }
}
