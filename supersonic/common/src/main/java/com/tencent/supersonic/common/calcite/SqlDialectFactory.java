package com.tencent.supersonic.common.calcite;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlDialect.Context;
import org.apache.calcite.sql.SqlDialect.DatabaseProduct;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SQL方言工厂类，用于根据不同的数据库引擎类型获取相应的SQL方言配置
 * 
 * @author JiangWeiWei
 */
public class SqlDialectFactory {

    /**
     * 默认的SQL方言上下文配置，适用于大多数数据库产品 配置了基本的字符串引用规则和大小写敏感性等
     */
    public static final Context DEFAULT_CONTEXT =
            SqlDialect.EMPTY_CONTEXT.withDatabaseProduct(DatabaseProduct.BIG_QUERY)
                    .withLiteralQuoteString("'").withLiteralEscapedQuoteString("''")
                    .withIdentifierQuoteString("`").withUnquotedCasing(Casing.UNCHANGED)
                    .withQuotedCasing(Casing.UNCHANGED).withCaseSensitive(false);

    /**
     * PostgreSQL数据库的SQL方言上下文配置 由于历史原因，虽然配置相同，但保留了单独的配置以支持特定的数据库产品
     */
    public static final Context POSTGRESQL_CONTEXT = SqlDialect.EMPTY_CONTEXT
            .withDatabaseProduct(DatabaseProduct.BIG_QUERY).withLiteralQuoteString("'")
            .withLiteralEscapedQuoteString("''").withUnquotedCasing(Casing.UNCHANGED)
            .withQuotedCasing(Casing.UNCHANGED).withCaseSensitive(false);

    /**
     * OpenGauss数据库的SQL方言上下文配置 同样，配置与默认配置相同，但单独定义以明确支持OpenGauss数据库
     */
    public static final Context OPENGAUSS_CONTEXT = SqlDialect.EMPTY_CONTEXT
            .withDatabaseProduct(DatabaseProduct.BIG_QUERY).withLiteralQuoteString("'")
            .withLiteralEscapedQuoteString("''").withUnquotedCasing(Casing.UNCHANGED)
            .withQuotedCasing(Casing.UNCHANGED).withCaseSensitive(false);

    /**
     * 存储不同数据库引擎类型对应的SQL方言实例的映射
     */
    private static Map<EngineType, SemanticSqlDialect> sqlDialectMap;

    /**
     * 静态代码块，初始化SQL方言映射 为每种支持的数据库引擎类型创建相应的SQL方言实例
     */
    static {
        sqlDialectMap = new HashMap<>();
        sqlDialectMap.put(EngineType.CLICKHOUSE, new SemanticSqlDialect(DEFAULT_CONTEXT));
        sqlDialectMap.put(EngineType.MYSQL, new SemanticSqlDialect(DEFAULT_CONTEXT));
        sqlDialectMap.put(EngineType.H2, new SemanticSqlDialect(DEFAULT_CONTEXT));
        sqlDialectMap.put(EngineType.POSTGRESQL, new SemanticSqlDialect(POSTGRESQL_CONTEXT));
        sqlDialectMap.put(EngineType.OPENGAUSS, new SemanticSqlDialect(OPENGAUSS_CONTEXT));
    }

    /**
     * 根据数据库引擎类型获取相应的SQL方言实例 如果给定的引擎类型不在映射中，则返回默认的SQL方言实例
     *
     * @param engineType 数据库引擎类型
     * @return 对应的SQL方言实例
     */
    public static SemanticSqlDialect getSqlDialect(EngineType engineType) {
        SemanticSqlDialect semanticSqlDialect = sqlDialectMap.get(engineType);
        if (Objects.isNull(semanticSqlDialect)) {
            return new SemanticSqlDialect(DEFAULT_CONTEXT);
        }
        return semanticSqlDialect;
    }
}
