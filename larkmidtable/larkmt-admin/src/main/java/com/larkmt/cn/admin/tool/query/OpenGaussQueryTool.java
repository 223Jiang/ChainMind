package com.larkmt.cn.admin.tool.query;

import com.larkmt.cn.admin.entity.JobDatasource;

import java.sql.SQLException;

public class OpenGaussQueryTool extends PostgresqlQueryTool {

    public OpenGaussQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}