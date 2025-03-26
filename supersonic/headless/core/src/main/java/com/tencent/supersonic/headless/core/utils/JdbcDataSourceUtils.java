package com.tencent.supersonic.headless.core.utils;

import javax.sql.DataSource;

import com.alibaba.druid.util.StringUtils;
import com.tencent.supersonic.common.util.MD5Util;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;
import static com.tencent.supersonic.common.pojo.Constants.COLON;
import static com.tencent.supersonic.common.pojo.Constants.DOUBLE_SLASH;
import static com.tencent.supersonic.common.pojo.Constants.EMPTY;
import static com.tencent.supersonic.common.pojo.Constants.JDBC_PREFIX_FORMATTER;
import static com.tencent.supersonic.common.pojo.Constants.NEW_LINE_CHAR;
import static com.tencent.supersonic.common.pojo.Constants.PATTERN_JDBC_TYPE;
import static com.tencent.supersonic.common.pojo.Constants.SPACE;

/** tools functions about jdbc */
@Slf4j
public class JdbcDataSourceUtils {

    @Getter
    private static Set releaseSourceSet = new HashSet();
    private JdbcDataSource jdbcDataSource;

    public JdbcDataSourceUtils(JdbcDataSource jdbcDataSource) {
        this.jdbcDataSource = jdbcDataSource;
    }

    /**
     * 测试数据库连接 此方法旨在验证给定数据库信息是否能够成功建立连接 它首先尝试根据数据库URL加载正确的数据库驱动类， 然后尝试使用提供的URL、用户名和密码建立数据库连接
     *
     * @param database 数据库对象，包含连接数据库所需的信息
     * @return 如果成功建立数据库连接，则返回true；否则返回false
     */
    public static boolean testDatabase(Database database) {
        // 加载数据库驱动类
        try {
            Class.forName(getDriverClassName(database.getUrl()));
        } catch (ClassNotFoundException e) {
            // 如果驱动类未找到，则记录错误并返回false
            log.error(e.toString(), e);
            return false;
        }

        // 尝试建立数据库连接
        try (Connection con = DriverManager.getConnection(database.getUrl(), database.getUsername(),
                database.passwordDecrypt())) {
            // 如果连接成功，则返回true
            return con != null;
        } catch (SQLException e) {
            // 如果连接失败，则记录错误并返回false
            log.error(e.toString(), e);
        }

        // 如果上述步骤均未返回，则返回false
        return false;
    }

    public static void releaseConnection(Connection connection) {
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Connection release error", e);
            }
        }
    }

    public static void closeResult(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.error("ResultSet close error", e);
            }
        }
    }

    public static String isSupportedDatasource(String jdbcUrl) {
        String dataSourceName = getDataSourceName(jdbcUrl);
        if (StringUtils.isEmpty(dataSourceName)) {
            throw new RuntimeException("Not supported dataSource: jdbcUrl=" + jdbcUrl);
        }

        if (!DataType.getAllSupportedDatasourceNameSet().contains(dataSourceName)) {
            throw new RuntimeException("Not supported dataSource: jdbcUrl=" + jdbcUrl);
        }

        String urlPrefix = String.format(JDBC_PREFIX_FORMATTER, dataSourceName);
        String checkUrl = jdbcUrl.replaceFirst(DOUBLE_SLASH, EMPTY).replaceFirst(AT_SYMBOL, EMPTY);
        if (urlPrefix.equals(checkUrl)) {
            throw new RuntimeException("Communications link failure");
        }

        return dataSourceName;
    }

    public static String getDataSourceName(String jdbcUrl) {
        String dataSourceName = null;
        jdbcUrl = jdbcUrl.replaceAll(NEW_LINE_CHAR, EMPTY).replaceAll(SPACE, EMPTY).trim();
        Matcher matcher = PATTERN_JDBC_TYPE.matcher(jdbcUrl);
        if (matcher.find()) {
            dataSourceName = matcher.group().split(COLON)[1];
        }
        return dataSourceName;
    }

    /**
     * 根据JDBC URL获取数据库驱动类名
     *
     * @param jdbcUrl 数据库的JDBC URL
     * @return 数据库驱动类名
     * @throws RuntimeException 如果不支持的数据类型抛出运行时异常
     */
    public static String getDriverClassName(String jdbcUrl) {
        // 初始化数据库驱动类名为null
        String className = null;

        try {
            // 尝试通过JDBC URL获取数据库驱动，并获取其类名
            className = DriverManager.getDriver(jdbcUrl.trim()).getClass().getName();
        } catch (SQLException e) {
            // 如果发生SQL异常，记录错误日志
            log.error("e", e);
        }

        // 检查获取到的数据库驱动类名是否为空或代理类
        if (!StringUtils.isEmpty(className) && !className.contains("com.sun.proxy")
                && !className.contains("net.sf.cglib.proxy")) {
            // 如果不是代理类，直接返回数据库驱动类名
            return className;
        }

        // 尝试通过JDBC URL获取数据类型枚举
        DataType dataTypeEnum = DataType.urlOf(jdbcUrl);
        if (dataTypeEnum != null) {
            // 如果数据类型枚举不为空，返回对应的数据类型驱动类名
            return dataTypeEnum.getDriver();
        }
        // 如果数据类型不支持，抛出运行时异常
        throw new RuntimeException("Not supported data type: jdbcUrl=" + jdbcUrl);
    }

    public static String getKey(String name, String jdbcUrl, String username, String password,
            String version, boolean isExt) {

        StringBuilder sb = new StringBuilder();

        sb.append(StringUtils.isEmpty(name) ? "null" : name).append(COLON);
        sb.append(StringUtils.isEmpty(username) ? "null" : username).append(COLON);
        sb.append(StringUtils.isEmpty(password) ? "null" : password).append(COLON);
        sb.append(jdbcUrl.trim()).append(COLON);
        if (isExt && !StringUtils.isEmpty(version)) {
            sb.append(version);
        } else {
            sb.append("null");
        }

        return MD5Util.getMD5(sb.toString(), true, 64);
    }

    public DataSource getDataSource(Database database) throws RuntimeException {
        return jdbcDataSource.getDataSource(database);
    }

    public Connection getConnection(Database database) throws RuntimeException {
        Connection conn = getConnectionWithRetry(database);
        if (conn == null) {
            try {
                releaseDataSource(database);
                DataSource dataSource = getDataSource(database);
                return dataSource.getConnection();
            } catch (Exception e) {
                log.error("Get connection error, jdbcUrl:{}, e:{}", database.getUrl(), e);
                throw new RuntimeException("Get connection error, jdbcUrl:" + database.getUrl()
                        + " you can try again later or reset datasource");
            }
        }
        return conn;
    }

    private Connection getConnectionWithRetry(Database database) {
        int rc = 1;
        for (;;) {

            if (rc > 3) {
                return null;
            }

            try {
                Connection connection = getDataSource(database).getConnection();
                if (connection != null && connection.isValid(5)) {
                    return connection;
                }
            } catch (Exception e) {
                log.error("e", e);
            }

            try {
                Thread.sleep((long) Math.pow(2, rc) * 1000);
            } catch (InterruptedException e) {
                log.error("e", e);
            }

            rc++;
        }
    }

    public void releaseDataSource(Database database) {
        jdbcDataSource.removeDatasource(database);
    }
}
