package com.bxt.picturebackend.manager;

import com.bxt.picturebackend.config.CosClientConfig;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

@Component
public class CosManager {
    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }
    /**
     * 下载对象
     *
     * @param key 对象键
     * @return COSObject（可通过 getObjectContent() 获取 InputStream）
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(
                cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }
    /**
     * 下载对象并保存到本地文件
     *
     * @param key        对象键
     * @param localFile  本地文件路径
     */
    public void downloadObjectToFile(String key, File localFile) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(
                cosClientConfig.getBucket(), key);
        cosClient.getObject(getObjectRequest, localFile);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        PicOperations picOperations=new PicOperations();
        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        if (putObjectResult == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        }
        return cosClient.putObject(putObjectRequest);
    }


}
