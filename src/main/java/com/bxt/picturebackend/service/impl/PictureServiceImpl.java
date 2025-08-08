package com.bxt.picturebackend.service.impl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bxt.picturebackend.config.CosClientConfig;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.dto.picture.PictureDownloadRequest;
import com.bxt.picturebackend.dto.picture.PictureQueryRequest;
import com.bxt.picturebackend.dto.picture.PictureReviewRequest;
import com.bxt.picturebackend.dto.picture.PictureUploadRequest;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.exception.ThrowUtils;
import com.bxt.picturebackend.manager.CosManager;
import com.bxt.picturebackend.manager.FileManager;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.entity.Picturedownload;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.model.enums.UserRoleEnum;
import com.bxt.picturebackend.service.PictureService;
import com.bxt.picturebackend.mapper.PictureMapper;
import com.bxt.picturebackend.service.PicturedownloadService;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.PictureVo;
import com.bxt.picturebackend.vo.UserLoginVo;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
* @author bxt
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-07-21 17:11:40
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{
    @Autowired
    private FileManager fileManager;
    @Autowired
    private UserService userService;
    @Autowired
    private ProjectInfoAutoConfiguration projectInfoAutoConfiguration;
    @Autowired
    private CosManager cosManager;
    @Override
    public PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (loginUser==null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        Long pictureId=null;
        if (pictureUploadRequest!=null &&pictureUploadRequest.getId()!=null){
            pictureId = pictureUploadRequest.getId();
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            if (!exists) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
        }
        // 上传图片，得到信息
        String picturePrefix = "public/"+loginUser.getId().toString();
        UploadPictureResult uploadResult = fileManager.uploadPicture(multipartFile, picturePrefix);
        if (uploadResult == null || uploadResult.getUrl() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
        // 创建或更新图片记录
        Picture picture = new Picture();
        picture.setUrl(uploadResult.getUrl());
        picture.setName(uploadResult.getPicName());
        picture.setIntroduction("");
        picture.setCategory("");
        picture.setTags("");
        picture.setPicSize(uploadResult.getPicSize());
        picture.setPicWidth(Math.toIntExact(uploadResult.getPicWidth()));
        picture.setPicHeight(Math.toIntExact(uploadResult.getPicHeight()));
        picture.setPicScale(uploadResult.getPicScale());
        picture.setPicFormat(uploadResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setEditTime(new Date());
        picture.setThumbnailUrl(uploadResult.getThumbnailUrl());
        if (pictureId != null) {
            picture.setId(pictureId);
            boolean updateResult = this.updateById(picture);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片更新失败");
            }
        } else {
            boolean saveResult = this.save(picture);
            System.out.println("保存图片结果: " + saveResult);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
            }
        }
        System.out.println("上传图片成功，图片ID: " + picture.getId());

        return PictureVo.objToVo(picture);
    }
    @Override
    public PictureVo uploadPicture(PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (loginUser==null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        Long pictureId=null;
        if (pictureUploadRequest.getId()!=null){
            pictureId = pictureUploadRequest.getId();
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            if (!exists) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
        }
        // 上传图片，得到信息
        String picturePrefix = "public/"+loginUser.getId().toString();
        System.out.println(pictureUploadRequest);
        UploadPictureResult uploadResult = fileManager.uploadPicture(pictureUploadRequest.getFileUrl(), picturePrefix);
        if (uploadResult == null || uploadResult.getUrl() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
        // 创建或更新图片记录
        Picture picture = new Picture();
        picture.setUrl(uploadResult.getUrl());
        picture.setName(uploadResult.getPicName());
        picture.setIntroduction("");
        picture.setCategory("");
        picture.setTags("");
        picture.setPicSize(uploadResult.getPicSize());
        picture.setPicWidth(Math.toIntExact(uploadResult.getPicWidth()));
        picture.setPicHeight(Math.toIntExact(uploadResult.getPicHeight()));
        picture.setPicScale(uploadResult.getPicScale());
        picture.setPicFormat(uploadResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setEditTime(new Date());
        picture.setThumbnailUrl(uploadResult.getThumbnailUrl());
        if (pictureId != null) {
            picture.setId(pictureId);
            boolean updateResult = this.updateById(picture);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片更新失败");
            }
        } else {
            boolean saveResult = this.save(picture);
            System.out.println("保存图片结果: " + saveResult);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
            }
        }
        System.out.println("上传图片成功，图片ID: " + picture.getId());

        return PictureVo.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest){
        QueryWrapper<Picture> queryWrapper=new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id= pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        String searchText = pictureQueryRequest.getSearchText();
        if (StrUtil.isNotBlank(searchText)){
            queryWrapper.nested(qw ->
                            qw.like("introduction", searchText)
                                    .or()
                                    .like("name", searchText)
                    );

        }
        if (id != null) {
            System.out.println("查询图片ID: " + id);
            queryWrapper.eq("id", id);
        }
        if (StrUtil.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }
        if (StrUtil.isNotBlank(introduction)) {
            queryWrapper.like("introduction", introduction);
        }
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }
        if (picSize != null) {
            queryWrapper.eq("picSize", picSize);
        }
        if (picWidth != null) {
            queryWrapper.eq("picWidth", picWidth);
        }
        if (picHeight != null) {
            queryWrapper.eq("picHeight", picHeight);
        }
        if (picScale != null) {
            queryWrapper.eq("picScale", picScale);
        }
        if (StrUtil.isNotBlank(picFormat)) {
            queryWrapper.eq("picFormat", picFormat);
        }
        for (String tag : tags) {
            if (StrUtil.isNotBlank(tag)) {
                queryWrapper.like("tags", tag);
            }
        }
        return queryWrapper;
    }
    @Override
    public PictureVo getPictureVo(Picture picture, HttpServletRequest request){
        UserLoginVo userLoginVo=userService.getCurrentUser(request);
        PictureVo pictureVo = PictureVo.objToVo(picture);
        if (userLoginVo != null) {
            pictureVo.setUserLoginVo(userLoginVo);
        }
        return pictureVo;
    }
    @Override
    public PictureVo getPictureVo(Picture picture, Long userId) {
        User user= userService.getById(userId);
        UserLoginVo userLoginVo = new UserLoginVo();
        BeanUtils.copyProperties(user, userLoginVo);
        PictureVo pictureVo = PictureVo.objToVo(picture);
        if (userLoginVo != null) {
            pictureVo.setUserLoginVo(userLoginVo);
        }
        return pictureVo;
    }
    @Override
    public Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        List<PictureVo> pictureVoList = pictureList.stream()
                .map(picture -> getPictureVo(picture, picture.getUserId()))
                .toList();
        Page<PictureVo> pictureVoPage = new Page<>();
        pictureVoPage.setRecords(pictureVoList);
        pictureVoPage.setCurrent(picturePage.getCurrent());
        pictureVoPage.setSize(picturePage.getSize());
        pictureVoPage.setTotal(picturePage.getTotal());
        pictureVoPage.setPages(picturePage.getPages());
        return pictureVoPage;
    }
    @Override
    public void validPicture(Picture picture) {
        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        Long id = picture.getId();
        String url= picture.getUrl();
        String name = picture.getName();
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        if (StrUtil.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }

    }
    @Override
    public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, Long reviewerId){
        Picture picture = this.getById(pictureReviewRequest.getId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        picture.setReviewReason(pictureReviewRequest.getReviewReason());
        picture.setReviewStatus(pictureReviewRequest.getStatus());
        picture.setReviewUserId(reviewerId);
        picture.setReviewTime(new Date());
        boolean updateResult = this.updateById(picture);
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "图片审核失败");
        return updateResult;
    }
    @Override
    public List<UploadPictureResult> fetchPicturesFromBaidu(String keyword, int count, String uploadPathPrefix) {
        List<String> imageUrls = new ArrayList<>();

        int pageSize = 30;
        int pages = (int) Math.ceil((double) count / pageSize);
        for (int page = 0; page < pages; page++) {
            int pn = page * pageSize;
            String url = StrUtil.format("https://image.baidu.com/search/acjson?tn=result&word={}&pn={}&rn={}",
                    URLUtil.encode(keyword), pn, pageSize);

            try {
                String response = HttpUtil.get(url);
                System.out.println(url);
//                System.out.println(response);
                String cleaned = HtmlUtil.unescape(response);
                JSONObject json = JSONUtil.parseObj(cleaned);
                JSONArray data = json.getJSONArray("data");
                System.out.println("data:"+data);
                if (data != null) {
                    for (int i = 0; i < data.size() && imageUrls.size() < count; i++) {
                        JSONObject item = data.getJSONObject(i);
                        String imageUrl = item.getStr("thumbURL");
                        if (StrUtil.isNotBlank(imageUrl)) {
                            imageUrls.add(imageUrl);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("抓取百度图片失败", e);
            }
        }

        // 上传到 COS
        List<UploadPictureResult> resultList = new ArrayList<>();
        for (String imgUrl : imageUrls) {
            try {
                UploadPictureResult result = fileManager.uploadPicture(imgUrl, uploadPathPrefix);
                resultList.add(result);
            } catch (Exception e) {
                log.warn("上传失败");
            }
        }

        return resultList;
    }
    @Override
    public List<String> getImageUrlsFromBaidu(String keyword, int count) {
        List<String> imageUrls = new ArrayList<>();
        int pageSize = 30;
        int pages = (int) Math.ceil((double) count / pageSize);

        for (int page = 0; page < pages; page++) {
            int pn = page * pageSize;
            String apiUrl = String.format("https://image.baidu.com/search/acjson?tn=result&word=%s&pn=%d&rn=%d",
                    URLUtil.encode(keyword), pn, pageSize);

            try {
                HttpResponse response = HttpRequest.get(apiUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/114.0.0.0 Safari/537.36")
                        .timeout(5000)
                        .execute();

                String body = response.body();

                // 正则提取 thumbURL
                Pattern pattern = Pattern.compile("\"thumbURL\":\"(.*?)\"");
                Matcher matcher = pattern.matcher(body);

                while (matcher.find() && imageUrls.size() < count) {
                    String imgUrl = matcher.group(1).replaceAll("\\\\", "");
                    imageUrls.add(imgUrl);
                }

            } catch (Exception e) {
                log.error("抓取百度图片失败: {}", apiUrl, e);
            }

            // 如果已经抓够了
            if (imageUrls.size() >= count) {
                break;
            }
        }

        return imageUrls;
    }
    private String getKeyFromUrl(String url) {
        String domain = "https://bxttttt-1321961985.cos.ap-shanghai.myqcloud.com/";
        if (url != null && url.startsWith(domain)) {
            return url.substring(domain.length());
        }
        return url;
    }

    @Override
    public boolean deletePicture(Long pictureId, UserLoginVo loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        if (!loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue())&&!picture.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权限删除该图片");
        }

        boolean deleteResult = this.removeById(pictureId);
        String key= getKeyFromUrl(picture.getUrl());
        cosManager.deletePictureObject(key);
        if (picture.getThumbnailUrl() != null) {
            cosManager.deletePictureObject(getKeyFromUrl(picture.getThumbnailUrl()));
        }
        if (!deleteResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片删除失败");
        }
        return deleteResult;
    }
    @Autowired
    private PicturedownloadService picturedownloadService;

    @Override
    public void downloadPictureBlindWatermarking(PictureDownloadRequest pictureDownloadRequest,
                                                 UserLoginVo userLoginVo,
                                                 HttpServletResponse httpServletResponse) {
        if (pictureDownloadRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求信息为空");
        }
        if (userLoginVo == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Long pictureId = pictureDownloadRequest.getPictureId();
        Long userId = userLoginVo.getId();
        String picUrl = pictureDownloadRequest.getPicUrl();

        String key = getKey(picUrl);
        System.out.println("pictureId=" + pictureId);
        System.out.println("userId=" + userId);
        System.out.println("key=" + key);

        // 从 key 中获取文件名和后缀
        String fileName = key.substring(key.lastIndexOf("/") + 1);
        String fileExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : "jpg";
        String contentType = "image/" + ("jpg".equalsIgnoreCase(fileExt) ? "jpeg" : fileExt);

        // 下载 COS 对象（加盲水印）
        COSObject cosObject = cosManager.downloadPictureToFileWithBlindWatermark(key);
        try (COSObjectInputStream inputStream = cosObject.getObjectContent()) {
            byte[] bytes = IOUtils.toByteArray(inputStream);

            // 设置响应头
            httpServletResponse.setContentType(contentType);
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpServletResponse.getOutputStream().write(bytes);
            httpServletResponse.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error, filepath = " + picUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }

        // 保存下载记录
        Picturedownload picturedownload = new Picturedownload();
        picturedownload.setPictureId(pictureId);
        picturedownload.setUserId(userId);
        picturedownloadService.save(picturedownload);
    }
    @Override
    public void downloadPictureWordWatermarking(PictureDownloadRequest pictureDownloadRequest,
                                                UserLoginVo userLoginVo,
                                                HttpServletResponse httpServletResponse) {
        if (pictureDownloadRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求信息为空");
        }
        if (userLoginVo == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Long pictureId = pictureDownloadRequest.getPictureId();
        Long userId = userLoginVo.getId();
        String picUrl = pictureDownloadRequest.getPicUrl();

        String key = getKey(picUrl);
        System.out.println("pictureId=" + pictureId);
        System.out.println("userId=" + userId);
        System.out.println("key=" + key);

        // 从 key 中获取文件名和后缀
        String fileName = key.substring(key.lastIndexOf("/") + 1);
        String fileExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : "jpg";
        String contentType = "image/" + ("jpg".equalsIgnoreCase(fileExt) ? "jpeg" : fileExt);

        // 下载 COS 对象（加盲水印）
        COSObject cosObject = cosManager.downloadPictureToFileWithWordWatermark(key);
        try (COSObjectInputStream inputStream = cosObject.getObjectContent()) {
            byte[] bytes = IOUtils.toByteArray(inputStream);

            // 设置响应头
            httpServletResponse.setContentType(contentType);
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpServletResponse.getOutputStream().write(bytes);
            httpServletResponse.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error, filepath = " + picUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }

        // 保存下载记录
        Picturedownload picturedownload = new Picturedownload();
        picturedownload.setPictureId(pictureId);
        picturedownload.setUserId(userId);
        picturedownloadService.save(picturedownload);
    }
    @Autowired
    private CosClientConfig cosClientConfig;
    private String getKey(String url) {
        String host = cosClientConfig.getHost();
        if (url != null && url.startsWith(host)) {
            return url.substring(host.length());
        }
        return url; // 如果不是COS域名开头，就原样返回或抛异常
    }








}








