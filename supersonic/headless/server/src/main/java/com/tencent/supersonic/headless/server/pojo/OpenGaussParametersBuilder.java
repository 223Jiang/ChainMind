package com.tencent.supersonic.headless.server.pojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OpenGaussParametersBuilder extends PostgresqlParametersBuilder {
    // 继承PostgreSQL参数配置，保持相同参数结构
    @Override
    public List<DatabaseParameter> build() {
        return super.build();
    }
}
