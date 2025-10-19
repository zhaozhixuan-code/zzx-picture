package com.zzx.zzxpicture.infrastructure.constant;

/**
 * ShardingSphere 静态常量
 */
public final class ShardingSphereConstant {

    /**
     * 私有构造器
     */
    private ShardingSphereConstant() {
    }

    /**
     * 逻辑表名称
     */
    public static final String LOGIC_TABLE_NAME = "picture";

    /**
     * 配置文件中的数据库名称
     */
    public static final String DATABASES_NAME = "logic_db";

    /**
     * 实际数据库名称
     */
    public static final String MY_DATABASES_NAME = "zzx_picture";
}
