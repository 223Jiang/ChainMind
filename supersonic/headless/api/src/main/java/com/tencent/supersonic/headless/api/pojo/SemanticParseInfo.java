package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterType;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.tencent.supersonic.common.pojo.Constants.DEFAULT_DETAIL_LIMIT;
import static com.tencent.supersonic.common.pojo.Constants.DEFAULT_METRIC_LIMIT;

@Data
public class SemanticParseInfo implements Serializable {

    private Integer id;
    private String queryMode = "PLAIN_TEXT";
    private SchemaElement dataSet;
    private QueryConfig queryConfig;
    private Set<SchemaElement> metrics = Sets.newTreeSet(new SchemaNameLengthComparator());
    private Set<SchemaElement> dimensions = Sets.newTreeSet(new SchemaNameLengthComparator());
    private SchemaElement entity;
    private AggregateTypeEnum aggType = AggregateTypeEnum.NONE;
    private FilterType filterType = FilterType.AND;
    private Set<QueryFilter> dimensionFilters = Sets.newHashSet();
    private Set<QueryFilter> metricFilters = Sets.newHashSet();
    private Set<Order> orders = Sets.newHashSet();
    private DateConf dateInfo;
    private long limit = DEFAULT_DETAIL_LIMIT;
    private double score;
    private List<SchemaElementMatch> elementMatches = Lists.newArrayList();
    private SqlInfo sqlInfo = new SqlInfo();
    private SqlEvaluation sqlEvaluation = new SqlEvaluation();
    private QueryType queryType = QueryType.ID;
    private EntityInfo entityInfo;
    private String textInfo;
    private Map<String, Object> properties = Maps.newHashMap();

    @Data
    @Builder
    public static class DataSetMatchResult {
        private double maxMetricSimilarity;
        private double maxDatesetSimilarity;
        private double totalSimilarity;
        private long maxMetricUseCnt;
    }

    public static class SemanticParseComparator implements Comparator<SemanticParseInfo> {
        @Override
        public int compare(SemanticParseInfo o1, SemanticParseInfo o2) {
            DataSetMatchResult mr1 = getDataSetMatchResult(o1.getElementMatches());
            DataSetMatchResult mr2 = getDataSetMatchResult(o2.getElementMatches());

            double difference = mr1.getMaxDatesetSimilarity() - mr2.getMaxDatesetSimilarity();
            if (difference == 0) {
                difference = mr1.getMaxMetricSimilarity() - mr2.getMaxMetricSimilarity();
                if (difference == 0) {
                    difference = mr1.getTotalSimilarity() - mr2.getTotalSimilarity();
                }
                if (difference == 0) {
                    difference = mr1.getMaxMetricUseCnt() - mr2.getMaxMetricUseCnt();
                }
            }
            return difference >= 0 ? -1 : 1;
        }

        private DataSetMatchResult getDataSetMatchResult(List<SchemaElementMatch> elementMatches) {
            double maxMetricSimilarity = 0;
            double maxDatasetSimilarity = 0;
            double totalSimilarity = 0;
            long maxMetricUseCnt = 0L;
            for (SchemaElementMatch match : elementMatches) {
                if (SchemaElementType.DATASET.equals(match.getElement().getType())) {
                    maxDatasetSimilarity = Math.max(maxDatasetSimilarity, match.getSimilarity());
                }
                if (SchemaElementType.METRIC.equals(match.getElement().getType())) {
                    maxMetricSimilarity = Math.max(maxMetricSimilarity, match.getSimilarity());
                    if (Objects.nonNull(match.getElement().getUseCnt())) {
                        maxMetricUseCnt = Math.max(maxMetricUseCnt, match.getElement().getUseCnt());
                    }
                }
                totalSimilarity += match.getSimilarity();
            }
            return DataSetMatchResult.builder().maxMetricSimilarity(maxMetricSimilarity)
                    .maxDatesetSimilarity(maxDatasetSimilarity).totalSimilarity(totalSimilarity)
                    .build();
        }
    }

    public static void sort(List<SemanticParseInfo> parses) {
        parses.sort(new SemanticParseComparator());
        // re-assign parseId
        for (int i = 0; i < parses.size(); i++) {
            SemanticParseInfo parseInfo = parses.get(i);
            parseInfo.setId(i + 1);
        }
    }

    private static class SchemaNameLengthComparator implements Comparator<SchemaElement> {
        @Override
        public int compare(SchemaElement o1, SchemaElement o2) {
            if (o1.getOrder() != o2.getOrder()) {
                if (o1.getOrder() < o2.getOrder()) {
                    return -1;
                } else {
                    return 1;
                }
            }
            int len1 = o1.getName().length();
            int len2 = o2.getName().length();
            if (len1 != len2) {
                return len1 - len2;
            } else {
                return o1.getName().compareTo(o2.getName());
            }
        }
    }

    public Long getDataSetId() {
        if (dataSet == null) {
            return null;
        }
        return dataSet.getDataSetId();
    }

    public long getDetailLimit() {
        long limit = DEFAULT_DETAIL_LIMIT;
        if (Objects.nonNull(queryConfig)
                && Objects.nonNull(queryConfig.getDetailTypeDefaultConfig())
                && Objects.nonNull(queryConfig.getDetailTypeDefaultConfig().getLimit())) {
            limit = queryConfig.getDetailTypeDefaultConfig().getLimit();
        }
        return limit;
    }

    public long getMetricLimit() {
        long limit = DEFAULT_METRIC_LIMIT;
        if (Objects.nonNull(queryConfig)
                && Objects.nonNull(queryConfig.getAggregateTypeDefaultConfig())
                && Objects.nonNull(queryConfig.getAggregateTypeDefaultConfig().getLimit())) {
            limit = queryConfig.getAggregateTypeDefaultConfig().getLimit();
        }
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SemanticParseInfo that = (SemanticParseInfo) o;
        return Objects.equals(textInfo, that.textInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(textInfo);
    }
}
