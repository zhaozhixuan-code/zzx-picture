package com.zzx.zzxpicturebackend.mapper;

import com.zzx.zzxpicturebackend.model.po.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.zzxpicturebackend.model.vo.analyze.SpaceCategoryAnalyzeResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author 28299
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-09-13 17:08:54
 * @Entity com.zzx.zzxpicturebackend.model.po.Picture
 */
@Mapper
public interface PictureMapper extends BaseMapper<Picture> {

    @Select("select category,count(id) as count,sum(picSize) as sumPicSize from picture group by category")
    List<SpaceCategoryAnalyzeResponse> selectCategoryStats();

}




