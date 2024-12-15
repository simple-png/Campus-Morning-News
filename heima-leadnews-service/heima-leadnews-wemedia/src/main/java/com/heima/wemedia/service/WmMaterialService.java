package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmFileChunkDto;
import com.heima.model.wemedia.dtos.WmFileDto;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface WmMaterialService extends IService<WmMaterial> {

    /**
     * 图片上传
     * @param multipartFile
     * @return
     */
    public ResponseResult uploadPicture(MultipartFile multipartFile);

    /**
     * 素材列表查询
     * @param dto
     * @return
     */
    public ResponseResult findList(WmMaterialDto dto);

    /**
     * 图片删除
     * @param id
     * @return
     */
    public ResponseResult delPicture(Integer id);

    /**
     * 收藏图片
     * @param id
     * @return
     */
    public ResponseResult collectPicture(Integer id);

    /**
     * 取消收藏图片
     * @param id
     * @return
     */
    public ResponseResult cancelCollectPicture(Integer id);

    public ResponseResult initFile(WmFileDto wmFileDto) throws Exception;

    public ResponseResult uploadFileChunk(WmFileChunkDto dto) throws InvocationTargetException, IllegalAccessException;

    public ResponseResult mergeFileChunk(String uploadId) throws ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, IOException, InvalidKeyException, XmlParserException, InvalidResponseException, InternalException;
}