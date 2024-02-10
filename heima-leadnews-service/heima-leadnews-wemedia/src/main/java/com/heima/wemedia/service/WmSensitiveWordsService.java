package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.admin.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;

public interface WmSensitiveWordsService extends IService<WmSensitive> {
    /**
     * 敏感词分页查询
     * @param dto
     * @return
     */
    public ResponseResult listSensitiveWords(SensitiveDto dto);

    /**
     * 根据id删除敏感词
     * @param id
     * @return
     */
    public ResponseResult delSensitive(Integer id);

    /**
     * 保存敏感词
     * @param wmSensitive
     * @return
     */
    public ResponseResult saveSensitiveWords(WmSensitive wmSensitive);

    /**
     * 修改敏感词
     * @param wmSensitive
     * @return
     */
    ResponseResult updateSensitiveWords(WmSensitive wmSensitive);
}
