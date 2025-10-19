package com.zzx.zzxpicturebackend.service.impl;

import com.zzx.zzxpicture.interfaces.dto.analyze.SpaceSizeAnalyzeRequest;
import com.zzx.zzxpicture.interfaces.dto.picture.PictureUploadByBatchRequest;
import com.zzx.zzxpicture.application.service.PictureApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.List;

@SpringBootTest
class PictureDomainServiceImplTest {
    @Resource
    private PictureApplicationService pictureApplicationService;

    @Test
    void uploadPictureByBatch() {
        PictureUploadByBatchRequest pictureUploadByBatchRequest = new PictureUploadByBatchRequest();
        pictureUploadByBatchRequest.setCount(5);
        pictureUploadByBatchRequest.setSearchText("动漫");
        Integer i = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, null);
        System.out.println(i);
    }

    @Test
    void getSpaceSizeAnalyze() {
        SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest = new SpaceSizeAnalyzeRequest();
        // spaceSizeAnalyzeRequest.setSpaceId();
        // spaceSizeAnalyzeRequest.setQueryPublic();
        spaceSizeAnalyzeRequest.setQueryAll(true);
        List<Long> spaceSizeAnalyze = pictureApplicationService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest);
        System.out.println(spaceSizeAnalyze);
    }
}