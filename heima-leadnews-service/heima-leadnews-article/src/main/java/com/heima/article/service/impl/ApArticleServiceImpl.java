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
import com.heima.common.constants.HotArticleConstants;
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
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.mess.UpdateArticleMess.UpdateArticleType;
import com.heima.utils.thread.ApThreadLocalUtil;
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

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public ResponseResult behaviorCollection(ArticleCollectionDto dto) {
        if (dto.getEntryId() == null || dto.getOperation() == null || dto.getType() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Integer userId = ApThreadLocalUtil.getUser().getId();

        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getEntryId());
        mess.setType(UpdateArticleType.COLLECTION);

        String userCollectionKey = ApUserConstants.COLLECTION + dto.getEntryId();
        if (dto.getOperation().equals((short) 0)) {
            //如果没有收藏
            if (!cacheService.sIsMember(userCollectionKey, userId.toString())) {
                //收藏
                cacheService.sAdd(userCollectionKey, userId.toString());
                mess.setAdd(1);
            } else {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "已经收藏过");
            }
        } else if (dto.getOperation().equals((short) 1)) {
            if (cacheService.sIsMember(userCollectionKey, userId.toString())) {
                //取消收藏
                cacheService.sRemove(userCollectionKey, userId.toString());
                mess.setAdd(-1);
            } else {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "未收藏");
            }
        }
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    @Override
    public ResponseResult loadArticleBehavior(ArticleBehaviorDto dto) {
        if (dto.getArticleId() == null || dto.getAuthorId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        String userId = ApThreadLocalUtil.getUser().getId().toString();
        Map<String, Boolean> behaviorMap = new HashMap<>();
        //是否关注
        Object follow = cacheService.hGet(ApUserConstants.FOLLOW, userId);
        if (follow instanceof String && StringUtils.isNotBlank((String) follow) && follow.equals(dto.getAuthorId().toString())) {
            behaviorMap.put("isfollow", true);
        } else {
            behaviorMap.put("isfollow", false);
        }
        //是否不喜欢
        if (cacheService.sIsMember(ApUserConstants.UNLIKE + dto.getArticleId(), userId)) {
            behaviorMap.put("isunlike", true);
        } else {
            behaviorMap.put("isunlike", false);
        }
        //是否喜欢
        if (cacheService.sIsMember(ApUserConstants.LIKES + dto.getArticleId(), userId)) {
            behaviorMap.put("islike", true);
        } else {
            behaviorMap.put("islike", false);
        }
        //是否收藏
        if (cacheService.sIsMember(ApUserConstants.COLLECTION + dto.getArticleId(), userId)) {
            behaviorMap.put("iscollection", true);
        } else {
            behaviorMap.put("iscollection", false);
        }
        return ResponseResult.okResult(behaviorMap);
    }

    @Override
    public void updateScore(ArticleVisitStreamMess mess) {
        //更新文章的阅读点赞收藏评论数量
        ApArticle article = updateArticle(mess);
        //计算文章的分值
        Integer score = computeScore(article);
        score *= 3;
        //替换当前文章对应的热点数据
        String key = ArticleConstants.HOT_ARTICLE_FIRST_PAGE + article.getChannelId();
        String articleListStr = cacheService.get(key);

        replaceDataToRedis(article, score, key, articleListStr);

        key = ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG;
        articleListStr = cacheService.get(key);
        replaceDataToRedis(article, score, key, articleListStr);
    }

    /**
     * 替换数据并且存入redis中
     *
     * @param article
     * @param score
     * @param key
     * @param articleListStr
     */
    private void replaceDataToRedis(ApArticle article, Integer score, String key, String articleListStr) {
        boolean isReplace = true;
        if (StringUtils.isNotBlank(articleListStr)) {
            List<HotArticleVo> hotArticleVos = JSON.parseArray(articleListStr, HotArticleVo.class);
            //如果缓存存在该文章，只更新分值
            for (HotArticleVo hotArticleVo : hotArticleVos) {
                if (hotArticleVo.getId().equals(article.getId())) {
                    hotArticleVo.setScore(score);
                    isReplace = false;
                    break;
                }
            }
            if (isReplace) {
                //文章热点数据大于30 找到最小的文章进行替换
                if (hotArticleVos.size() >= 30) {
                    // 找到分数最小的文章
                    HotArticleVo minScoreArticle = hotArticleVos.get(0);
                    for (HotArticleVo hotArticleVo : hotArticleVos) {
                        if (minScoreArticle.getScore() < hotArticleVo.getScore()) {
                            minScoreArticle = hotArticleVo;
                        }
                    }
                    if (minScoreArticle.getScore() < score) {
                        hotArticleVos.remove(minScoreArticle);
                        HotArticleVo hotArticleVo = new HotArticleVo();
                        BeanUtils.copyProperties(article, hotArticleVo);
                        hotArticleVo.setScore(score);
                        hotArticleVos.add(hotArticleVo);
                    }
                }
                //直接添加新数据
                else {
                    HotArticleVo hotArticleVo = new HotArticleVo();
                    BeanUtils.copyProperties(article, hotArticleVo);
                    hotArticleVo.setScore(score);
                    hotArticleVos.add(hotArticleVo);
                }
            }
            hotArticleVos = hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore)).collect(Collectors.toList());
            cacheService.set(key, JSON.toJSONString(hotArticleVos));
        }
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

    private ApArticle updateArticle(ArticleVisitStreamMess mess) {
        ApArticle apArticle = getById(mess.getArticleId());
        apArticle.setCollection(apArticle.getCollection() == null ? mess.getCollect() : apArticle.getCollection() + mess.getCollect());
        apArticle.setLikes(apArticle.getLikes() == null ? mess.getLike() : apArticle.getLikes() + mess.getLike());
        apArticle.setViews(apArticle.getViews() == null ? mess.getView() : apArticle.getViews() + mess.getView());
        apArticle.setComment(apArticle.getComment() == null ? mess.getComment() : apArticle.getComment() + mess.getComment());
        updateById(apArticle);
        return apArticle;
    }
}
