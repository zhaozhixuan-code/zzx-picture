package com.zzx.zzxpicturebackend.service.impl;

import com.zzx.zzxpicturebackend.model.dto.analyze.SpaceSizeAnalyzeRequest;
import com.zzx.zzxpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.zzx.zzxpicturebackend.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import java.util.List;

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

    @Test
    void getSpaceSizeAnalyze() {
        SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest = new SpaceSizeAnalyzeRequest();
        // spaceSizeAnalyzeRequest.setSpaceId();
        // spaceSizeAnalyzeRequest.setQueryPublic();
        spaceSizeAnalyzeRequest.setQueryAll(true);
        List<Long> spaceSizeAnalyze = pictureService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest);
        System.out.println(spaceSizeAnalyze);
    }
}