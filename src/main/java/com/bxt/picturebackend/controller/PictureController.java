package com.bxt.picturebackend.controller;

import com.bxt.picturebackend.aliYunAi.CreateTaskResponse;
import com.bxt.picturebackend.aliYunAi.QueryTaskResponse;
import com.bxt.picturebackend.annotation.VIPCheck;
import com.bxt.picturebackend.common.PageRequest;
import com.bxt.picturebackend.config.RabbitMQConfig;
import com.bxt.picturebackend.imageSearch.model.ImageSearchResult;
import com.bxt.picturebackend.imageSearch.sub.GetImagePageUrlApi;
import com.google.common.hash.BloomFilter;
import cn.hutool.core.lang.copier.SrcToDestCopier;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bxt.picturebackend.annotation.AuthCheck;
import com.bxt.picturebackend.common.BaseResponse;
import com.bxt.picturebackend.common.ResultUtils;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.dto.file.UploadPictureResult;
import com.bxt.picturebackend.dto.picture.*;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.exception.ThrowUtils;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.model.enums.PictureReviewStatusEnum;
import com.bxt.picturebackend.service.PictureService;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.PictureTagCategory;
import com.bxt.picturebackend.vo.PictureVo;
import com.bxt.picturebackend.vo.UserLoginVo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.Funnels;
import com.qcloud.cos.demo.ci.CIUniversalDemo;
import com.qcloud.cos.event.DeliveryMode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.AssertNonNullIfNonNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

//bxt@bxtdeMacBook-Air picture-backend % redis-cli
//127.0.0.1:6379> ping
//PONG
//127.0.0.1:6379> CONFIG SET appendonly yes
//OK
//127.0.0.1:6379> CONFIG SET appendfsync everysec
//OK
//127.0.0.1:6379>
//在终端动态配置redis cli，AOF持久化
@RestController
@Slf4j
@RequestMapping("/picture")
public class PictureController {
    @Autowired
    private UserService userService;
    @Autowired
    private PictureService pictureService;
    @Resource
    RabbitTemplate rabbitTemplate;
    @PostMapping("/upload/file")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<PictureVo> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 @RequestPart(value = "pictureUploadRequest", required = false) PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest httpServletRequest) throws IOException {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        pictureService.isDuplicateUpload(DigestUtils.md5DigestAsHex(multipartFile.getBytes()),DigestUtils.md5DigestAsHex(loginUser.getId().toString().getBytes(StandardCharsets.UTF_8)));
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, pictureUploadRequest, userService.getById(loginUser.getId()));
        rabbitTemplate.convertAndSend(RabbitMQConfig.PICTURE_CACHE_INVALIDATE_QUEUE, pictureVo.getId());
        return ResultUtils.success(pictureVo);
    }
    @PostMapping("/upload/url")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<PictureVo> uploadPicture(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        pictureService.isDuplicateUpload(DigestUtils.md5DigestAsHex(pictureUploadRequest.getFileUrl().getBytes()),DigestUtils.md5DigestAsHex(loginUser.getId().toString().getBytes(StandardCharsets.UTF_8)));
        PictureVo pictureVo = pictureService.uploadPicture(pictureUploadRequest, userService.getById(loginUser.getId()));
        rabbitTemplate.convertAndSend(RabbitMQConfig.PICTURE_CACHE_INVALIDATE_QUEUE, pictureVo.getId());
        return ResultUtils.success(pictureVo);
    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody PictureDeleteRequest pictureDeleteRequest, HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        Long pictureId = pictureDeleteRequest.getId();
        System.out.println("pictureId"+pictureId);
        Long userId=pictureService.getById(pictureId).getUserId();
        Long loginUserId = loginUser.getId();
        if (!Objects.equals(loginUser.getUserRole(), UserConstant.ROLE_ADMIN) && !userId.equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除他人图片");
        }
        boolean result = pictureService.deletePicture(pictureId, loginUser);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PICTURE_CACHE_INVALIDATE_QUEUE, pictureId);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片删除失败");
        }
        return ResultUtils.success(result);
    }
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        Long pictureId = pictureUpdateRequest.getId();
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        if (!Objects.equals(loginUser.getUserRole(), UserConstant.ROLE_ADMIN) && !Objects.equals(picture.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改他人图片");
        }
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        picture.setEditTime(new Date());
        pictureService.validPicture(picture);
        boolean result = pictureService.updateById(picture);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片更新失败");
        }
        rabbitTemplate.convertAndSend(RabbitMQConfig.PICTURE_CACHE_INVALIDATE_QUEUE, pictureId);
        return ResultUtils.success(result);
    }
    @PostMapping("/getPictureById")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Picture> getPictureById(Long id, HttpServletRequest httpServletRequest) {
        Picture picture = pictureService.getById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        return ResultUtils.success(picture);
    }
    @PostMapping("/getPictureById/vo")
    public BaseResponse<PictureVo> getPictureVoById(Long id, HttpServletRequest httpServletRequest) {
        pictureService.checkBloomFilter(null,id,null);
        String hashKey= DigestUtils.md5DigestAsHex(id.toString().getBytes(StandardCharsets.UTF_8));
        String redisKey="picture:id:"+hashKey;
        ValueOperations<String,String> valueOperations=stringRedisTemplate.opsForValue();
        String cachedResult=valueOperations.get(redisKey);
        if (cachedResult!=null) {
            PictureVo result=JSONUtil.toBean(cachedResult,PictureVo.class);
            return ResultUtils.success(result);
        }
        Picture picture = pictureService.getById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        PictureVo pictureVo = pictureService.getPictureVo(picture,picture.getUserId());
        if (pictureVo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片信息转换失败");
        }
        String cacheValue=JSONUtil.toJsonStr(pictureVo);
        int expireTime=60*10;
        valueOperations.set(redisKey,cacheValue,expireTime,TimeUnit.SECONDS);
        return ResultUtils.success(pictureVo);
    }
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVo>> listPictureVoByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR,"分页大小不能超过20");
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePage, httpServletRequest);
        return ResultUtils.success(pictureVoPage);
    }
