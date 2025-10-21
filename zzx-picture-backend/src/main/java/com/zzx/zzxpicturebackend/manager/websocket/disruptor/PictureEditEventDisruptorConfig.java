package com.zzx.zzxpicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

/**
 * disruptor配置类
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    /**
     * 创建并配置图片编辑事件的Disruptor实例
     *
     * @return 配置好的Disruptor<PictureEditEvent>实例
     */
    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> disruptorModel() {
        // 设置环形缓冲区大小为262,144个事件槽位
        int bufferSize = 1024 * 256;

        // 创建Disruptor实例，使用PictureEditEvent工厂和自定义线程工厂
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,  // 事件工厂，用于创建新的PictureEditEvent实例
                bufferSize,             // 环形缓冲区大小
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()  // 线程工厂，设置线程名称前缀
        );

        // 设置事件消费者，使用工作池模式处理图片编辑事件
        // 工作池模式允许多个工作者线程并行处理事件，提高并发性能
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);

        // 启动disruptor，开始监听和处理事件
        disruptor.start();

        return disruptor;
    }

}

