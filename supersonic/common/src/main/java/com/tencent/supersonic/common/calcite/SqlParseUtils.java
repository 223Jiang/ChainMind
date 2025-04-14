package com.tencent.supersonic.common.calcite;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL 解析工具
 */
public class SqlParseUtils {

    /**
     * 获取 SQL parseInfo
     *
     * @param SQL SQL 语句进行解析
     * @return 返回 SQL 语句的解析信息
     * @throws RuntimeException，如果解析失败，则抛出运行时异常
     */
    public static SqlParserInfo getSqlParseInfo(String sql) {
        try {
            // 创建 SQL 解析器
            SqlParser parser = SqlParser.create(sql);
            // 将 SQL 语句解析为抽象语法树
            SqlNode sqlNode = parser.parseQuery();
            // 初始化 SQL 解析信息对象
            SqlParserInfo sqlParserInfo = new SqlParserInfo();

            // 递归处理 SQL 节点以提取解析信息
            handlerSQL(sqlNode, sqlParserInfo);

            // 删除所有字段中的重复字段
            sqlParserInfo.setAllFields(
                    sqlParserInfo.getAllFields().stream().distinct().collect(Collectors.toList()));

            // 删除选择字段中的重复字段
            sqlParserInfo.setSelectFields(sqlParserInfo.getSelectFields().stream().distinct()
                    .collect(Collectors.toList()));

            // 返回 SQL 解析信息对象
            return sqlParserInfo;
        } catch (SqlParseException e) {
            // 如果解析失败，则引发运行时异常
            throw new RuntimeException("getSqlParseInfo", e);
        }
    }

    /**
     * 处理SQL语句
     * <p>
     * 该方法根据SQL节点的类型调用相应的处理方法它通过解析SQL节点的种类来决定执行哪种类型的SQL处理函数
     * 主要目的是对传入的SQL语句进行分类处理，目前只处理SELECT和ORDER BY两种类型
     *
     * @param sqlNode       SQL节点对象，包含了SQL语句的结构化信息
     * @param sqlParserInfo SQL解析信息对象，用于存储和传递SQL解析过程中的相关信息
     */
    public static void handlerSQL(SqlNode sqlNode, SqlParserInfo sqlParserInfo) {
        // 获取SQL节点的类型
        SqlKind kind = sqlNode.getKind();

        // 根据SQL类型执行相应的处理逻辑
        switch (kind) {
            case SELECT:
                // 当SQL类型为SELECT时，调用处理SELECT语句的方法
                handlerSelect(sqlNode, sqlParserInfo);
                break;
            case ORDER_BY:
                // 当SQL类型为ORDER BY时，调用处理ORDER BY语句的方法
                handlerOrderBy(sqlNode, sqlParserInfo);
                break;
            default:
                // 对于其他SQL类型，目前不进行处理
                break;
        }
    }

    /**
     * 处理 SQL 语句中的 order by 子句。
     * 该方法负责对 order by 子句进行解析和处理，以提取排序字段并更新相关的 SQL 解析信息。
     *
     * @param node          表示 order by 子句的 SqlNode，用于访问和处理 order by 信息。
     * @param sqlParserInfo 包含从 SQL 语句解析的所有信息的 SqlParserInfo 对象，用于存储提取的字段信息。
     */
    private static void handlerOrderBy(SqlNode node, SqlParserInfo sqlParserInfo) {
        // 将输入节点转换为 SqlOrderBy 类型，以处理特定的 order by作。
        SqlOrderBy sqlOrderBy = (SqlOrderBy) node;

        // 提取 order by 子句之前的查询部分以进行进一步处理。
        SqlNode query = sqlOrderBy.query;

        // 以递归方式处理 SQL 查询部分，以确保 SQL 的所有部分都得到完全解析。
        handlerSQL(query, sqlParserInfo);

        // 从 order by 子句中提取排序列表。
        SqlNodeList orderList = sqlOrderBy.orderList;

        // 解析 orderList 以获取排序中涉及的所有字段名称。
        Set<String> orderFields = handlerField(orderList);

        // 使用提取的排序字段更新 SqlParserInfo 对象，以维护与 SQL 语句相关的所有字段信息。
        sqlParserInfo.getAllFields().addAll(orderFields);
    }

    /**
     * hanlder select
     *
     * @param select
     * @param sqlParserInfo
     */
    private static void handlerSelect(SqlNode select, SqlParserInfo sqlParserInfo) {
        List<String> allFields = sqlParserInfo.getAllFields();
        SqlSelect sqlSelect = (SqlSelect) select;
        SqlNodeList selectList = sqlSelect.getSelectList();

        selectList.getList().forEach(list -> {
            Set<String> selectFields = handlerField(list);
            sqlParserInfo.getSelectFields().addAll(selectFields);
        });
        String tableName = handlerFrom(sqlSelect.getFrom());
        sqlParserInfo.setTableName(tableName);

        Set<String> selectFields = handlerSelectField(sqlSelect);
        allFields.addAll(selectFields);
    }

