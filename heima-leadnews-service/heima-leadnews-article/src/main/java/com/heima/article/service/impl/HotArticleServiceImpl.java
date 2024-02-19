package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HotArticleServiceImpl implements HotArticleService {
    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private CacheService cacheService;

    @Override
    public void listHotArticle() {
        //查询前5天的文章数据
        Date date = DateTime.now().minusDays(5).toDate();
        List<ApArticle> apArticleList = apArticleMapper.findArticleListByLast5Day(date);
        //计算文章的分值
        List<HotArticleVo> hotArticleVos = computeHotArticle(apArticleList);
        //为每个频道缓存30条分数较高的文章
        if (!hotArticleVos.isEmpty()) {
            cacheTagToRedis(hotArticleVos);
        }
    }

    @Autowired
    private IWemediaClient wemediaClient;

    private void cacheTagToRedis(List<HotArticleVo> hotArticleVos) {
        ResponseResult responseResult = wemediaClient.getChannels();
        if (responseResult.getCode().equals(200)) {
            String jsonString = JSON.toJSONString(responseResult.getData());
            List<WmChannel> wmChannels = JSON.parseArray(jsonString, WmChannel.class);
            if (wmChannels != null && !wmChannels.isEmpty()) {
                for (WmChannel wmChannel : wmChannels) {
                    List<HotArticleVo> collect = hotArticleVos.stream().filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
                    //给文章排序,取30条分值较高的文章存入redis key:频道id value:30条分值较高的文章
                    sortAndCache(collect, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
                }
            }
        }
        sortAndCache(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);

    }

    /**
     * 排序并且缓存数据
     * @param hotArticleList
     * @param key
     */
    private void sortAndCache(List<HotArticleVo> hotArticleList, String key) {
        hotArticleList = hotArticleList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
        if (hotArticleList.size() > 30) {
            hotArticleList = hotArticleList.subList(0, 30);
        }
        cacheService.set(key, JSON.toJSONString(hotArticleList));
    }

    private List<HotArticleVo> computeHotArticle(List<ApArticle> apArticleList) {
        List<HotArticleVo> hotArticleVoList = new ArrayList<>();
        if (apArticleList != null && !apArticleList.isEmpty()) {
            for (ApArticle apArticle : apArticleList) {
                HotArticleVo hotArticleVo = new HotArticleVo();
                BeanUtils.copyProperties(apArticle, hotArticleVo);
                Integer score = computeScore(apArticle);
                hotArticleVo.setScore(score);
                hotArticleVoList.add(hotArticleVo);
            }
        }
        return hotArticleVoList;
    }

    private Integer computeScore(ApArticle apArticle) {
        int score = 0;
        if (apArticle.getLikes() != null) {
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if (apArticle.getCollection() != null) {
            score += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }
        if (apArticle.getComment() != null) {
            score += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if (apArticle.getViews() != null) {
            score += apArticle.getViews();
        }
        return score;
    }
}
