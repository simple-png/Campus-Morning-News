package com.heima.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.UserBehaviorService;
import com.heima.common.constants.ApUserConstants;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.BehaviorLikeDto;
import com.heima.model.behavior.dtos.BehaviorReadDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.mess.UpdateArticleMess.UpdateArticleType;
import com.heima.utils.thread.ApThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserBehaviorServiceImpl implements UserBehaviorService {
    @Autowired
    private CacheService cacheService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public ResponseResult behaviorLikes(BehaviorLikeDto dto) {
        if (dto.getArticleId() == null || dto.getOperation() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Integer userId = ApThreadLocalUtil.getUser().getId();

        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleType.LIKES);

        String userLikeKey = ApUserConstants.LIKES + dto.getArticleId();
        if (dto.getOperation().equals((short) 0)) {
            //如果没有点过赞
            if (!cacheService.sIsMember(userLikeKey, userId.toString())) {
                //点赞
                cacheService.sAdd(userLikeKey, userId.toString());
                mess.setAdd(1);
            } else {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "已经点过赞");
            }
        } else if (dto.getOperation().equals((short) 1)) {
            if (cacheService.sIsMember(userLikeKey, userId.toString())) {
                //取消点赞
                cacheService.sRemove(userLikeKey, userId.toString());
                mess.setAdd(-1);
            } else {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "未点赞");
            }
        } else {
            //不喜欢
            cacheService.sAdd(ApUserConstants.UNLIKE + dto.getArticleId(), userId.toString());
            mess.setAdd(-1);
        }
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    @Override
    public ResponseResult behaviorRead(BehaviorReadDto dto) {
        if (dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleType.VIEWS);
        mess.setAdd(1);

        cacheService.hIncrBy(ArticleConstants.READ, dto.getArticleId().toString(), 1);
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
