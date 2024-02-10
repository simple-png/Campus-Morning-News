package com.heima.wemedia.controller.v1;

import com.heima.model.admin.dtos.SensitiveDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.service.WmSensitiveWordsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/v1/sensitive")
public class WmSensitiveWordsController {
    @Autowired
    private WmSensitiveWordsService wmSensitiveWordsService;

    @DeleteMapping("/del/{id}")
    public ResponseResult delSensitiveWords(@PathVariable("id") Integer id) {
        return wmSensitiveWordsService.delSensitive(id);
    }

    @PostMapping("/list")
    public ResponseResult listSensitiveWords(@RequestBody SensitiveDto dto) {
        return wmSensitiveWordsService.listSensitiveWords(dto);
    }
    @PostMapping("/save")
    public ResponseResult saveSensitiveWords(@RequestBody WmSensitive wmSensitive){
        return wmSensitiveWordsService.saveSensitiveWords(wmSensitive);
    }
    @PostMapping("/update")
    public ResponseResult updateSensitiveWords(@RequestBody WmSensitive wmSensitive){
        return wmSensitiveWordsService.updateSensitiveWords(wmSensitive);
    }
}
