package com.bxt.picturebackend.controller;

import com.bxt.picturebackend.annotation.AuthCheck;
import com.bxt.picturebackend.common.BaseResponse;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import com.bxt.picturebackend.constant.UserConstant;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@RestController
@RequestMapping("/file")
public class FileController {

    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @Resource
    private CosManager cosManager;


    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error");
                }
            }
        }
    }

    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    @PostMapping("/test/download")
    public void testDownloadFile(String filepath, HttpServletResponse httpServletResponse){
        COSObject cosObject= cosManager.getObject(filepath);
        COSObjectInputStream inputStream = cosObject.getObjectContent();
        try {
            byte[] bytes=IOUtils.toByteArray(inputStream);
            // 设置响应头
            httpServletResponse.setContentType("application/octet-stream");
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + cosObject.getKey() + "\"");
            // 将文件内容写入响应
            httpServletResponse.getOutputStream().write(bytes);
            httpServletResponse.getOutputStream().flush();

        } catch (IOException e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }finally {
            // 关闭输入流
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("file input stream close error, filepath = " + filepath, e);
            }
        }
    }


}
