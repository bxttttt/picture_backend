package com.bxt.picturebackend.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.config.CosClientConfig;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.exception.ThrowUtils;
import com.bxt.picturebackend.manager.Uploader.MultipartFileUploader;
import com.bxt.picturebackend.manager.Uploader.UrlFileUploader;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.enums.UserRoleEnum;
import com.bxt.picturebackend.vo.PictureVo;
import com.bxt.picturebackend.vo.UserLoginVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.bytecode.stackmap.BasicBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@Slf4j
@Service
public class FileManager {
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

    @Resource
    private CosManager cosManager;
    public UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailCiObject) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth((long) picWidth);
        uploadPictureResult.setPicHeight((long) picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        // 设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        return uploadPictureResult;
    }
    @Autowired
    private MultipartFileUploader multipartFileUploader;
    @Autowired
    private UrlFileUploader urlFileUploader;
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix){
        return multipartFileUploader.uploadPicture(multipartFile,uploadPathPrefix);
    }
    public UploadPictureResult uploadPicture(String fileUrl,String uploadPathPrefix){
        return urlFileUploader.uploadPicture(fileUrl,uploadPathPrefix);
    }
    @PostConstruct
    public void init() {
        System.out.println("multipartFileUploader = " + multipartFileUploader);
        System.out.println("urlFileUploader = " + urlFileUploader);
    }

//    @Deprecated
//    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
//        UploadPictureResult uploadPictureResult = new UploadPictureResult();
//
//        // 校验图片
//        validPicture(multipartFile);
//        // 图片上传地址
//        String uuid= RandomUtil.randomString(16);
//        String originalFilename = multipartFile.getOriginalFilename();
//        String uploadFileName = (DateUtil.formatDate(new Date()))+"_" +uuid +"."+ FileUtil.getSuffix(originalFilename);
//        String uploadPath = uploadPathPrefix + File.separator + uploadFileName;
//        // 解析结果并返回
//        File file=null;
//        UploadPictureResult result = new UploadPictureResult();
//        try {
//            // 上传文件
//            file = File.createTempFile(uploadPath, null);
//            multipartFile.transferTo(file);
//            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
//            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
//            List<CIObject> objectList = processResults.getObjectList();
//            if (CollUtil.isNotEmpty(objectList)){
//                CIObject compressedObject = objectList.get(0);
//                CIObject thumbnailObject = objectList.get(1);
//                return buildResult(originalFilename,compressedObject,thumbnailObject);
//            }
//            // 封装返回结果
//            result.setPicFormat( imageInfo.getFormat());
//            result.setPicHeight((long) imageInfo.getHeight());
//            result.setPicWidth((long) imageInfo.getWidth());
//            result.setPicSize(multipartFile.getSize());
//            result.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
//            result.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));
//            result.setPicScale( (double) imageInfo.getWidth() / imageInfo.getHeight());
//        } catch (Exception e) {
//            log.error("file upload error, filepath = " + uploadPath, e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
//        } finally {
//
//            deleteOriginPicture(result.getUrl());
//            deleteTemplateFile(file);
//        }
//
//        return result;
//    }
//    @Deprecated
//    public UploadPictureResult uploadPicture(String fileUrl,String uploadPathPrefix) {
//        UploadPictureResult uploadPictureResult = new UploadPictureResult();
//        // 校验图片
//        validPicture(fileUrl);
//        // 图片上传地址
//        String uuid= RandomUtil.randomString(16);
//        String originFilename = FileUtil.mainName(fileUrl);
//        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
//                FileUtil.getSuffix(originFilename));
//        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
//        File file = null;
//        UploadPictureResult result=new UploadPictureResult();
//        try {
//            // 创建临时文件
//            file = File.createTempFile(uploadPath, null);
//            // multipartFile.transferTo(file);
//            HttpUtil.downloadFile(fileUrl, file);
//            // 上传图片
//            // ... 其余代码保持不变
//            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
//            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
//            List<CIObject> objectList = processResults.getObjectList();
//            if (CollUtil.isNotEmpty(objectList)){
//                CIObject compressedObject = objectList.get(0);
//                CIObject thumbnailObject = objectList.get(1);
//                deleteOriginPicture(compressedObject.getKey());
//                return buildResult(originFilename,compressedObject,thumbnailObject);
//            }
//            // 封装返回结果
//            result.setPicFormat( imageInfo.getFormat());
//            result.setPicHeight((long) imageInfo.getHeight());
//            result.setPicWidth((long) imageInfo.getWidth());
//            result.setPicSize(file.length());
//            result.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
//            result.setPicName(originFilename);
//            result.setPicScale( (double) imageInfo.getWidth() / imageInfo.getHeight());
//        } catch (Exception e) {
//            log.error("图片上传到对象存储失败", e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
//        } finally {
////            deletePicture(result.getUrl());
//            deleteTemplateFile(file);
//        }
//        return result;
//
//    }
//

    private String getKeyFromUrl(String url) {
        String domain=cosClientConfig.getHost()+"/";
        if (url != null && url.startsWith(domain)) {
            return url.substring(domain.length());
        }
        return url;
    }
    // 用于删除原图（既不是缩略图，也不是webp文件）
    public boolean deleteOriginPicture(String key) {
//        String key= getKeyFromUrl(url);
        System.out.println("删除对象的key1: " + key);
        // 删除key中的webp
        if (StrUtil.isBlank(key)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        }
        if (key.endsWith("webp")) {
            key = key.substring(0, key.length() - 4); // 去掉 .webp 后缀
        }
        System.out.println("删除对象的key2: " + key);
        try{
            cosManager.deletePictureObject(key);
        }catch (Exception e){
            log.error("删除对象失败，key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除对象失败");
        }
        // 准备所有需要删除的key列表，包含原key和jpg/png/gif/jpeg格式
        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(key); // 原key（已经去掉webp）

        // 拼接四种常见图片格式后缀
        String[] suffixes = {"jpg", "png", "gif", "jpeg"};
        for (String suffix : suffixes) {
            keysToDelete.add(key + suffix);
        }

        // 遍历删除
        for (String k : keysToDelete) {
            try {
                cosManager.deletePictureObject(k);
                System.out.println("已删除对象，key: " + k);
            } catch (Exception e) {
                log.error("删除对象失败，key: {}", k, e);
                // 这里可以根据业务选择继续删除还是抛异常，我这里继续尝试删除剩余
            }
        }
        return true;

    }


    public static void deleteTemplateFile(File file) {
        if (file != null) {
            // 删除临时文件
            boolean delete = file.delete();
            if (!delete) {
                log.error("file delete error");
            }
        }
    }
    @Deprecated
    private void validPicture(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的文件不能为空");
        }
        String contentType = multipartFile.getContentType();
//        if (contentType == null || !contentType.startsWith("image/")) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的文件不是图片类型");
//        }
        long filesize = multipartFile.getSize();
        if (filesize <= 0 || filesize > 5 * 1024 * 1024) { // 限制图片大小为5MB
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的图片大小不符合要求");
        }
        String suffix=FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (!"jpg".equalsIgnoreCase(suffix) && !"jpeg".equalsIgnoreCase(suffix) &&
                !"png".equalsIgnoreCase(suffix) && !"gif".equalsIgnoreCase(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的图片格式不支持");
        }
    }
    @Deprecated
    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        try {
            // 1. 验证 URL 格式
            new URL(fileUrl); // 验证是否是合法的 URL
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 2. 校验 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 5 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }




}