    private static Set<String> handlerSelectField(SqlSelect sqlSelect) {
        Set<String> results = new HashSet<>();
        if (sqlSelect.getFrom() instanceof SqlBasicCall) {
            Set<String> formFields = handlerField(sqlSelect.getFrom());
            results.addAll(formFields);
        }

        sqlSelect.getSelectList().getList().forEach(list -> {
            Set<String> selectFields = handlerField(list);
            results.addAll(selectFields);
        });

        if (sqlSelect.hasWhere()) {
            Set<String> whereFields = handlerField(sqlSelect.getWhere());
            results.addAll(whereFields);
        }
        if (sqlSelect.hasOrderBy()) {
            Set<String> orderByFields = handlerField(sqlSelect.getOrderList());
            results.addAll(orderByFields);
        }
        SqlNodeList group = sqlSelect.getGroup();
        if (group != null) {
            group.forEach(groupField -> {
                Set<String> groupByFields = handlerField(groupField);
                results.addAll(groupByFields);
            });
        }
        return results;
    }

    /**
     * hander from
     *
     * @param from
     * @return
     */
    private static String handlerFrom(SqlNode from) {
        SqlKind kind = from.getKind();
        switch (kind) {
            case IDENTIFIER:
                SqlIdentifier sqlIdentifier = (SqlIdentifier) from;
                return sqlIdentifier.getSimple();
            case AS:
                SqlBasicCall sqlBasicCall = (SqlBasicCall) from;
                SqlNode sqlNode = sqlBasicCall.getOperandList().get(0);
                SqlSelect sqlSelect = (SqlSelect) sqlNode;
                return handlerFrom(sqlSelect.getFrom());
            default:
                break;
        }
        return "";
    }

    /**
     * handler field
     *
     * @param field
     */
    private static Set<String> handlerField(SqlNode field) {
        Set<String> fields = new HashSet<>();
        SqlKind kind = field.getKind();
        switch (kind) {
            case AS:
                List<SqlNode> operandList1 = ((SqlBasicCall) field).getOperandList();
                SqlNode leftAs = operandList1.get(0);
                fields.addAll(handlerField(leftAs));
                break;
            case IDENTIFIER:
                SqlIdentifier sqlIdentifier = (SqlIdentifier) field;
                String simpleName = sqlIdentifier.getSimple();
                if (StringUtils.isNotEmpty(simpleName)) {
                    fields.add(simpleName);
                }
                break;
            case SELECT:
                SqlSelect sqlSelect = (SqlSelect) field;
                fields.addAll(handlerSelectField(sqlSelect));
                break;
            default:
                if (field instanceof SqlBasicCall) {
                    List<SqlNode> operandList2 = ((SqlBasicCall) field).getOperandList();
                    for (int i = 0; i < operandList2.size(); i++) {
                        fields.addAll(handlerField(operandList2.get(i)));
                    }
                }
                if (field instanceof SqlNodeList) {
                    ((SqlNodeList) field).getList().forEach(node -> {
                        fields.addAll(handlerField(node));
                    });
                }
                break;
        }
        return fields;
    }

    public static String addAliasToSql(String sql) throws SqlParseException {
        SqlParser parser = SqlParser.create(sql);
        SqlNode sqlNode = parser.parseStmt();

        if (!(sqlNode instanceof SqlSelect)) {
            return sql;
        }

        SqlNodeList selectList = ((SqlSelect) sqlNode).getSelectList();
        for (SqlNode node : selectList) {
            if (node instanceof SqlBasicCall) {
                SqlBasicCall sqlBasicCall = (SqlBasicCall) node;

                List<SqlNode> operandList = sqlBasicCall.getOperandList();
                if (CollectionUtils.isNotEmpty(operandList) && operandList.size() == 1) {
                    SqlIdentifier sqlIdentifier = (SqlIdentifier) operandList.get(0);
                    String simple = sqlIdentifier.getSimple();
                    SqlBasicCall aliasedNode =
                            new SqlBasicCall(SqlStdOperatorTable.AS,
                                    new SqlNode[]{sqlBasicCall, new SqlIdentifier(
                                            simple.toLowerCase(), SqlParserPos.ZERO)},
                                    SqlParserPos.ZERO);
                    selectList.set(selectList.indexOf(node), aliasedNode);
                }
            }
        }
        SqlDialect dialect = new S2MysqlSqlDialect(S2MysqlSqlDialect.DEFAULT_CONTEXT);
        SqlString newSql = sqlNode.toSqlString(dialect);
        return newSql.getSql().replaceAll("`", "");
    }

