package com.heima.model.wemedia.vos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;

@Data
public class WmFileVo {
    private String updateId;
    private LinkedList<Item> partList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private String url;
        private Integer chunkIndex;
    }
}