//
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @PostMapping("/list/page/vo/redis")
    @Deprecated
    public BaseResponse<Page<PictureVo>> listPictureVoByPageFromRedis(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR,"分页大小不能超过20");
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.APPROVED.getValue());
        String queryCondition=JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey= DigestUtils.md5DigestAsHex(queryCondition.getBytes(StandardCharsets.UTF_8));
        String redisKey="picture:query:"+hashKey;
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String cachedResult = valueOperations.get(redisKey);
        if (cachedResult != null) {
            Page<PictureVo> cachedPage = JSONUtil.toBean(cachedResult, Page.class);
            return ResultUtils.success(cachedPage);
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePage, httpServletRequest);
        String cacheValue = JSONUtil.toJsonStr(pictureVoPage);
        int cacheExpireTime = 60 * 10; // 缓存10分钟
        valueOperations.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        return ResultUtils.success(pictureVoPage);
    }

    @PostMapping("/list/page/vo/caffeine")
    @Deprecated
    public BaseResponse<Page<PictureVo>> listPictureVoByPageFromCaffeine(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR,"分页大小不能超过20");
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.APPROVED.getValue());
        String queryCondition=JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey= DigestUtils.md5DigestAsHex(queryCondition.getBytes(StandardCharsets.UTF_8));
        String key="picture:query:"+hashKey;

        String cachedResult = cache.getIfPresent(key);
        if (cachedResult != null) {
            Page<PictureVo> cachedPage = JSONUtil.toBean(cachedResult, Page.class);
            return ResultUtils.success(cachedPage);
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePage, httpServletRequest);
        String cacheValue = JSONUtil.toJsonStr(pictureVoPage);
        cache.put(key, cacheValue);
        return ResultUtils.success(pictureVoPage);
    }
    private Cache<String,String> cache = Caffeine.newBuilder().maximumSize(1000) // 最大缓存条数
            .expireAfterWrite(10, TimeUnit.MINUTES) // 写入后10分钟过期
            .build();
    //多级缓存的缺点是首次查询会比较慢，但如果查找缓存中的数据，会很快，只需要二分之一到三分之一的时间（和用自带的缓存相比）
    @PostMapping("/list/page/vo/multiLevelCache")
    public BaseResponse<Page<PictureVo>> listPictureVoByPageFromMultiLevelCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR,"分页大小不能超过20");
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        pictureService.checkBloomFilter(pictureQueryRequest.getUserId(),pictureQueryRequest.getId(),null);
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.APPROVED.getValue());
        String queryCondition=JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey= DigestUtils.md5DigestAsHex(queryCondition.getBytes(StandardCharsets.UTF_8));
        String key="picture:query:"+hashKey;
        // 1. 先从 Caffeine 缓存中获取
        Random random=new Random();

        String cachedResult = cache.getIfPresent(key);
        if (cachedResult != null) {
            Page<PictureVo> cachedPage = JSONUtil.toBean(cachedResult, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2. 如果 Caffeine 缓存中没有，再从 Redis 中获取
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String redisCachedResult = valueOperations.get(key);
        if (redisCachedResult != null) {
            Page<PictureVo> cachedPage = JSONUtil.toBean(redisCachedResult, Page.class);
            // 同时将数据写入 Caffeine 缓存
            cache.put(key, redisCachedResult);
            return ResultUtils.success(cachedPage);
        }
        // 3. 如果 Redis 中也没有，则从数据库查询
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePage, httpServletRequest);
        String cacheValue = JSONUtil.toJsonStr(pictureVoPage);
        // 将查询结果写入 Redis 和 Caffeine 缓存
        // 即使查询结果为null也会写入
        int cacheExpireTime = 300+random.nextInt(300); // 缓存随机分钟
        // 缓存随机分钟的原因是为了避免缓存雪崩
        valueOperations.set(key, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        cache.put(key, cacheValue);

        return ResultUtils.success(pictureVoPage);

    }
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTags(tagList);
        pictureTagCategory.setCategories(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }
    @PostMapping("/reviewPicture")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest httpServletRequest) {
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        if (!Objects.equals(loginUser.getUserRole(), UserConstant.ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限审核图片");
        }
        boolean result = pictureService.doPictureReview(pictureReviewRequest, loginUser.getId());
        return ResultUtils.success(result);
    }
    @PostMapping("/picture/autoFetch")
    @AuthCheck(mustRole = UserConstant.ROLE_ADMIN)
    public BaseResponse<List<PictureVo>> autoFetchPictures(@RequestParam String keyword,
                                                           @RequestParam(defaultValue = "10") int count,
                                                           @RequestParam(defaultValue = "admin/fetch") String uploadPathPrefix,HttpServletRequest httpServletRequest) {
        List<String> imgUrls = pictureService.getImageUrlsFromBaidu(keyword, count);

        List<PictureVo> results = new ArrayList<>();
        UserLoginVo loginUser = userService.getCurrentUser(httpServletRequest);
        pictureService.isDuplicateUpload(DigestUtils.md5DigestAsHex(keyword.getBytes(StandardCharsets.UTF_8)),DigestUtils.md5DigestAsHex(loginUser.getId().toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("img"+imgUrls);
        for (String imgUrl : imgUrls) {
            System.out.println(imgUrl);
            try{
                PictureUploadRequest pictureUploadRequest=new PictureUploadRequest();
                pictureUploadRequest.setFileUrl(imgUrl);
                PictureVo uploadPictureResult=pictureService.uploadPicture(pictureUploadRequest, userService.getById(loginUser.getId()));
            }catch (Exception e){
                log.error("图片上传失败,图片地址:{}",imgUrl,e);
            }
        }
        return ResultUtils.success(results);
    }

    @PostMapping("/downloadPicture/watermarking/blind")
    public void downloadPictureBlindWaterMarking(@RequestBody PictureDownloadRequest pictureDownloadRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        UserLoginVo userLoginVo=userService.getCurrentUser(httpServletRequest);
        try {
            pictureService.downloadPictureBlindWatermarking(pictureDownloadRequest,userLoginVo,httpServletResponse);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    @PostMapping("/downloadPicture/watermarking/word")
    public void downloadPictureWordWaterMarking(@RequestBody PictureDownloadRequest pictureDownloadRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        UserLoginVo userLoginVo=userService.getCurrentUser(httpServletRequest);
        try {
            pictureService.downloadPictureWordWatermarking(pictureDownloadRequest,userLoginVo,httpServletResponse);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    @VIPCheck(mustVIP = UserConstant.USER_VIP_YES)
    @PostMapping("/searchSimilarPictures")
    public BaseResponse<List<String>> searchSimilarPictures(@RequestParam Long pictureId, HttpServletRequest httpServletRequest) {
        UserLoginVo userLoginVo = userService.getCurrentUser(httpServletRequest);
        if (userLoginVo == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        List<String> results = GetImagePageUrlApi.getUrlList(pictureService.getById(pictureId).getUrl());
        return ResultUtils.success(results);
    }
    @PostMapping("/getPictureMainColor")
    public BaseResponse<String> getPictureMainColor(@RequestParam Long pictureId, HttpServletRequest httpServletRequest) {
        UserLoginVo userLoginVo = userService.getCurrentUser(httpServletRequest);
        if (userLoginVo == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        String url = pictureService.getById(pictureId).getUrl();
        if (url == null || url.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片URL不能为空");
        }
        String mainColor = pictureService.getPictureMainColor(url);
        return ResultUtils.success(mainColor);
    }
    @PostMapping("/getPictureSimilarMainColor")
    public BaseResponse<Page<PictureVo>> getPictureSimilarMainColor(@RequestParam Long pictureId, @RequestBody PageRequest pageRequest, HttpServletRequest httpServletRequest) {
        UserLoginVo userLoginVo = userService.getCurrentUser(httpServletRequest);
        if (userLoginVo == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        Page<PictureVo> similarPictures = pictureService.getPictureSimilarMainColor(picture.getPicColor(), pageRequest);
        return ResultUtils.success(similarPictures);
    }
    /**
     * Step1: 创建扩图任务
     */
    @PostMapping("/outPainting")
    @VIPCheck(mustVIP = UserConstant.USER_VIP_YES)
    public BaseResponse<CreateTaskResponse> createOutPaintingTask(@RequestParam Long pictureId, HttpServletRequest httpServletRequest) throws Exception {
        UserLoginVo userLoginVo = userService.getCurrentUser(httpServletRequest);
        if (userLoginVo == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        return ResultUtils.success(pictureService.createOutPaintingTask(picture.getUrl()));
    }

    /**
     * Step2: 查询扩图任务
     */
    @GetMapping("/outPainting/{taskId}")
    @VIPCheck(mustVIP = UserConstant.USER_VIP_YES)
    public BaseResponse<QueryTaskResponse> queryOutPaintingTask(@PathVariable String taskId) {
        return ResultUtils.success(pictureService.queryOutPaintingTask(taskId));
    }






}
