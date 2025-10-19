package com.zzx.zzxpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.zzxpicture.domain.user.entity.User;
import com.zzx.zzxpicture.domain.user.repository.UserRepository;
import com.zzx.zzxpicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户仓库实现类
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User>
        implements UserRepository {
}
