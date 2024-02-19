package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.admin.dtos.ManualReviewPageDto;
import com.heima.model.admin.dtos.WmNewsManualReviewDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNews.Status;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        //1.检查参数
        dto.checkParam();
        //2.分页条件查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus());
        lambdaQueryWrapper.eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId());
        lambdaQueryWrapper.between(dto.getBeginPubDate() != null && dto.getEndPubDate() != null,
                WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        lambdaQueryWrapper.like(StringUtils.isNotBlank(dto.getKeyword()), WmNews::getTitle, dto.getKeyword());
        lambdaQueryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());
        lambdaQueryWrapper.orderByDesc(WmNews::getPublishTime);
        page = page(page, lambdaQueryWrapper);
        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    @Override
    @GlobalTransactional
    public ResponseResult submitNews(WmNewsDto dto) {
        //0.条件判断
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.保存或修改文章
        WmNews wmNews = new WmNews();
        //属性拷贝
        BeanUtils.copyProperties(dto, wmNews);
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);
        //2.判断是否为草稿 是的话结束当前方法
        if (dto.getStatus().equals(Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        //3.不是草稿，保存文章内容图片与素材的关系
        List<String> materials = extractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials, wmNews.getId());


        //4.不是草稿，保存文章封面图片与素材关系，如果当前布局是自动，需要匹封面图片
        saveRelativeInfoForCover(dto, wmNews, materials);

        //审核文章
        wmNewsTaskService.addNewsToTask(wmNews.getId(), wmNews.getPublishTime());
//        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult getArticle(Integer id) {
        //检查参数是否为空
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_REQUIRE);
        }
        //检查数据库中是否存在
        WmNews wmNews = getById(id);
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_NOT_EXIST);
        }
        return ResponseResult.okResult(wmNews);
    }

    @Override
    public ResponseResult delArticle(Integer id) {
        //检查参数是否为空
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_REQUIRE);
        }
        //检查数据库中是否存在
        WmNews wmNews = getById(id);
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_NOT_EXIST);
        }
        //检查是否已发布
        if (wmNews.getEnable() == 1 && wmNews.getStatus() == 9) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_PUBLISHED);
        }
        //删除文章
        removeById(id);
        //删除文章图片关联
        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, id));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public ResponseResult downOrUpArticle(WmNewsDto dto) {
        //检查参数是否为空
        if (dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_REQUIRE);
        }
        //检查数据库中是否存在
        WmNews wmNews = getById(dto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_NOT_EXIST);
        }
        //检查是否已发布
        if (!wmNews.getStatus().equals(Status.PUBLISHED.getCode())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_NOT_PUBLISHED);
        }
        //修改文章enable
        if (dto.getEnable() != null && dto.getEnable() > -1 && dto.getEnable() < 2) {
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable, dto.getEnable()).eq(WmNews::getId, dto.getId()));
            if (wmNews.getArticleId() != null) {
                //发送消息，通知article修改文章配置
                Map<String, Object> map = new HashMap<>();
                map.put("articleId", wmNews.getArticleId());
                map.put("enable", dto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString(map));
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult listManualReview(ManualReviewPageDto dto) {
        dto.checkParam();
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> wmNewsLambdaQueryWrapper = Wrappers.<WmNews>lambdaQuery()
                .eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus())
                .like(StringUtils.isNotBlank(dto.getTitle()), WmNews::getTitle, dto.getTitle());
        page(page, wmNewsLambdaQueryWrapper);
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult manualReviewFailed(WmNewsManualReviewDto dto) {
        if (dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_REQUIRE);
        }
        WmNews wmNews = new WmNews();
        wmNews.setId(dto.getId());
        wmNews.setReason(dto.getMsg());
        wmNews.setStatus(Status.FAIL.getCode());
        updateById(wmNews);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult manualReviewPassed(WmNewsManualReviewDto dto) {
        if (dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.ARTICLE_REQUIRE);
        }
        WmNews wmNews = new WmNews();
        wmNews.setId(dto.getId());
        wmNews.setReason("审核通过");
        wmNews.setStatus(Status.ADMIN_SUCCESS.getCode());
        updateById(wmNews);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 如果当前封面类型为自动，则设置封面类型的数据
     * 匹配规则:
     * 1.如果内容图片大于等于1，小于3 单图 type 1
     * 2.如果内容图片大于等于3 多图 type 3
     * 3.如果内容没有图片 无图 type 0
     * <p>
     * 保存封面图片与素材的关系
     *
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
        List<String> images = dto.getImages();
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            //多图
            if (materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() >= 1) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else {
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
                images = materials.stream().limit(0).collect(Collectors.toList());
            }
            if (images != null && !images.isEmpty()) {
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }
        if (images != null && !images.isEmpty()) {
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }


    }


    /**
     * 提取文章内容中的图片信息
     *
     * @param content
     * @return
     */
    private List<String> extractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if (map.get("type").equals("image")) {
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }

    /**
     * 处理文章内容图片与素材的关系
     *
     * @param materials
     * @param newsId
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials, newsId, WemediaConstants.WM_CONTENT_REFERENCE);
    }


    /**
     * 保存文章图片与素材的关系到数据库中
     *
     * @param materials
     * @param newsId
     * @param type
     */
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        if (materials != null && !materials.isEmpty()) {
            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));
            if (dbMaterials == null || dbMaterials.isEmpty() || materials.size() != dbMaterials.size()) {
                //查询出的结果是空或者和传入的数量不匹配
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }
            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
            //批量保存
            wmNewsMaterialMapper.saveRelations(idList, newsId, type);
        }

    }


    /**
     * 保存或更新文章
     *
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1);

        if (wmNews.getId() == null) {
            save(wmNews);
        } else {
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }
}
