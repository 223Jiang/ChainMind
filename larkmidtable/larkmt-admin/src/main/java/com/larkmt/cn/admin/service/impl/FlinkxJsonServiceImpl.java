package com.larkmt.cn.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.larkmt.cn.admin.dto.FlinkXJsonBuildDto;
import com.larkmt.cn.admin.entity.JobDatasource;
import com.larkmt.cn.admin.service.FlinkxJsonService;
import com.larkmt.cn.admin.service.JobDatasourceService;
import com.larkmt.cn.admin.tool.flinkx.FlinkxJsonHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *
 * @Author: LarkMidTable
 * @Date: 2020/9/16 11:14
 * @Description:  JSON构建实现类
 **/
@Service
public class FlinkxJsonServiceImpl implements FlinkxJsonService {

    @Resource
    private JobDatasourceService jobJdbcDatasourceService;

    @Override
    public String buildJobJson(FlinkXJsonBuildDto FlinkXJsonBuildDto) {
        FlinkxJsonHelper flinkxJsonHelper = new FlinkxJsonHelper();
        // reader
        JobDatasource readerDatasource = jobJdbcDatasourceService.getById(FlinkXJsonBuildDto.getReaderDatasourceId());
        buildJobJson(readerDatasource);
        flinkxJsonHelper.initReader(FlinkXJsonBuildDto, readerDatasource);
        // writer
        JobDatasource writerDatasource = jobJdbcDatasourceService.getById(FlinkXJsonBuildDto.getWriterDatasourceId());
        buildJobJson(writerDatasource);
        flinkxJsonHelper.initWriter(FlinkXJsonBuildDto, writerDatasource);

        return JSON.toJSONString(flinkxJsonHelper.buildJob());
    }

    /**
     * 将opengauss进行postgresql适配
     */
    public JobDatasource buildJobJson(JobDatasource jobDatasource) {
        if (jobDatasource.getDatasource().equals("opengauss")) {
            jobDatasource.setDatasource("postgresql");
            jobDatasource.setDatabaseName("postgresql");
            jobDatasource.setJdbcUrl(jobDatasource.getJdbcUrl().replace("opengauss", "postgresql"));
            jobDatasource.setJdbcDriverClass("org.postgresql.Driver");
        }

        return jobDatasource;
    }
}
