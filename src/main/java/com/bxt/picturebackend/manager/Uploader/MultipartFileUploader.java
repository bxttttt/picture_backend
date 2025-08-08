package com.bxt.picturebackend.manager.Uploader;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@Component
public class MultipartFileUploader extends AbstractPictureUploader<MultipartFile>{
    @PostConstruct
    public void init() {
        System.out.println("cosManager = " + cosManager);
    }

    @Override
    public String getOriginFileName(MultipartFile source) {
        return source.getOriginalFilename();
    }

    @Override
    public File getFileFromSource(MultipartFile source, String uploadPath) throws IOException {
        File file=null;
        file=File.createTempFile(uploadPath,null);
        source.transferTo(file);
        return file;
    }

    @Override
    protected String generateUploadPath(String originFileName, String uploadPathPrefix) {
        String uuid= RandomUtil.randomString(16);
        String uploadFileName = (DateUtil.formatDate(new Date()))+"_" +uuid +"."+ FileUtil.getSuffix(originFileName);
        return uploadPathPrefix + File.separator + uploadFileName;
    }

    @Override
    protected void validPicture(MultipartFile source) {
        if (source == null || source.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的文件不能为空");
        }
        String contentType = source.getContentType();
//        if (contentType == null || !contentType.startsWith("image/")) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的文件不是图片类型");
//        }
        long filesize = source.getSize();
        if (filesize <= 0 || filesize > 5 * 1024 * 1024) { // 限制图片大小为5MB
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的图片大小不符合要求");
        }
        String suffix=FileUtil.getSuffix(source.getOriginalFilename());
        if (!"jpg".equalsIgnoreCase(suffix) && !"jpeg".equalsIgnoreCase(suffix) &&
                !"png".equalsIgnoreCase(suffix) && !"gif".equalsIgnoreCase(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的图片格式不支持");
        }
    }
}
