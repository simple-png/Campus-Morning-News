package com.heima.article.controller.v1;

import com.heima.article.service.ApArticleService;
import com.heima.model.article.dtos.ArticleCollectionDto;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class ArticleBehaviorController {
    @Autowired
    private ApArticleService apArticleService;
    @PostMapping("/collection_behavior")
    public ResponseResult behaviorCollection(@RequestBody ArticleCollectionDto dto){
        return apArticleService.behaviorCollection(dto);
    }
}
