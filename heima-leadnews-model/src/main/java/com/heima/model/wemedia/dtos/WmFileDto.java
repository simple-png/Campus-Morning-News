package com.heima.model.wemedia.dtos;

import lombok.Data;

@Data
public class WmFileDto {
    private String fileName;
    private Integer fileSize;
    private Integer chunkSize;
    private String contentType;
    private String fileMd5;
}
