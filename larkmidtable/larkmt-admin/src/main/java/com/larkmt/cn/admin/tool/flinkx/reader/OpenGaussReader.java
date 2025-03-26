package com.larkmt.cn.admin.tool.flinkx.reader;

import java.util.Map;

/**
 * OpenGauss reader 构建类
 */
public class OpenGaussReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "opengaussreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}