package com.bxt.picturebackend.manager;

import cn.hutool.core.io.FileUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
@Slf4j
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
     * 下载带有盲水印的图片到本地文件
     */
    public COSObject downloadPictureToFileWithBlindWatermark(String key){

        GetObjectRequest getObj = new GetObjectRequest(cosClientConfig.getBucket(), key);
        System.out.println("key:"+key);
        String text="bxt";
        // 转 Base64
        String textBase64 = Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
        String rule = "?watermark/3/type/3/text/"+textBase64;

        System.out.println("rule:"+rule);
        getObj.putCustomQueryParameter(rule, null);
        return cosClient.getObject(getObj);
    }
    /**
     * 下载带有文字水印的图片到本地文件
     */
    public COSObject downloadPictureToFileWithWordWatermark(String key){
        System.out.println("word");
        GetObjectRequest getObj = new GetObjectRequest(cosClientConfig.getBucket(), key);
        System.out.println("key:"+key);
        String textBase64 = Base64.getEncoder()
                .encodeToString(key.getBytes(StandardCharsets.UTF_8));
        String rule = "watermark/2/degree/45/text/"+textBase64;
        getObj.putCustomQueryParameter(rule, null);
        return cosClient.getObject(getObj);
    }


    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        // 图片缩放（缩放到 128*128）
        PicOperations.Rule thumbNailRule = new PicOperations.Rule();
        thumbNailRule.setBucket(cosClientConfig.getBucket());
        String thumbNailKey = FileUtil.mainName(key) + "_thumbnail.webp";
        thumbNailRule.setFileId(thumbNailKey);
        thumbNailRule.setRule("imageMogr2/thumbnail/128x128");
        rules.add(thumbNailRule);
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }
    public boolean deletePictureObject(String key) {
        try {
            cosClient.deleteObject(cosClientConfig.getBucket(), key);
            return true;
        } catch (Exception e) {
            log.error("删除对象失败，key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除对象失败");
        }
    }


}
