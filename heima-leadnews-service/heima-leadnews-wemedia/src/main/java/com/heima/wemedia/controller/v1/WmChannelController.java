package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.service.WmChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channel")
public class WmChannelController {

    @Autowired
    private WmChannelService wmChannelService;

    @GetMapping("/channels")
    public ResponseResult findAll() {
        return wmChannelService.findAll();
    }

    @GetMapping("/del/{id}")
    public ResponseResult delChannel(@PathVariable("id") Integer id) {
        return wmChannelService.delById(id);
    }
    @PostMapping("/list")
    public ResponseResult listChannel(@RequestBody WmChannelDto wmChannelDto){
        return wmChannelService.listChannel(wmChannelDto);
    }
    @PostMapping("/save")
    public ResponseResult saveChannel(@RequestBody WmChannel wmChannel){
        return wmChannelService.saveChannel(wmChannel);
    }
    @PostMapping("/update")
    public ResponseResult updateChannel(@RequestBody WmChannel wmChannel){
        return wmChannelService.updateChannel(wmChannel);
    }
}
