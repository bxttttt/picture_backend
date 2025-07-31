package com.bxt.picturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.config.CosClientConfig;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;

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
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();

        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid= RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFileName = (DateUtil.formatDate(new Date()))+"_" +uuid +"."+ FileUtil.getSuffix(originalFilename);
        String uploadPath = uploadPathPrefix + File.separator + uploadFileName;
        // 解析结果并返回
        File file=null;
        UploadPictureResult result = new UploadPictureResult();
        try {
            // 上传文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            result.setPicFormat( imageInfo.getFormat());
            result.setPicHeight((long) imageInfo.getHeight());
            result.setPicWidth((long) imageInfo.getWidth());
            result.setPicSize(multipartFile.getSize());
            result.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            result.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));
            result.setPicScale( (double) imageInfo.getWidth() / imageInfo.getHeight());
        } catch (Exception e) {
            log.error("file upload error, filepath = " + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTemplateFile(file);
        }

        return result;
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


}
