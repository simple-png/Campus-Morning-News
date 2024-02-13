package com.heima.behavior.service.impl;

import com.heima.behavior.mapper.ApArticleMapper;
import com.heima.behavior.service.UserBehaviorService;
import com.heima.common.constants.ApUserConstants;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.behavior.dtos.BehaviorLikeDto;
import com.heima.model.behavior.dtos.BehaviorReadDto;
import com.heima.model.behavior.dtos.BehaviorUnLikeDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.thread.ApThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class UserBehaviorServiceImpl implements UserBehaviorService {
    @Autowired
    private CacheService cacheService;

    @Override
    public ResponseResult behaviorLikes(BehaviorLikeDto dto) {
        if (dto.getArticleId() == null || dto.getOperation() == null || dto.getType() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Integer userId = ApThreadLocalUtil.getUser().getId();
        String key = cacheKey(dto.getArticleId(), dto.getType(), userId, ApUserConstants.LIKES);
        cacheService.set(key, dto.getOperation().toString());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult behaviorUnLikes(BehaviorUnLikeDto dto) {
        if (dto.getArticleId() == null || dto.getType() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Integer userId = ApThreadLocalUtil.getUser().getId();
        String key = cacheKey(dto.getArticleId(), dto.getType(), userId, ApUserConstants.UNLIKE);
        cacheService.set(key, "0");
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult behaviorRead(BehaviorReadDto dto) {
        if (dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        String key = ArticleConstants.READ + dto.getArticleId();
        cacheService.incrBy(key, 1);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private String cacheKey(Long articleId, Short type, Integer userId, String behavior) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(behavior).append(":")
                .append(articleId).append(":")
                .append(userId).append(":")
                .append(type);
        return stringBuffer.toString();
    }

    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 每分钟定时将缓存中的喜欢数同步到数据库
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void reloadLikesData() {
        Set<String> keys = cacheService.scan(ApUserConstants.LIKES + "*");
        //截取文章id
        List<String> articleIdList = new ArrayList<>();
        for (String key : keys) {
            String articleId = key.split(":")[1];
            articleIdList.add(articleId);
        }
        //获取文章喜欢数更新到数据库
        for (String articleId : articleIdList) {
            Set<String> valueList = cacheService.scan(ApUserConstants.LIKES + ":" + articleId + "*");
            long count = valueList.stream().filter(value -> value.equals("0")).count();
            ApArticle apArticle = new ApArticle();
            apArticle.setLikes((int) count);
            apArticle.setId(Long.valueOf(articleId));
            apArticleMapper.updateById(apArticle);
        }
        log.info("将文章喜欢同步到数据库");
    }

    /**
     * 每分钟将缓存中文章的阅读数同步到数据库
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void reloadReadData() {
        Set<String> keys = cacheService.scan(ArticleConstants.READ + "*");
        //截取文章id
        List<String> articleIdList = new ArrayList<>();
        for (String key : keys) {
            String articleId = key.split(":")[1];
            articleIdList.add(articleId);
        }
        //获取文章阅读数更新到数据库
        List<String> articleReads = cacheService.multiGet(keys);
        for (int i = 0; i < articleReads.size(); i++) {
            ApArticle apArticle = new ApArticle();
            apArticle.setId(Long.valueOf(articleIdList.get(i)));
            apArticle.setViews(Integer.valueOf(articleReads.get(i)));
            apArticleMapper.updateById(apArticle);
        }
        log.info("将文章阅读数同步到数据库");
    }
}
