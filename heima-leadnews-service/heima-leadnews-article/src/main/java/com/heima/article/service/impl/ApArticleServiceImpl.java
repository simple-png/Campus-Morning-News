package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ApUserConstants;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleBehaviorDto;
import com.heima.model.article.dtos.ArticleCollectionDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.thread.ApThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {
    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private CacheService cacheService;
    private static final short MAX_PAGE_SIZE = 50;

    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        if (type.equals(ArticleConstants.LOADTYPE_LOAD_TOP)) {
            String jsonString = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if (StringUtils.isNotBlank(jsonString)) {
                List<HotArticleVo> hotArticleVos = JSON.parseArray(jsonString, HotArticleVo.class);
                return ResponseResult.okResult(hotArticleVos);
            }
        }
        //分页条数的校验
        Integer size = dto.getSize();
        if (size == null || size == 0) {
            size = 10;
        }
        //分页的值不超过50
        size = Math.min(size, MAX_PAGE_SIZE);
        //校验type参数
        if (!ArticleConstants.LOADTYPE_LOAD_MORE.equals(type) && !ArticleConstants.LOADTYPE_LOAD_NEW.equals(type)) {
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //校验频道参数
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //校验时间参数
        if (dto.getMaxBehotTime() == null) {
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime() == null) {
            dto.setMinBehotTime(new Date());
        }
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, type);
        return ResponseResult.okResult(apArticles);
    }

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;

    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        //1.检查参数
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto, apArticle);
        System.out.println(dto.getContent());
        //2.判断是否存在id
        if (dto.getId() == null) {
            //2.1 不存在 保存 文章配置 文章内容
            save(apArticle);
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(apArticleContent);
        } else {
            //2.2 存在 修改 文章 文章内容
            updateById(apArticle);
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery()
                    .eq(ApArticleContent::getArticleId, apArticle.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }
        //异步调用 生成静态文件上传到minio中
        articleFreemarkerService.buildArticleToMinio(apArticle, dto.getContent());
        return ResponseResult.okResult(apArticle.getId());
    }

    @Override
    public ResponseResult behaviorCollection(ArticleCollectionDto dto) {
        if (dto.getOperation() == null || dto.getType() == null || dto.getEntryId() == null || dto.getPublishedTime() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Integer userId = ApThreadLocalUtil.getUser().getId();
        String key = ApUserConstants.COLLECTION + ":"
                + dto.getEntryId() + ":"
                + userId + ":"
                + dto.getType();
        cacheService.set(key, String.valueOf(dto.getOperation()));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult loadArticleBehavior(ArticleBehaviorDto dto) {
        Integer userId = ApThreadLocalUtil.getUser().getId();
        Map<String, Boolean> behaviorMap = new HashMap<>();
        //获取是否喜欢||不喜欢||收藏
        putBehavior(dto.getArticleId(), userId, behaviorMap, "islike");
        putBehavior(dto.getArticleId(), userId, behaviorMap, "isunlike");
        putBehavior(dto.getArticleId(), userId, behaviorMap, "iscollection");
        String followKey = ApUserConstants.FOLLOW + ":" +
                dto.getArticleId() + ":" +
                userId + ":" +
                dto.getAuthorId();
        String isFollow = cacheService.get(followKey);
        boolean isFollowBool = false;
        if (isFollow != null && isFollow.equals("0")) {
            isFollowBool = true;
        }
        behaviorMap.put("isfollow", isFollowBool);
        return ResponseResult.okResult(behaviorMap);
    }

    private void putBehavior(Long articleId, Integer userId, Map<String, Boolean> behaviorMap, String isLike) {
        String likeKey = ApUserConstants.LIKES + ":"
                + articleId + ":"
                + userId + ":"
                + 0;
        String value = cacheService.get(likeKey);
        boolean operationBool = false;
        if (value != null && value.equals("0")) {
            operationBool = true;
        }
        behaviorMap.put(isLike, operationBool);
    }

    /**
     * 每分钟定时将缓存中的喜欢数同步到数据库
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void reloadCollectionData() {
        Set<String> keys = cacheService.scan(ApUserConstants.COLLECTION + "*");
        //截取文章id
        List<String> articleIdList = new ArrayList<>();
        for (String key : keys) {
            String articleId = key.split(":")[1];
            articleIdList.add(articleId);
        }
        //获取文章收藏数更新到数据库
        for (String articleId : articleIdList) {
            Set<String> valueList = cacheService.scan(ApUserConstants.COLLECTION + ":" + articleId + "*");
            long count = valueList.stream().filter(value -> value.equals("0")).count();
            ApArticle apArticle = new ApArticle();
            apArticle.setCollection((int) count);
            apArticle.setId(Long.valueOf(articleId));
            apArticleMapper.updateById(apArticle);
        }
        log.info("将文章喜欢同步到数据库");
    }
}
