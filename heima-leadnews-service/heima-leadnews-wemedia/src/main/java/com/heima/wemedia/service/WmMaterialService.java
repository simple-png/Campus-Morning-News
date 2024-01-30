package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

public interface WmMaterialService extends IService<WmMaterial> {

    /**
     * 图片上传
     * @param multipartFile
     * @return
     */
    public ResponseResult uploadPicture(MultipartFile multipartFile);

    /**
     * 素材列表查询
     * @param dto
     * @return
     */
    public ResponseResult findList(WmMaterialDto dto);

    /**
     * 图片删除
     * @param id
     * @return
     */
    public ResponseResult delPicture(Integer id);

    /**
     * 收藏图片
     * @param id
     * @return
     */
    public ResponseResult collectPicture(Integer id);

    /**
     * 取消收藏图片
     * @param id
     * @return
     */
    public ResponseResult cancelCollectPicture(Integer id);
}