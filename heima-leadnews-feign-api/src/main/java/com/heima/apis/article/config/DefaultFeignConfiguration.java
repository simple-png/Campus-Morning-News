package com.heima.apis.article.config;

import com.heima.apis.article.fallback.ArticleClientFallbackFactory;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

@Slf4j
public class DefaultFeignConfiguration {
    @Bean
    public Logger.Level logLevel(){
        return Logger.Level.BASIC;
    }
    @Bean
    public ArticleClientFallbackFactory userClientFallbackFactory(){
        return new ArticleClientFallbackFactory();
    }
}