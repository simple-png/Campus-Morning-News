package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmFileChunkDto;
import com.heima.model.wemedia.dtos.WmFileDto;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.wemedia.service.WmMaterialService;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/v1/material")
@Slf4j
public class WmMaterialController {

    @Autowired
    private WmMaterialService wmMaterialService;

    @PostMapping("/upload_picture")
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        return wmMaterialService.uploadPicture(multipartFile);
    }

    @PostMapping("/init_file")
    public ResponseResult initFile(WmFileDto dto) throws Exception {
        return wmMaterialService.initFile(dto);
    }

    @PostMapping("/upload_chunk")
    public ResponseResult uploadChunk(@RequestBody WmFileChunkDto dto) throws InvocationTargetException, IllegalAccessException {
        return wmMaterialService.uploadFileChunk(dto);
    }

    @PostMapping("/merge_file")
    public ResponseResult uploadChunk(@RequestParam("uploadId") String uploadId) throws ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, IOException, InvalidKeyException, XmlParserException, InvalidResponseException, InternalException {
        return wmMaterialService.mergeFileChunk(uploadId);
    }

    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmMaterialDto dto){
        return wmMaterialService.findList(dto);
    }

    @GetMapping("/del_picture/{id}")
    public ResponseResult delPicture(@PathVariable("id") Integer id){
        return wmMaterialService.delPicture(id);
    }

    @GetMapping("/collect/{id}")
    public ResponseResult collectPicture(@PathVariable("id") Integer id){
        return wmMaterialService.collectPicture(id);
    }
    @GetMapping("/cancel_collect/{id}")
    public ResponseResult cancelCollectPicture(@PathVariable("id") Integer id){
        return wmMaterialService.cancelCollectPicture(id);
    }
}
