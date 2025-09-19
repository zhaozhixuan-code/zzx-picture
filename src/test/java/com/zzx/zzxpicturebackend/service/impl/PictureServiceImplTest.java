package com.zzx.zzxpicturebackend.service.impl;

import com.zzx.zzxpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.zzx.zzxpicturebackend.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PictureServiceImplTest {
    @Resource
    private PictureService pictureService;

    @Test
    void uploadPictureByBatch() {
        PictureUploadByBatchRequest pictureUploadByBatchRequest = new PictureUploadByBatchRequest();
        pictureUploadByBatchRequest.setCount(5);
        pictureUploadByBatchRequest.setSearchText("动漫");
        Integer i = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, null);
        System.out.println(i);
    }
}