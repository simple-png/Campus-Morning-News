package com.heima.article.stream;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.mess.UpdateArticleMess.UpdateArticleType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class HotArticleStreamHandler {
    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        //接受消息
        KStream<String, String> stream = streamsBuilder.stream(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC);
        //聚合流式处理
        //将{hot.article.score.topic:mess对象}->{文章id:对应的用户行为信息}
        stream.map((key, value) -> {
                    UpdateArticleMess mess = JSON.parseObject(value, UpdateArticleMess.class);
                    //重置消息的key,value  如'123456':'like:1'
                    if (mess.getAdd() == null) {
                        mess.setAdd(0);
                    }
                    return new KeyValue<>(mess.getArticleId().toString(), mess.getType().name() + ":" + mess.getAdd().toString());
                })
                .groupBy((key, value) -> key)
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                .aggregate(new Initializer<String>() {
                               /**
                                * 初始方法，返回值是消息的value
                                * @return
                                */
                               @Override
                               public String apply() {
                                   return "COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0";
                               }
                           },
                        /**
                         * 真正的聚合操作，返回是消息的value
                         * 例:在10s内
                         * 开始为null
                         * aggValue初始化为COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0
                         * 第二次收到信息:如like:1 aggValue为COLLECTION:0,COMMENT:0,LIKES:1,VIEWS:0
                         * 10s后将发送到HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC
                         */
                        new Aggregator<String, String, String>() {
                            @Override
                            public String apply(String key, String value, String aggValue) {
                                if (StringUtils.isBlank(value)) {
                                    return aggValue;
                                }
                                String[] aggAry = aggValue.split(",");
                                int collection = 0, comment = 0, likes = 0, views = 0;
                                for (String agg : aggAry) {
                                    String[] split = agg.split(":");
                                    switch (UpdateArticleType.valueOf(split[0])) {
                                        case COLLECTION:
                                            collection = Integer.parseInt(split[1]);
                                            break;
                                        case LIKES:
                                            likes = Integer.parseInt(split[1]);
                                            break;
                                        case VIEWS:
                                            views = Integer.parseInt(split[1]);
                                            break;
                                        case COMMENT:
                                            comment = Integer.parseInt(split[1]);
                                            break;
                                    }
                                }
                                String[] valAry = value.split(":");
                                switch (UpdateArticleType.valueOf(valAry[0])) {
                                    case COLLECTION:
                                        collection += Integer.parseInt(valAry[1]);
                                        break;
                                    case LIKES:
                                        likes += Integer.parseInt(valAry[1]);
                                        break;
                                    case VIEWS:
                                        views += Integer.parseInt(valAry[1]);
                                        break;
                                    case COMMENT:
                                        comment += Integer.parseInt(valAry[1]);
                                        break;
                                }
                                String format = String.format("COLLECTION:%d,COMMENT:%d,LIKES:%d,VIEWS:%d", collection, comment, likes, views);
                                System.out.println("文字id:" + key);
                                System.out.println("当前时间窗口处理结果:" + format);
                                return format;
                            }
                        }, Materialized.as("hot-article-stream-count-001")
                )
                .toStream()
                //将{文章id:COLLECTION:0,COMMENT:0,LIKES:1,VIEWS:0}->{文章id:ArticleVisitStreamMess对象的String}
                .map((key, value) -> {
                    return new KeyValue<>(key.key().toString(), formatObj(key.key().toString(), value));
                })
                //发送消息
                .to(HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC);
        return stream;
    }

    /**
     * 格式化消息的value数据
     *
     * @param articleId
     * @param value
     * @return
     */
    public String formatObj(String articleId, String value) {
        ArticleVisitStreamMess mess = new ArticleVisitStreamMess();
        mess.setArticleId(Long.valueOf(articleId));
        //COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0
        String[] valAry = value.split(",");
        for (String val : valAry) {
            String[] split = val.split(":");
            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])) {
                case COLLECTION:
                    mess.setCollect(Integer.parseInt(split[1]));
                    break;
                case COMMENT:
                    mess.setComment(Integer.parseInt(split[1]));
                    break;
                case LIKES:
                    mess.setLike(Integer.parseInt(split[1]));
                    break;
                case VIEWS:
                    mess.setView(Integer.parseInt(split[1]));
                    break;
            }
        }
        log.info("聚合消息处理之后的结果为:{}", JSON.toJSONString(mess));
        return JSON.toJSONString(mess);

    }
}
