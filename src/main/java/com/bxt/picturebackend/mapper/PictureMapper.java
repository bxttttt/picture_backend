package com.bxt.picturebackend.mapper;

import com.bxt.picturebackend.dto.picture.UserUploadCountDTO;
import com.bxt.picturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author bxt
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2025-07-21 17:11:40
* @Entity com.bxt.picturebackend.model.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 按 id + userId 更新 name/introduction/editTime（分库分表路由需要 userId）
     */
    int updateNameAndIntroductionByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("introduction") String introduction,
            @Param("editTime") Date editTime);
//    @Select("""
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_0 WHERE is_deleted = 0 GROUP BY user_id
//        UNION ALL
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_1 WHERE is_deleted = 0 GROUP BY user_id
//        UNION ALL
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_2 WHERE is_deleted = 0 GROUP BY user_id
//        UNION ALL
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_3 WHERE is_deleted = 0 GROUP BY user_id
//        UNION ALL
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_4 WHERE is_deleted = 0 GROUP BY user_id
//        UNION ALL
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_5 WHERE is_deleted = 0 GROUP BY user_id
//        UNION ALL
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_6 WHERE is_deleted = 0 GROUP BY user_id
//        UNION ALL
//        SELECT user_id AS userId, COUNT(*) AS uploadCount FROM picture_7 WHERE is_deleted = 0 GROUP BY user_id
//    """)
//    List<UserUploadCountDTO> countByUserId();

}