    public static String addFieldsToSql(String sql, List<String> addFields)
            throws SqlParseException {
        if (CollectionUtils.isEmpty(addFields)) {
            return sql;
        }
        SqlParser parser = SqlParser.create(sql);
        SqlNode sqlNode = parser.parseStmt();
        SqlNodeList selectList = getSelectList(sqlNode);

        // agg to field not allow to add field
        if (Objects.isNull(selectList)) {
            return sql;
        }
        for (SqlNode node : selectList) {
            if (node instanceof SqlBasicCall) {
                return sql;
            }
        }
        Set<String> existFields = new HashSet<>();
        for (SqlNode node : selectList.getList()) {
            if (node instanceof SqlIdentifier) {
                String fieldName = ((SqlIdentifier) node).getSimple();
                existFields.add(fieldName.toLowerCase());
            }
        }

        for (String addField : addFields) {
            if (existFields.contains(addField.toLowerCase())) {
                continue;
            }
            SqlIdentifier newField = new SqlIdentifier(addField, SqlParserPos.ZERO);
            selectList.add(newField);
            existFields.add(addField.toLowerCase());
        }
        SqlDialect dialect = new S2MysqlSqlDialect(S2MysqlSqlDialect.DEFAULT_CONTEXT);
        SqlString newSql = sqlNode.toSqlString(dialect);

        return newSql.getSql().replaceAll("`", "");
    }

    private static SqlNodeList getSelectList(SqlNode sqlNode) {
        SqlKind kind = sqlNode.getKind();

        switch (kind) {
            case SELECT:
                SqlSelect sqlSelect = (SqlSelect) sqlNode;
                return sqlSelect.getSelectList();
            case ORDER_BY:
                SqlOrderBy sqlOrderBy = (SqlOrderBy) sqlNode;
                SqlSelect query = (SqlSelect) sqlOrderBy.query;
                return query.getSelectList();
            default:
                break;
        }
        return null;
    }

    public static Set<String> getFilterField(String where) {
        Set<String> result = new HashSet<>();
        try {

            SqlParser parser = SqlParser.create(where);
            SqlNode sqlNode = parser.parseExpression();
            getFieldByExpression(sqlNode, result);
            return result;
        } catch (SqlParseException e) {
            throw new RuntimeException("getSqlParseInfo", e);
        }
    }

    public static void getFieldByExpression(SqlNode sqlNode, Set<String> fields) {
        if (sqlNode instanceof SqlIdentifier) {
            SqlIdentifier sqlIdentifier = (SqlIdentifier) sqlNode;
            fields.add(sqlIdentifier.names.get(0).toLowerCase());
            return;
        }
        if (sqlNode instanceof SqlBasicCall) {
            SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
            for (SqlNode operand : sqlBasicCall.getOperandList()) {
                getFieldByExpression(operand, fields);
            }
        }
    }

    public static Map getCaseExprFields(String expr) {
        SqlParser parser = SqlParser.create(expr);
        Map<String, String> ret = new HashMap();
        try {
            SqlNode sqlNodeCase = parser.parseExpression();
            if (sqlNodeCase instanceof SqlCase) {
                SqlCase sqlCase = (SqlCase) sqlNodeCase;
                if (CollectionUtils.isEmpty(sqlCase.getThenOperands())
                        || CollectionUtils.isEmpty(sqlCase.getWhenOperands())) {
                    return ret;
                }
                SqlDialect dialect = new S2MysqlSqlDialect(S2MysqlSqlDialect.DEFAULT_CONTEXT);
                int i = 0;
                for (SqlNode sqlNode : sqlCase.getWhenOperands().getList()) {
                    if (sqlNode instanceof SqlBasicCall) {
                        SqlBasicCall when = (SqlBasicCall) sqlNode;
                        if (!org.springframework.util.CollectionUtils.isEmpty(when.getOperandList())
                                && when.getOperandList().size() > 1) {
                            String value =
                                    when.getOperandList().get(1).toSqlString(dialect).getSql();
                            if (sqlCase.getThenOperands().get(i) != null) {
                                if (sqlCase.getThenOperands().get(i) instanceof SqlIdentifier) {
                                    SqlIdentifier sqlIdentifier =
                                            (SqlIdentifier) sqlCase.getThenOperands().get(i);
                                    String field = sqlIdentifier.getSimple();
                                    ret.put(value, field);
                                }
                            }
                        }
                    }
                    i++;
                }
            }
        } catch (SqlParseException e) {
            throw new RuntimeException("getSqlParseInfo", e);
        }
        return ret;
    }
}
