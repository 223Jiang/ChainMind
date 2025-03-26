package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class BaseDbAdaptor implements DbAdaptor {

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
            // 获取数据库的schemas信息
            ResultSet schemaSet = metaData.getSchemas();
            while (schemaSet.next()) {
                String db = schemaSet.getString("TABLE_SCHEM");
                dbs.add(db);
            }
        } catch (Exception e) {
            // 如果获取schemas失败，记录日志并尝试获取catalogs信息
            log.info("get meta schemas failed, try to get catalogs");
        }
        try {
            // 获取数据库的catalogs信息
            ResultSet catalogSet = metaData.getCatalogs();
            while (catalogSet.next()) {
                String db = catalogSet.getString("TABLE_CAT");
                dbs.add(db);
            }
        } catch (Exception e) {
            // 如果获取catalogs失败，记录日志并尝试获取schemas信息
            log.info("get meta catalogs failed, try to get schemas");
        }
        return dbs;
    }

    /**
     * 获取指定模式下的所有表和视图的名称列表。 该方法通过提供的连接信息连接到数据库，并检索指定模式下的所有表和视图的名称。
     *
     * @param connectionInfo 连接信息对象，包含数据库连接所需的信息
     * @param schemaName 模式名称，用于筛选属于该模式的表和视图
     * @return 返回一个字符串列表，包含指定模式下的所有表和视图的名称
     * @throws SQLException 如果在获取表和视图时发生SQL异常，则抛出该异常
     */
    @Override
    public List<String> getTables(ConnectInfo connectionInfo, String schemaName)
            throws SQLException {
        // 初始化一个列表来存储表和视图的名称
        List<String> tablesAndViews = new ArrayList<>();
        // 获取数据库元数据
        DatabaseMetaData metaData = getDatabaseMetaData(connectionInfo);

        try {
            // 获取指定模式下的表和视图的结果集
            ResultSet resultSet = getResultSet(schemaName, metaData);
            // 遍历结果集，将每个表和视图的名称添加到列表中
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                tablesAndViews.add(name);
            }
        } catch (SQLException e) {
            // 如果获取表和视图失败，记录错误日志
            log.error("Failed to get tables and views", e);
        }
        // 返回包含所有表和视图名称的列表
        return tablesAndViews;
    }


    protected ResultSet getResultSet(String schemaName, DatabaseMetaData metaData)
            throws SQLException {
        return metaData.getTables(schemaName, schemaName, null, new String[] {"TABLE", "VIEW"});
    }

    @Override
    public List<DBColumn> getColumns(ConnectInfo connectInfo, String schemaName, String tableName)
            throws SQLException {
        List<DBColumn> dbColumns = Lists.newArrayList();
        DatabaseMetaData metaData = getDatabaseMetaData(connectInfo);
        ResultSet columns = metaData.getColumns(schemaName, schemaName, tableName, null);
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String dataType = columns.getString("TYPE_NAME");
            String remarks = columns.getString("REMARKS");
            dbColumns.add(new DBColumn(columnName, dataType, remarks));
        }
        return dbColumns;
    }

    protected DatabaseMetaData getDatabaseMetaData(ConnectInfo connectionInfo) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionInfo.getUrl(),
                connectionInfo.getUserName(), connectionInfo.getPassword());
        return connection.getMetaData();
    }

}
