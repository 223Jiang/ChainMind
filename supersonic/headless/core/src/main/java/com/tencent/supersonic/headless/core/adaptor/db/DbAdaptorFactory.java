package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.enums.EngineType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author JiangWeiWei
 *
 *         数据库适配器工厂类 用于根据不同的数据库类型提供相应的数据库操作适配器
 */
public class DbAdaptorFactory {

    // 存储数据库类型与对应适配器的映射关系
    private static Map<String, DbAdaptor> dbAdaptorMap;

    // 静态代码块用于初始化数据库适配器映射
    static {
        dbAdaptorMap = new HashMap<>();
        // 初始化各个数据库类型的适配器
        dbAdaptorMap.put(EngineType.CLICKHOUSE.getName(), new ClickHouseAdaptor());
        dbAdaptorMap.put(EngineType.MYSQL.getName(), new MysqlAdaptor());
        dbAdaptorMap.put(EngineType.H2.getName(), new H2Adaptor());
        dbAdaptorMap.put(EngineType.POSTGRESQL.getName(), new PostgresqlAdaptor());
        // 添加OpenGauss数据库适配器
        dbAdaptorMap.put(EngineType.OPENGAUSS.getName(), new OpenGaussAdaptor());
        // 添加默认数据库适配器，用于未知或其它数据库类型
        dbAdaptorMap.put(EngineType.OTHER.getName(), new DefaultDbAdaptor());
    }

    /**
     * 根据数据库类型获取相应的数据库适配器
     *
     * @param engineType 数据库类型名称
     * @return 对应数据库类型的适配器实例，如果没有找到则返回null
     */
    public static DbAdaptor getEngineAdaptor(String engineType) {
        return dbAdaptorMap.get(engineType);
    }
}
