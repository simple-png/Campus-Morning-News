package com.heima.model.wemedia.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("wm_file_chunk")
public class WmFileChunk {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("url")
    private String url;

    @TableField("update_id")
    private String uploadId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("chunk_size")
    private Integer chunkSize;

    @TableField("status")
    private Integer status;

    @TableField("created_time")
    private Date createdTime;

    @TableField("update_time")
    private Date updateTime;
}
