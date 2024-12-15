package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmMaterialConstants;
import com.heima.common.redis.CacheService;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmFileChunkDto;
import com.heima.model.wemedia.dtos.WmFileDto;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmFile;
import com.heima.model.wemedia.pojos.WmFileChunk;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.vos.WmFileVo;
import com.heima.model.wemedia.vos.WmFileVo.Item;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmFileChunkMapper;
import com.heima.wemedia.mapper.WmFileMapper;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmFileMapper wmFileMapper;
    @Autowired
    private WmFileChunkMapper wmFileChunkMapper;
    @Autowired
    private CacheService cacheService;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        //1.检查参数
        if (multipartFile == null || multipartFile.getSize() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.上传图片到miniIO
        String fileName = UUID.randomUUID().toString().replace("-", "");
        String originalFileName = multipartFile.getOriginalFilename();
        String postfix = originalFileName.substring(originalFileName.lastIndexOf("."));
        //url路径
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName + postfix, multipartFile.getInputStream());
            log.info("上传图片到minIO中,fileId:{}", fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("WmMaterialServiceImpl-上传文件失败");
        }
        //3.保存到数据库中
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
        wmMaterial.setUrl(fileId);
        wmMaterial.setIsCollection((short) 0);
        wmMaterial.setType((short) 0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);
        //4.返回结果
        return ResponseResult.okResult(wmMaterial);
    }

    @Override
    public ResponseResult findList(WmMaterialDto dto) {
        //1.检查参数
        dto.checkParam();
        //2.分页查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (dto.getIsCollection() != null && dto.getIsCollection() == 1) {
            lambdaQueryWrapper.eq(WmMaterial::getIsCollection, dto.getIsCollection());
        }
        lambdaQueryWrapper.eq(WmMaterial::getUserId, WmThreadLocalUtil.getUser().getId());
        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);
        page = page(page, lambdaQueryWrapper);
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult delPicture(Integer id) {
        //1.检擦参数
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否在数据库中
        WmMaterial material = getById(id);
        if (material == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否与文章有关联
        List<WmNewsMaterial> wmNewsMaterials = wmNewsMaterialMapper.selectList(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getMaterialId, material.getId()));
        if (wmNewsMaterials != null && !wmNewsMaterials.isEmpty()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.FAIL_DEL_DOCUMENT);
        }
        //删除图片
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult collectPicture(Integer id) {
        return isCollectPicture(id,WemediaConstants.COLLECT_MATERIAL);
    }

    @Override
    public ResponseResult cancelCollectPicture(Integer id) {
        return isCollectPicture(id,WemediaConstants.CANCEL_COLLECT_MATERIAL);
    }

    private ResponseResult isCollectPicture(Integer id,Short isCollect){
        //1.检擦参数
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否在数据库中
        WmMaterial material = getById(id);
        if (material == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否收藏
        material.setIsCollection(isCollect);
        //保存
        updateById(material);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult initFile(WmFileDto wmFileDto) throws Exception {
        // 1. 检查 Redis 缓存
        Boolean isExistsFile = cacheService.sIsMember(WmMaterialConstants.FILES, wmFileDto.getFileMd5());
        if (isExistsFile) {
            // 2. 如果缓存中存在，则处理已有文件逻辑
            return handleExistingFile(wmFileDto);
        } else {
            // 3. 如果缓存中不存在，则处理新文件的初始化逻辑
            return handleNewFile(wmFileDto);
        }
    }

    private ResponseResult handleExistingFile(WmFileDto wmFileDto) {
        WmFile wmFile = wmFileMapper.selectOne(Wrappers.<WmFile>lambdaQuery().eq(WmFile::getFileMd5, wmFileDto.getFileMd5()));
        if (wmFile == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        // 文件已经上传完成
        if (wmFile.getStatus().equals(WmMaterialConstants.FINISHED)) {
            return ResponseResult.okResult(wmFile);
        }

        // 查询未完成的分片
        List<WmFileChunk> wmFileChunks = wmFileChunkMapper.selectList(Wrappers.<WmFileChunk>lambdaQuery()
                .eq(WmFileChunk::getUploadId, wmFile.getUploadId())
                .ne(WmFileChunk::getStatus, WmMaterialConstants.FINISHED));

        // 构建返回数据
        LinkedList<Item> items = wmFileChunks.stream().map(chunk -> {
            Item item = new Item();
            item.setUrl(chunk.getUrl());
            item.setChunkIndex(chunk.getChunkIndex());
            return item;
        }).collect(Collectors.toCollection(LinkedList::new));

        WmFileVo wmFileVo = new WmFileVo();
        wmFileVo.setUpdateId(wmFile.getUploadId());
        wmFileVo.setPartList(items);

        return ResponseResult.okResult(wmFileVo);
    }

    private ResponseResult handleNewFile(WmFileDto wmFileDto) throws Exception {
        // 生成文件唯一标识
        String minioFileName = UUID.randomUUID().toString().replace("-", "");

        // 初始化 MinIO 文件
        WmFileVo wmFileVo = fileStorageService.initFile(minioFileName, wmFileDto.getChunkSize());

        // 保存文件记录到数据库
        WmFile wmFile = new WmFile();
        BeanUtils.copyProperties(wmFileDto, wmFile);
        wmFile.setFileUuid(minioFileName);
        wmFile.setUploadId(wmFileVo.getUpdateId());
        wmFile.setStatus(WmMaterialConstants.UPLOADING);
        wmFile.setCreatedTime(new Date());
        wmFile.setUpdateTime(new Date());
        wmFileMapper.insert(wmFile);

        // 保存分片记录到数据库
        saveFileChunks(wmFileVo.getPartList(), wmFileVo.getUpdateId());

        return ResponseResult.okResult(wmFileVo);
    }

    private void saveFileChunks(List<Item> partList, String uploadId) {
        partList.forEach(item -> {
            WmFileChunk fileChunk = new WmFileChunk();
            fileChunk.setUrl(item.getUrl());
            fileChunk.setUploadId(uploadId);
            fileChunk.setChunkIndex(item.getChunkIndex());
            fileChunk.setStatus(WmMaterialConstants.UPLOADING);
            fileChunk.setCreatedTime(new Date());
            fileChunk.setUpdateTime(new Date());
            wmFileChunkMapper.insert(fileChunk);
        });
    }

    @Override
    public ResponseResult uploadFileChunk(WmFileChunkDto dto) {
        WmFileChunk fileChunk = wmFileChunkMapper.selectOne(Wrappers.<WmFileChunk>lambdaQuery()
                .eq(WmFileChunk::getUploadId, dto.getUploadId())
                .eq(WmFileChunk::getChunkIndex, dto.getChunkIndex()));
        BeanUtils.copyProperties(dto, fileChunk);
        fileChunk.setStatus(WmMaterialConstants.FINISHED);
        wmFileChunkMapper.updateById(fileChunk);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult mergeFileChunk(String uploadId) throws ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, IOException, InvalidKeyException, XmlParserException, InvalidResponseException, InternalException {
        WmFile file = wmFileMapper.selectOne(Wrappers.<WmFile>lambdaQuery().eq(WmFile::getUploadId, uploadId));
        fileStorageService.mergeFile(file.getFileUuid(), uploadId);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
