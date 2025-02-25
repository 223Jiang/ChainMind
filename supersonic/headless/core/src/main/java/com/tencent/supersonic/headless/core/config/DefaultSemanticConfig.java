package com.tencent.supersonic.headless.core.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class DefaultSemanticConfig {

    @Value("${s2.semantic.url.prefix:http://localhost:8081}")
    private String semanticUrl;

    @Value("${s2.searchByStruct.path:/supersonic/api/semantic/query/struct}")
    private String searchByStructPath;

    @Value("${s2.searchByStruct.path:/supersonic/api/semantic/query/multiStruct}")
    private String searchByMultiStructPath;

    @Value("${s2.searchByStruct.path:/supersonic/api/semantic/query/sql}")
    private String searchBySqlPath;

    @Value("${s2.searchByStruct.path:/supersonic/api/semantic/query/queryDimValue}")
    private String queryDimValuePath;

    @Value("${s2.fetchModelSchemaPath.path:/supersonic/api/semantic/schema}")
    private String fetchModelSchemaPath;

    @Value("${s2.fetchModelList.path:/supersonic/api/semantic/schema/dimension/page}")
    private String fetchDimensionPagePath;

    @Value("${s2.fetchModelList.path:/supersonic/api/semantic/schema/metric/page}")
    private String fetchMetricPagePath;

    @Value("${s2.fetchModelList.path:/supersonic/api/semantic/schema/domain/list}")
    private String fetchDomainListPath;

    @Value("${s2.fetchModelList.path:/supersonic/api/semantic/schema/model/list}")
    private String fetchModelListPath;

    @Value("${s2.explain.path:/supersonic/api/semantic/query/explain}")
    private String explainPath;
}
