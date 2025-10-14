package com.zzx.zzxpicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
// TODO 关闭分表
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.zzx.zzxpicturebackend.mapper") // mp扫描包
@EnableAspectJAutoProxy(exposeProxy = true)
public class ZzxPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZzxPictureBackendApplication.class, args);
    }

}
