package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.dtos.WmNewsUpDownDto;
import com.heima.model.wemedia.pojos.WmNews;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface WmNewsService extends IService<WmNews> {
    /**
     * 条件查询
     * @param dto
     * @return
     */
    public ResponseResult findList(WmNewsPageReqDto dto);


    /**
     * 发布修改文章或保存为草稿
     * @param dto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto);

    /**
     * 查看文章详情
     * @param id
     * @return
     */
    public ResponseResult getArticle(Integer id);

    /**
     * 删除文章
     * @param id
     * @return
     */
    public ResponseResult delArticle(Integer id);

    /**
     * 上下架文章
     * @param dto
     * @return
     */
    public ResponseResult downOrUpArticle(WmNewsDto dto);
}
