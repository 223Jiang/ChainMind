package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.common.pojo.enums.EngineType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author WeiWei
 *
 *         数据库参数工厂类 用于根据不同的数据库引擎类型获取相应的参数构建器
 */
public class DbParameterFactory {

    // 存储不同数据库引擎参数构建器的映射
    private static Map<String, DbParametersBuilder> parametersBuilder;

    // 静态代码块，初始化参数构建器映射
    static {
        parametersBuilder = new LinkedHashMap<>();
        // 为支持的数据库引擎类型初始化参数构建器
        parametersBuilder.put(EngineType.H2.getName(), new H2ParametersBuilder());
        parametersBuilder.put(EngineType.CLICKHOUSE.getName(), new ClickHouseParametersBuilder());
        parametersBuilder.put(EngineType.MYSQL.getName(), new MysqlParametersBuilder());
        parametersBuilder.put(EngineType.POSTGRESQL.getName(), new PostgresqlParametersBuilder());
        // 新增OpenGauss
        parametersBuilder.put(EngineType.OPENGAUSS.getName(), new OpenGaussParametersBuilder());
        parametersBuilder.put(EngineType.OTHER.getName(), new OtherParametersBuilder());
    }

    /**
     * 根据数据库引擎类型获取对应的参数构建器
     * 
     * @param engineType 数据库引擎类型
     * @return 对应引擎类型的参数构建器，如果不存在则返回null
     */
    public static DbParametersBuilder get(String engineType) {
        return parametersBuilder.get(engineType);
    }

    /**
     * 获取所有参数构建器的映射
     * 
     * @return 包含所有参数构建器的映射
     */
    public static Map<String, DbParametersBuilder> getMap() {
        return parametersBuilder;
    }
}
