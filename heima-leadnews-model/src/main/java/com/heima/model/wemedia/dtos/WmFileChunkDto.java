package com.heima.model.wemedia.dtos;

import lombok.Data;

@Data
public class WmFileChunkDto {
    private String uploadId;
    private Integer chunkIndex;
    private Integer chunkSize;
}
