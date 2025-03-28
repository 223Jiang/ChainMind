package com.tencent.supersonic.headless.core.utils;

import javax.sql.DataSource;

import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import io.seata.common.exception.DataAccessException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.rmi.ServerException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

/** tools functions about sql query */
@Slf4j
@Component
public class SqlUtils {

    @Getter
    private Database database;

    @Autowired
    private JdbcDataSource jdbcDataSource;

    @Value("${s2.source.result-limit:1000000}")
    private int resultLimit;

    @Value("${s2.source.enable-query-log:false}")
    private boolean isQueryLogEnable;

    @Getter
    private DataType dataTypeEnum;

    @Getter
    private JdbcDataSourceUtils jdbcDataSourceUtils;

    public SqlUtils() {}

    public SqlUtils(Database database) {
        this.database = database;
        this.dataTypeEnum = DataType.urlOf(database.getUrl());
    }

    public SqlUtils init(Database database) {
        return SqlUtilsBuilder.getBuilder()
                .withName(database.getId() + AT_SYMBOL + database.getName())
                .withType(database.getType()).withJdbcUrl(database.getUrl())
                .withUsername(database.getUsername()).withPassword(database.getPassword())
                .withJdbcDataSource(this.jdbcDataSource).withResultLimit(this.resultLimit)
                .withIsQueryLogEnable(this.isQueryLogEnable).build();
    }

    public List<Map<String, Object>> execute(String sql) throws ServerException {
        try {
            List<Map<String, Object>> list = jdbcTemplate().queryForList(sql);
            log.info("list:{}", list);
            return list;
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new ServerException(e.getMessage());
        }
    }

    public void execute(String sql, SemanticQueryResp queryResultWithColumns) {
        getResult(sql, queryResultWithColumns, jdbcTemplate());
    }

    public JdbcTemplate jdbcTemplate() throws RuntimeException {
        Connection connection = null;
        try {
            connection = jdbcDataSourceUtils.getConnection(database);
        } catch (Exception e) {
            log.warn("e:", e);
        } finally {
            JdbcDataSourceUtils.releaseConnection(connection);
        }
        DataSource dataSource = jdbcDataSourceUtils.getDataSource(database);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setDatabaseProductName(database.getName());
        jdbcTemplate.setFetchSize(500);
        return jdbcTemplate;
    }

    public void queryInternal(String sql, SemanticQueryResp queryResultWithColumns) {
        getResult(sql, queryResultWithColumns, jdbcTemplate());
    }

