package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;

public interface WmChannelService extends IService<WmChannel> {
    /**
     * 查询所有频道
     * @return
     */
    public ResponseResult findAll();

    /**
     * 根据id删除频道
     * @return
     */
    public ResponseResult delById(Integer id);

    /**
     * 分页查询频道
     * @param wmChannelDto
     * @return
     */
    ResponseResult listChannel(WmChannelDto wmChannelDto);

    /**
     * 保存频道
     * @param wmChannel
     * @return
     */
    ResponseResult saveChannel(WmChannel wmChannel);

    /**
     * 更新频道
     * @param wmChannel
     * @return
     */
    ResponseResult updateChannel(WmChannel wmChannel);
}