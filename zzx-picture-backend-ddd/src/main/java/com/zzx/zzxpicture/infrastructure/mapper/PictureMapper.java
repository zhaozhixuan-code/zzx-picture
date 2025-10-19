package com.zzx.zzxpicture.infrastructure.mapper;

import com.zzx.zzxpicture.interfaces.dto.analyze.SpaceSizeAnalyzeRequest;
import com.zzx.zzxpicture.domain.picture.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.zzxpicture.interfaces.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author 28299
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-09-13 17:08:54
 * @Entity com.zzx.zzxpicture.domain.picture.entity.Picture
 */
@Mapper
public interface PictureMapper extends BaseMapper<Picture> {

    @Select("select category,count(id) as count,sum(picSize) as sumPicSize from picture group by category")
    List<SpaceCategoryAnalyzeResponse> selectCategoryStats();

    /**
     * 获取空间图片大小
     *
     * @param spaceSizeAnalyzeRequest 空间图片大小分析请求参数
     * @return
     */
    List<Long> selectSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest);

}




