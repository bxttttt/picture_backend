package com.bxt.picturebackend.manager.Uploader;

import cn.hutool.core.collection.CollUtil;
import com.bxt.picturebackend.config.CosClientConfig;
import com.bxt.picturebackend.config.RabbitMQConfig;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.dto.picture.PictureFileDeleteEvent;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.manager.CosManager;
import com.bxt.picturebackend.manager.FileManager;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;

import java.io.File;
import java.io.IOException;
import java.util.List;
/*
用策略模式、模板方法模式优化原来的代码
 */
@Slf4j
public abstract class AbstractPictureUploader<T>{
    @Resource
    protected CosManager cosManager;
    @Resource
    protected CosClientConfig cosClientConfig;

    @Resource
    @Lazy
    protected FileManager fileManager;
    @Resource
    protected RabbitTemplate rabbitTemplate;

    public final UploadPictureResult uploadPicture(T source,String uploadPathPrefix){
        validPicture(source);
        String originFilename = getOriginFileName(source);
        String uploadPath = generateUploadPath(originFilename,uploadPathPrefix);
        File file=null;
        UploadPictureResult uploadPictureResult=new UploadPictureResult();
        try {
            file=getFileFromSource(source,uploadPath);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                CIObject compressedObject = objectList.get(0);
                CIObject thumbnailObject = objectList.get(1);
                // 删除原图
//                fileManager.deleteOriginPicture(compressedObject.getKey());
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.PICTURE_FILE_DELETE_QUEUE,
                        new PictureFileDeleteEvent(compressedObject.getKey())
                );

                return fileManager.buildResult(originFilename,compressedObject,thumbnailObject);
            }
        }catch (Exception e){
            log.error("上传图片失败，上传路径：{}，错误信息：{}", uploadPath, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败，请稍后重试");
        }finally {
            fileManager.deleteTemplateFile(file);
        }
        return null;
    }

    public abstract String getOriginFileName(T source) ;

    public abstract File getFileFromSource(T source, String uploadPath) throws IOException;

    protected abstract String generateUploadPath(String originFileName, String uploadPathPrefix) ;

    protected abstract void validPicture(T source);
}
