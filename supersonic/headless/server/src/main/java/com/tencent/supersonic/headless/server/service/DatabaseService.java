package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import com.tencent.supersonic.headless.api.pojo.request.SqlExecuteReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.pojo.DatabaseParameter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author JiangWeiWei
 */
public interface DatabaseService {

    SemanticQueryResp executeSql(String sql, DatabaseResp databaseResp);

    SemanticQueryResp executeSql(SqlExecuteReq sqlExecuteReq, Long id, User user);

    /**
     * 执行表相关sql语句操作
     * @param sqlExecuteReq SQL执行请求
     * @param id            数据库实例id
     * @param user          用户id
     */
    void executeTableSql(SqlExecuteReq sqlExecuteReq, Long id, User user);

    /**
     * 执行批量添加sql语句操作
     * @param sqlExecuteReq SQL执行请求
     * @param batchArgs     插入数据集
     * @param id            数据库实例id
     * @param user          用户id
     */
    void executeSaveSql(SqlExecuteReq sqlExecuteReq, List<Object[]> batchArgs, Long id, User user);

    DatabaseResp getDatabase(Long id, User user);

    DatabaseResp getDatabase(Long id);

    Map<String, List<DatabaseParameter>> getDatabaseParameters(User user);

    boolean testConnect(DatabaseReq databaseReq, User user);

    DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user);

    List<DatabaseResp> getDatabaseList(User user);

    void deleteDatabase(Long databaseId);

    List<String> getDbNames(Long id) throws SQLException;

    List<String> getTables(Long id, String db) throws SQLException;

    Map<String, List<DBColumn>> getDbColumns(ModelBuildReq modelBuildReq) throws SQLException;

    List<DBColumn> getColumns(Long id, String db, String table) throws SQLException;

    List<DBColumn> getColumns(Long id, String sql) throws SQLException;
}