    /**
     * 执行SQL查询并获取结果
     *
     * 该方法使用JdbcTemplate执行给定的SQL查询，并将查询结果封装在SemanticQueryResp对象中
     * 它首先从查询结果中提取列信息，然后提取所有数据行，最后将这些信息设置到SemanticQueryResp对象中
     *
     * @param sql 包含查询语句的字符串
     * @param queryResultWithColumns 用于存储查询结果的SemanticQueryResp对象
     * @param jdbcTemplate 用于执行SQL查询的JdbcTemplate对象
     * @return 返回填充了查询结果的SemanticQueryResp对象
     */
    private SemanticQueryResp getResult(String sql, SemanticQueryResp queryResultWithColumns,
            JdbcTemplate jdbcTemplate) {
        // 使用JdbcTemplate执行SQL查询，处理结果集
        jdbcTemplate.query(sql, rs -> {
            // 如果结果集为空，直接返回原始的queryResultWithColumns对象
            if (null == rs) {
                return queryResultWithColumns;
            }

            // 获取结果集的元数据
            ResultSetMetaData metaData = rs.getMetaData();
            // 创建一个列表，用于存储查询列信息
            List<QueryColumn> queryColumns = new ArrayList<>();
            // 遍历每一列，提取列标签和列类型，并添加到queryColumns列表中
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String key = metaData.getColumnLabel(i);
                queryColumns.add(new QueryColumn(key, metaData.getColumnTypeName(i)));
            }
            // 将列信息设置到queryResultWithColumns对象中
            queryResultWithColumns.setColumns(queryColumns);

            // 从结果集中提取所有数据，并将结果设置到queryResultWithColumns对象中
            List<Map<String, Object>> resultList = getAllData(rs, queryColumns);
            queryResultWithColumns.setResultList(resultList);
            // 返回填充了查询结果的queryResultWithColumns对象
            return queryResultWithColumns;
        });
        // 返回queryResultWithColumns对象，此时应已填充好查询结果
        return queryResultWithColumns;
    }

    /**
     * 执行SQL表相关语句
     *
     * 该方法通过参数化查询和输入校验来防止SQL注入。
     *
     * @param createTableSql 包含创建表语句的字符串
     * @throws ServerException 如果执行过程中发生异常，则抛出ServerException
     */
    public void executeCreateTable(String createTableSql) throws ServerException {
        // 校验输入是否为空或无效
        if (StringUtils.isBlank(createTableSql)) {
            String errorMessage = "The create table SQL statement cannot be null or empty.";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // 去除SQL语句中的多余空格并打印日志
        String trimmedSql = createTableSql.trim();
        log.info("Attempting to execute create table SQL: {}", trimmedSql);

        // 使用正则表达式校验SQL语句是否合法（仅允许特定关键字）
        // TODO 待完善校验逻辑
        /*if (!isValidSql(trimmedSql)) {
            String errorMessage = "Invalid SQL statement detected: " + trimmedSql;
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }*/

        try {
            // 使用 JdbcTemplate 执行 SQL 创建表语句
            jdbcTemplate().execute(trimmedSql);
            log.info("Table created successfully with SQL: {}", trimmedSql);
        } catch (Exception e) {
            // 捕获异常并记录详细错误信息
            String errorDetails = String.format("Error executing create table SQL: %s, error message: %s",
                    trimmedSql, e.getMessage());
            log.error(errorDetails, e);
            throw new ServerException(errorDetails);
        }
    }

    public void executeBatchUpdate(String sql, List<Object[]> batchArgs) throws ServerException {
        // 校验输入是否为空或无效
        if (StringUtils.isBlank(sql)) {
            String errorMessage = "The batch update SQL statement cannot be null or empty.";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // 去除SQL语句中的多余空格并打印日志
        String trimmedSql = sql.trim();

        try {
            // 使用 JdbcTemplate 执行批量更新操作
            jdbcTemplate().batchUpdate(trimmedSql, batchArgs);
            log.info("Batch update executed successfully with SQL: {}", trimmedSql);
        } catch (DataAccessException e) {
            // 捕获更具体的异常并记录详细错误信息
            String errorDetails = String.format("Error executing batch update SQL: %s, error message: %s",
                    trimmedSql, e.getMessage());
            log.error(errorDetails, e);
            throw new ServerException(errorDetails);
        }
    }


    /**
     * 校验SQL语句是否合法
     *
     * @param sql 待校验的SQL语句
     * @return 合法返回true，否则返回false
     */
    private boolean isValidSql(String sql) {
        // 定义允许的关键字和模式
        String regex = "^(?i)(CREATE\\s+TABLE\\s+\\w+\\s*\\([^\\(]*\\)|ALTER\\s+TABLE\\s+\\w+\\s+.*)$";
        return sql.matches(regex);

    }


    private List<Map<String, Object>> getAllData(ResultSet rs, List<QueryColumn> queryColumns) {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            while (rs.next()) {
                data.add(getLineData(rs, queryColumns));
            }
        } catch (Exception e) {
            log.warn("error in getAllData, e:", e);
        }
        return data;
    }

    private Map<String, Object> getLineData(ResultSet rs, List<QueryColumn> queryColumns)
            throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        for (QueryColumn queryColumn : queryColumns) {
            String colName = queryColumn.getNameEn();
            Object value = rs.getObject(colName);
            map.put(colName, getValue(value));
        }
        return map;
    }

    private Object getValue(Object value) {
        if (value instanceof LocalDate) {
            LocalDate localDate = (LocalDate) value;
            return localDate.format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_DATE_FORMAT));
        } else if (value instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) value;
            return localDateTime.format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_TIME_FORMAT));
        } else if (value instanceof Date) {
            Date date = (Date) value;
            return DateUtils.format(date);
        } else if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value;
    }

    public static final class SqlUtilsBuilder {

        private JdbcDataSource jdbcDataSource;
        private int resultLimit;
        private boolean isQueryLogEnable;
        private String name;
        private String type;
        private String jdbcUrl;
        private String username;
        private String password;

        private SqlUtilsBuilder() {}

        public static SqlUtilsBuilder getBuilder() {
            return new SqlUtilsBuilder();
        }

        SqlUtilsBuilder withJdbcDataSource(JdbcDataSource jdbcDataSource) {
            this.jdbcDataSource = jdbcDataSource;
            return this;
        }

        SqlUtilsBuilder withResultLimit(int resultLimit) {
            this.resultLimit = resultLimit;
            return this;
        }

        SqlUtilsBuilder withIsQueryLogEnable(boolean isQueryLogEnable) {
            this.isQueryLogEnable = isQueryLogEnable;
            return this;
        }

        SqlUtilsBuilder withName(String name) {
            this.name = name;
            return this;
        }

        SqlUtilsBuilder withType(String type) {
            this.type = type;
            return this;
        }

        SqlUtilsBuilder withJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        SqlUtilsBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        SqlUtilsBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public SqlUtils build() {
            Database database = Database.builder().name(this.name).type(this.type).url(this.jdbcUrl)
                    .username(this.username).password(this.password).build();

            SqlUtils sqlUtils = new SqlUtils(database);
            sqlUtils.jdbcDataSource = this.jdbcDataSource;
            sqlUtils.resultLimit = this.resultLimit;
            sqlUtils.isQueryLogEnable = this.isQueryLogEnable;
            sqlUtils.jdbcDataSourceUtils = new JdbcDataSourceUtils(this.jdbcDataSource);

            return sqlUtils;
        }
    }
}
