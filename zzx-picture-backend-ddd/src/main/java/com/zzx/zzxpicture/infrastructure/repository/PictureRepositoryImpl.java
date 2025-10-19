package com.zzx.zzxpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicture.domain.picture.entity.Picture;
import com.zzx.zzxpicture.domain.picture.repository.PictureRepository;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.domain.user.repository.UserRepository;
import com.zzx.zzxpicture.infrastructure.mapper.PictureMapper;
import com.zzx.zzxpicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 图片仓库实现类
 */
@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureRepository {
}
