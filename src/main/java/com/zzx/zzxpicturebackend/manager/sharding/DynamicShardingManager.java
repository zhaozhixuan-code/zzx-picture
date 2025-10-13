package com.zzx.zzxpicturebackend.manager.sharding;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.zzx.zzxpicturebackend.constant.ShardingSphereConstant;
import com.zzx.zzxpicturebackend.exception.BusinessException;
import com.zzx.zzxpicturebackend.exception.ErrorCode;
import com.zzx.zzxpicturebackend.model.enums.SpaceLevelEnum;
import com.zzx.zzxpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.zzxpicturebackend.model.po.Space;
import com.zzx.zzxpicturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.route.strategy.ShardingStrategy;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态分表管理
 * 首次加载项目时需要更新分表配置
 */
@Component
@Slf4j
public class DynamicShardingManager {

    @Resource
    private DataSource dataSource;

    @Resource
    private SpaceService spaceService;


    /**
     * 初始化动态分表
     */
    @PostConstruct // 当这个bean 创建成功之后，会执行这个方法
    public void initialize() {
        log.info("初始化动态分表配置");
        updateShardingTableNodes();
    }

    /**
     * 更新 ShardingSphere 的 actual-data-nodes 动态表名配置
     */
    private void updateShardingTableNodes() {
        // 现获取所有的表名
        Set<String> tableNames = fetchAllTableNames();
        // zzx_picture.picture_53613631563,zzx_picture.picture_436434847934,zzx_picture.picture_48348934873934,
        String newActualDataNodes = tableNames.stream()
                .map(tableName -> ShardingSphereConstant.MY_DATABASES_NAME + "." + tableName) // 将表名转换为逻辑表名
                .collect(Collectors.joining(","));
        log.info("动态分表 actual-data-nodes 配置为：{}", newActualDataNodes);
        ContextManager contextManager = getContextManager();
        // 获取 ShardingSphere 的规则元数据
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()
                .getDatabases()
                .get(ShardingSphereConstant.DATABASES_NAME)
                .getRuleMetaData();
        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
        // 如果存在，则更新
        if (shardingRule.isPresent()) {
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
            List<ShardingTableRuleConfiguration> updateRules = ruleConfig.getTables()
                    .stream()
                    .map(oldTableRule -> {
                        if (ShardingSphereConstant.LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                            // 设置新的表配置
                            ShardingTableRuleConfiguration newTableRuleConfig = new ShardingTableRuleConfiguration(ShardingSphereConstant.LOGIC_TABLE_NAME, newActualDataNodes);
                            newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                            newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                            newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                            newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
                            return newTableRuleConfig;
                        }
                        return oldTableRule;
                    })
                    .collect(Collectors.toList());
            // 更新配置
            ruleConfig.setTables(updateRules);
            // 更新规则
            contextManager.alterRuleConfiguration(ShardingSphereConstant.DATABASES_NAME, Collections.singleton(ruleConfig));
            // 重新加载数据库
            contextManager.reloadDatabase(ShardingSphereConstant.DATABASES_NAME);
            log.info("动态分表配置更新成功");
        } else {
            log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败");
        }
    }

    /**
     * 动态创建一张分表
     *
     * @param space 团队空间
     */
    public void createSpacePictureTable(Space space) {
        // 仅为旗舰版团队空间创建分表
        if (space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue()) && space.getSpaceLevel() == SpaceLevelEnum.FLAGSHIP.getValue()) {
            Long spaceId = space.getId();
            String tableName = ShardingSphereConstant.LOGIC_TABLE_NAME + "_" + spaceId;
            // 创建新表
            String createTableSql = "CREATE TABLE " + tableName + " LIKE " + ShardingSphereConstant.LOGIC_TABLE_NAME;
            try {
                SqlRunner.db().update(createTableSql);
                // 更新分表
                updateShardingTableNodes();
            } catch (Exception e) {
                log.error("创建分表失败，空间 id = {}", spaceId);
                e.printStackTrace();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败，空间 id = " + spaceId);
            }
        }
    }

    /**
     * 获取 ShardingSphere 上下文管理器
     */
    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (SQLException e) {
            throw new RuntimeException("获取 ShardingSphere 上下文管理器失败", e);
        }
    }

    /**
     * 获取所有的表名，包括初始表 picture 和 分表 picture_{spaceId}
     *
     * @return 所有的表名
     */
    private Set<String> fetchAllTableNames() {
        // 查询对旗舰版的团队空间分表
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue());
        // 为了方便测试，目前只对团队空间进行动态分表，实际上线可取消注释
        queryWrapper.eq(Space::getSpaceLevel, SpaceLevelEnum.FLAGSHIP.getValue());
        Set<Long> spaceIds = spaceService.list(queryWrapper).stream()
                .map(Space::getId)
                .collect(Collectors.toSet());
        // 获取所有的表名
        Set<String> tableNames = spaceIds.stream()
                .map(spaceId -> ShardingSphereConstant.LOGIC_TABLE_NAME + "_" + spaceId)
                .collect(Collectors.toSet());
        // 添加初始表
        tableNames.add(ShardingSphereConstant.LOGIC_TABLE_NAME);
        return tableNames;
    }


}