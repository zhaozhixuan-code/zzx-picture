package com.zzx.zzxpicture.infrastructure.api.imagesearch;

import com.zzx.zzxpicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.zzx.zzxpicture.infrastructure.api.imagesearch.sub.GetImageFirstUrlApi;
import com.zzx.zzxpicture.infrastructure.api.imagesearch.sub.GetImageListApi;
import com.zzx.zzxpicture.infrastructure.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 以图搜图接口的 API
 */
@Slf4j
public class ImageSearchApiFacade {

    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        String imageUrl = "https://zzx-picture-1349365238.cos.ap-beijing.myqcloud.com//public/1965419228446646274/2025-09-18_0grGg6wu5GvtvHHD.jpg";
        List<ImageSearchResult> imageSearchResults = searchImage(imageUrl);
        System.out.println("搜索成功" + imageSearchResults);
    }
}
