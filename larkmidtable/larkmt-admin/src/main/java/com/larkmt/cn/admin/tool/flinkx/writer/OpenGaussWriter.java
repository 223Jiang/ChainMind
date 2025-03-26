package com.larkmt.cn.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * OpenGauss writer 构建类
 */
public class OpenGaussWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "opengausswriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}