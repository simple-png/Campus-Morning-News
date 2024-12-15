package com.heima.model.wemedia.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("wm_file")
public class WmFile {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("url")
    private String url;

    @TableField("file_size")
    private Integer fileSize;

    @TableField("file_md5")
    private String fileMd5;

    @TableField("content_type")
    private String contentType;

    @TableField("upload_id")
    private String uploadId;

    @TableField("file_uuid")
    private String fileUuid;

    @TableField("status")
    private Integer status;

    @TableField("created_time")
    private Date createdTime;

    @TableField("update_time")
    private Date updateTime;
}