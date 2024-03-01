package com.heima.article.service.impl;

import com.heima.article.ArticleApplication;
import com.heima.common.redis.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
class HotArticleServiceImplTest {
    @Autowired
    private CacheService cacheService;

    @Test
    void listHotArticle() {
        Object o = cacheService.hGet("user_behavior_likes", "4");
        System.out.println(o.getClass() + "\n" + o);
    }
}