package com.zzx.zzxpicture;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication()
@MapperScan("com.zzx.zzxpicture.infrastructure.mapper") // mp扫描包
@EnableAspectJAutoProxy(exposeProxy = true)
public class ZzxPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZzxPictureBackendApplication.class, args);
    }

}
