package com.zzx.zzxpicture.interfaces.controller;


import com.zzx.zzxpicture.infrastructure.common.BaseResponse;
import com.zzx.zzxpicture.infrastructure.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 */
@RestController
@RequestMapping("/")
public class MainController {


    /**
     * 健康检查接口
     *
     * @return BaseResponse<String> 健康检查结果，成功时返回"ok"
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

}
