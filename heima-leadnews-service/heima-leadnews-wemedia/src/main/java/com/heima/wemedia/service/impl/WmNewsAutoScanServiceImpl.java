package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.TextAutoRoute;
import com.heima.common.exception.CustomException;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNews.Status;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Autowired
    private WmNewsMapper wmNewsMapper;
    @Autowired
    private TextAutoRoute textAutoRoute;

    @Override
    @Async
    public void autoScanWmNews(Integer id) {
        //1.查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new CustomException(AppHttpCodeEnum.ARTICLE_NOT_EXIST);
        }
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);
            //2.审核文本内容
            boolean isTextScan = handleTextScan((String) textAndImages.get("content"), wmNews);
            if (!isTextScan) return;
            //3.审核图片 相关功能需要企业账号 略过
            boolean isImgScan = true;
            if (!isImgScan) return;
            //4.审核成功保存数据
            ResponseResult responseResult = saveAppArticle(wmNews);
            if (!responseResult.getCode().equals(200)) {
                throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章数据失败");
            }
            //回填article_id
            wmNews.setArticleId((Long) responseResult.getData());
            updateWmNews(wmNews, Status.PUBLISHED.getCode(), "审核成功");
        }
    }

    @Autowired
    private IArticleClient articleClient;
    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存文章
     *
     * @param wmNews
     * @return
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto articleDto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, articleDto);
        articleDto.setLayout(wmNews.getType());
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            articleDto.setChannelName(wmChannel.getName());
        }
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null) {
            articleDto.setAuthorId(Long.valueOf(wmUser.getId()));
            articleDto.setAuthorName(wmUser.getName());
        }
        if (wmNews.getArticleId() != null) {
            articleDto.setId(wmNews.getArticleId());
        }
        articleDto.setCreatedTime(new Date());
        return articleClient.saveArticle(articleDto);
    }

    /**
     * 审核文本
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = true;
        if ((wmNews.getTitle() + "-" + content).length() == 1) {
            return flag;
        }
        try {
            Map<String, Object> contentScanResult = textAutoRoute.performTextModeration(content);
            if (contentScanResult != null && JSON.parseObject((String) contentScanResult.get("result")).getString("code").equals("200")) {
                // reason和label都不为空，代表审核通过
                if (StringUtils.isNotBlank((String) contentScanResult.get("labels")) && StringUtils.isNotBlank((String) contentScanResult.get("reason"))) {
                    String reason = JSON.parseObject((String) contentScanResult.get("reason")).getString("riskTips");
                    updateWmNews(wmNews, Status.FAIL.getCode(), reason);
                    flag = false;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return flag;
    }

    /**
     * 修改文章内容
     *
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 从自媒体文章内容中提取文字和图片
     * 提取封面图片
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
        StringBuilder stringBuilder = new StringBuilder();
        ArrayList<String> images = new ArrayList<>();
        if (StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if ("text".equals(map.get("type"))) {
                    stringBuilder.append(map.get("value"));
                } else if ("image".equals(map.get("image"))) {
                    images.add((String) map.get("value"));
                }
            }
        }
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] coverImages = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(coverImages));
        }
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", stringBuilder.toString());
        resultMap.put("images", images);
        return resultMap;
    }


}
