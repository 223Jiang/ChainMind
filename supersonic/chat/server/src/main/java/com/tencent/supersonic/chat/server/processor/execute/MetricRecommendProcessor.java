package com.tencent.supersonic.chat.server.processor.execute;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.knowledge.MetaEmbeddingService;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MetricRecommendProcessor fills recommended metrics based on embedding similarity.
 **/
public class MetricRecommendProcessor implements ExecuteResultProcessor {

    private static final int METRIC_RECOMMEND_SIZE = 5;

    @Override
    public void process(ExecuteContext executeContext, QueryResult queryResult) {
        fillSimilarMetric(executeContext.getParseInfo());
    }

    /**
     * 根据相似性填充指标信息
     * 当查询类型为聚合查询且指标数量不超过推荐大小时，本方法会尝试寻找并添加相似的指标
     *
     * @param parseInfo 解析信息对象，包含查询类型、指标等信息
     */
    private void fillSimilarMetric(SemanticParseInfo parseInfo) {
        // 检查是否满足执行条件：查询类型为聚合、指标数量不超过推荐大小且指标列表非空
        if (!parseInfo.getQueryType().equals(QueryType.AGGREGATE)
                || parseInfo.getMetrics().size() > METRIC_RECOMMEND_SIZE
                || CollectionUtils.isEmpty(parseInfo.getMetrics())) {
            return;
        }
        // 获取首个指标的名称，用于后续的相似指标查询
        List<String> metricNames =
                Collections.singletonList(parseInfo.getMetrics().iterator().next().getName());
        // 准备过滤条件，包括模型ID和类型
        Map<String, Object> filterCondition = new HashMap<>();
        filterCondition.put("modelId",
                parseInfo.getMetrics().iterator().next().getDataSetId().toString());
        filterCondition.put("type", SchemaElementType.METRIC.name());
        // 构建检索查询对象
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(metricNames)
                .filterCondition(filterCondition).queryEmbeddings(null).build();
        // 获取元数据嵌入服务实例
        MetaEmbeddingService metaEmbeddingService =
                ContextUtils.getBean(MetaEmbeddingService.class);
        // 执行检索查询，获取结果
        List<RetrieveQueryResult> retrieveQueryResults = metaEmbeddingService.retrieveQuery(
                retrieveQuery, METRIC_RECOMMEND_SIZE + 1, new HashMap<>(), new HashSet<>());
        if (CollectionUtils.isEmpty(retrieveQueryResults)) {
            return;
        }
        // 处理查询结果，按相似度排序并去重
        List<Retrieval> retrievals = retrieveQueryResults.stream()
                .flatMap(retrieveQueryResult -> retrieveQueryResult.getRetrieval().stream())
                .sorted(Comparator.comparingDouble(Retrieval::getSimilarity)).distinct()
                .collect(Collectors.toList());
        // 获取已有的指标ID集合，用于后续判断是否添加新指标
        Set<Long> metricIds = parseInfo.getMetrics().stream().map(SchemaElement::getId)
                .collect(Collectors.toSet());
        // 为现有指标设置顺序
        int metricOrder = 0;
        for (SchemaElement metric : parseInfo.getMetrics()) {
            metric.setOrder(metricOrder++);
        }
        // 遍历相似指标，添加符合条件的新指标
        for (Retrieval retrieval : retrievals) {
            if (!metricIds.contains(Retrieval.getLongId(retrieval.getId()))) {
                if (Objects.nonNull(retrieval.getMetadata().get("id"))) {
                    String idStr = retrieval.getMetadata().get("id").toString()
                            .replaceAll(DictWordType.NATURE_SPILT, "");
                    retrieval.getMetadata().put("id", idStr);
                }
                String metaStr = JSONObject.toJSONString(retrieval.getMetadata());
                SchemaElement schemaElement = JSONObject.parseObject(metaStr, SchemaElement.class);
                if (retrieval.getMetadata().containsKey("dataSetId")) {
                    String dataSetId = retrieval.getMetadata().get("dataSetId").toString()
                            .replace(Constants.UNDERLINE, "");
                    schemaElement.setDataSetId(Long.parseLong(dataSetId));
                }
                schemaElement.setOrder(++metricOrder);
                parseInfo.getMetrics().add(schemaElement);
            }
        }
    }
}
