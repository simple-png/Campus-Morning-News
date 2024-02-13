package com.heima.model.article.dtos;

import lombok.Data;

import java.util.Date;

@Data
public class ArticleCollectionDto {
    private Long entryId;
    private Short operation;
    private Date publishedTime;
    private Short type;
}
