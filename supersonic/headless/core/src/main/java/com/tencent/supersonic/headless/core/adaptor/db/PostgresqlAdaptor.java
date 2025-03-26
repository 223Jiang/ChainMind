package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author WeiWei
 */
@Slf4j
public class PostgresqlAdaptor extends BaseDbAdaptor {

    /**
     * 获取数据库列表
     *
     * @param connectionInfo 连接信息对象，包含数据库连接所需的信息
     * @return 返回一个字符串列表，包含所有数据库名称
     * @throws SQLException 如果获取数据库列表时发生SQL异常
     */
    @Override
    public List<String> getDBs(ConnectInfo connectionInfo) throws SQLException {
        List<String> dbs = Lists.newArrayList();
        DatabaseMetaData metaData = getDatabaseMetaData(connectionInfo);
        try {
            // 获取数据库的catalogs信息
            ResultSet catalogSet = metaData.getCatalogs();
            while (catalogSet.next()) {
                String db = catalogSet.getString("TABLE_CAT");
                dbs.add(db);
            }
        } catch (Exception e) {
            // 如果获取catalogs失败，记录日志
            log.info("get meta catalogs failed", e);
        }
        return dbs;
    }

    /**
     * 根据日期类型和格式获取日期格式化字符串
     *
     * @param dateType 日期类型，如月、周等
     * @param dateFormat 日期格式，支持整数格式和常规格式
     * @param column 数据库中的日期列名
     * @return 返回根据日期类型和格式化要求的日期字符串
     */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        // 当日期格式为整数类型时
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            // 如果日期类型为月，则格式化为yyyy-mm格式
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "to_char(to_date(%s,'yyyymmdd'), 'yyyy-mm')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                // 如果日期类型为周，则格式化为yyyy-mm-dd格式，并truncate到周的开始
                return "to_char(date_trunc('week',to_date(%s, 'yyyymmdd')),'yyyy-mm-dd')"
                        .replace("%s", column);
            } else {
                // 对于其他情况，直接格式化为yyyy-mm-dd格式
                return "to_char(to_date(%s,'yyyymmdd'), 'yyyy-mm-dd')".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            // 当日期格式为常规类型时
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                // 如果日期类型为月，则格式化为yyyy-mm格式
                return "to_char(to_date(%s,'yyyy-mm-dd'), 'yyyy-mm')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                // 如果日期类型为周，则格式化为yyyy-mm-dd格式，并truncate到周的开始
                return "to_char(date_trunc('week',to_date(%s, 'yyyy-mm-dd')),'yyyy-mm-dd')"
                        .replace("%s", column);
            } else {
                // 对于其他情况，直接返回原始列名，不需要格式化
                return column;
            }
        }
        // 如果不满足上述条件，直接返回原始列名
        return column;
    }

    /**
     * 重写函数名修正器方法 该方法用于将SQL语句中的特定函数名替换为正确的函数名，并根据函数名添加相应的参数
     * 主要用于处理SQL语句中函数名的错误拼写或不规范使用，提高SQL语句的正确性和一致性
     *
     * @param sql 需要修正的SQL语句
     * @return 修正后的SQL语句
     */
    @Override
    public String functionNameCorrector(String sql) {
        // 创建一个映射，用于将错误或不规范的函数名映射到正确的函数名
        Map<String, String> functionMap = new HashMap<>();
        functionMap.put("MONTH".toLowerCase(), "TO_CHAR");
        functionMap.put("DAY".toLowerCase(), "TO_CHAR");
        functionMap.put("YEAR".toLowerCase(), "TO_CHAR");

        // 创建一个映射，用于定义如何处理特定函数名的参数
        Map<String, UnaryOperator> functionCall = new HashMap<>();
        functionCall.put("MONTH".toLowerCase(), o -> {
            if (Objects.nonNull(o) && o instanceof ExpressionList) {
                ExpressionList expressionList = (ExpressionList) o;
                expressionList.add(new StringValue("MM"));
                return expressionList;
            }
            return o;
        });
        functionCall.put("DAY".toLowerCase(), o -> {
            if (Objects.nonNull(o) && o instanceof ExpressionList) {
                ExpressionList expressionList = (ExpressionList) o;
                expressionList.add(new StringValue("dd"));
                return expressionList;
            }
            return o;
        });
        functionCall.put("YEAR".toLowerCase(), o -> {
            if (Objects.nonNull(o) && o instanceof ExpressionList) {
                ExpressionList expressionList = (ExpressionList) o;
                expressionList.add(new StringValue("YYYY"));
                return expressionList;
            }
            return o;
        });

        // 调用SqlReplaceHelper类的静态方法replaceFunction，执行函数名和参数的替换
        return SqlReplaceHelper.replaceFunction(sql, functionMap, functionCall);
    }

    /**
     * 获取指定数据库模式下的所有表和视图名称。 该方法通过提供的连接信息连接到数据库，并使用数据库元数据查询指定模式下的所有表和视图。
     * 查询结果包括表和视图的名称，并将这些名称存储在一个列表中返回。
     *
     * @param connectionInfo 数据库连接信息对象，包含建立数据库连接所需的详细信息。
     * @param schemaName 要查询的数据库模式名称。
     * @return 包含指定模式下所有表和视图名称的字符串列表。
     * @throws SQLException 如果发生SQL错误。
     */
    @Override
    public List<String> getTables(ConnectInfo connectionInfo, String schemaName)
            throws SQLException {
        // 初始化一个列表，用于存储表和视图的名称。
        List<String> tablesAndViews = Lists.newArrayList();

        // 获取数据库元数据，这是查询表和视图信息的前提。
        DatabaseMetaData metaData = getDatabaseMetaData(connectionInfo);

        // 执行查询，获取指定模式下的所有表和视图。
        try (ResultSet resultSet = metaData.getTables(null,
                getParameterValue(connectionInfo.getUrl(), "currentSchema"), null,
                new String[] {"TABLE", "VIEW"})) {
            // 遍历查询结果，提取表和视图的名称。
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                tablesAndViews.add(name);
            }
        } catch (SQLException e) {
            // 记录错误信息，便于问题追踪和定位。
            log.error("Failed to get tables and views", e);
        }
        // 返回包含所有表和视图名称的列表。
        return tablesAndViews;
    }

    /**
     * 从JDBC URL中提取指定参数的值。
     *
     * @param jdbcUrl JDBC URL字符串
     * @param paramName 要提取的参数名称
     * @return 参数的值，如果参数不存在则返回null
     */
    public static String getParameterValue(String jdbcUrl, String paramName) {
        // 定义正则表达式模式，用于匹配参数名和参数值
        String regex = "[?&]" + Pattern.quote(paramName) + "=([^&]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jdbcUrl);

        // 查找匹配项
        if (matcher.find()) {
            // 返回匹配到的参数值
            return matcher.group(1);
        }
        // 如果未找到匹配项，返回null
        return null;
    }

    /**
     * 获取指定数据库连接信息、schema名称和表名称下的所有列信息
     *
     * @param connectInfo 数据库连接信息对象，包含连接数据库所需的信息
     * @param schemaName schema名称，用于指定数据库模式
     * @param tableName 表名称，用于指定要获取列信息的表
     * @return 返回一个包含DBColumn对象的列表，每个对象代表表中的一列
     * @throws SQLException 如果在执行数据库操作时发生错误
     */
    @Override
    public List<DBColumn> getColumns(ConnectInfo connectInfo, String schemaName, String tableName)
            throws SQLException {
        List<DBColumn> dbColumns = Lists.newArrayList();
        // 获取数据库元数据，用于进一步查询表的列信息
        DatabaseMetaData metaData = getDatabaseMetaData(connectInfo);
        // 查询指定表的所有列信息
        ResultSet columns = metaData.getColumns(null, null, tableName, null);
        // 遍历查询结果，为每一列创建DBColumn对象，并添加到列表中
        while (columns.next()) {
            // 获取列名
            String columnName = columns.getString("COLUMN_NAME");
            // 获取数据类型
            String dataType = columns.getString("TYPE_NAME");
            // 获取列的注释或备注
            String remarks = columns.getString("REMARKS");
            // 创建DBColumn对象并添加到列表中
            dbColumns.add(new DBColumn(columnName, dataType, remarks));
        }
        // 返回包含所有列信息的列表
        return dbColumns;
    }
}
