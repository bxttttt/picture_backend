package com.bxt.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.dto.picture.PictureDownloadRequest;
import com.bxt.picturebackend.dto.picture.PictureQueryRequest;
import com.bxt.picturebackend.dto.picture.PictureReviewRequest;
import com.bxt.picturebackend.dto.picture.PictureUploadRequest;
import com.bxt.picturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.vo.PictureVo;
import com.bxt.picturebackend.vo.UserLoginVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
* @author bxt
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-07-21 17:11:40
*/
public interface PictureService extends IService<Picture> {

    PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    PictureVo uploadPicture(PictureUploadRequest pictureUploadRequest, User loginUser);

    void checkBloomFilter(Long userId, Long pictureId, String userAccount);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVo getPictureVo(Picture picture, HttpServletRequest request);

    PictureVo getPictureVo(Picture picture, Long userId);

    Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);

    boolean doPictureReview(PictureReviewRequest pictureReviewRequest, Long reviewerId);

    List<UploadPictureResult> fetchPicturesFromBaidu(String keyword, int count, String uploadPathPrefix);

    List<String> getImageUrlsFromBaidu(String keyword, int count);

    boolean deletePicture(Long pictureId, UserLoginVo loginUser);

    void downloadPictureBlindWatermarking(PictureDownloadRequest pictureDownloadRequest, UserLoginVo userLoginVo, HttpServletResponse httpServletResponse);

    void downloadPictureWordWatermarking(PictureDownloadRequest pictureDownloadRequest,
                                         UserLoginVo userLoginVo,
                                         HttpServletResponse httpServletResponse);

    void isDuplicateUpload(String fileMd, String userIdMd);

}
