package com.zzx.zzxpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicture.domain.space.entity.SpaceUser;
import com.zzx.zzxpicture.domain.space.repository.SpaceUserRepository;
import com.zzx.zzxpicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;


@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
