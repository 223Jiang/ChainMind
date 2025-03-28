package com.larkmt.cn.admin.tool.meta;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/3
 */
public class OpenGaussDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {

    private volatile static OpenGaussDatabaseMeta single;

    public static OpenGaussDatabaseMeta getInstance() {
        if (single == null) {
            synchronized (PostgresqlDatabaseMeta.class) {
                if (single == null) {
                    single = new OpenGaussDatabaseMeta();
                }
            }
        }
        return single;
    }

    @Override
    public String getSQLQueryPrimaryKey() {
        return "select column_name from information_schema.columns where table_schema='public' and table_name='tb_cis_patient_info' and is_identity = 'YES'";
    }

    @Override
    public String getSQLQueryTables() {
        return "select relname as tabname from pg_class c \n" +
                "where  relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%' group by relname order by relname limit 500";
    }


    @Override
    public String getSQLQueryTables(String... tableSchema) {
        return "SELECT concat_ws('.',\"table_schema\",\"table_name\") FROM information_schema.tables \n" +
                "where (\"table_name\" not like 'pg_%' AND \"table_name\" not like 'sql_%') \n" +
                "and table_type='BASE TABLE' and table_schema='" + tableSchema[0] + "'";
    }

    @Override
    public String getSQLQueryTableSchema(String... args) {
        return "select table_schema FROM information_schema.tables where \"table_name\" not like 'pg_%' or \"table_name\" not like 'sql_%' group by table_schema;";
    }

    @Override
    public String getSQLQueryColumns(String... args) {
        return "SELECT a.attname as name \n" +
                "FROM pg_class as c,pg_attribute as a where c.relname = ? and a.attrelid = c.oid and a.attnum>0";
    }

    @Override
    public String getSQLQueryComment(String schemaName, String tableName, String columnName) {
        return null;
    }
}
