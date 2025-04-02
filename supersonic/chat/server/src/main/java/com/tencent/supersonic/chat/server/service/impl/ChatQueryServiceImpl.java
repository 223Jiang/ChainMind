package com.tencent.supersonic.chat.server.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.executor.ChatQueryExecutor;
import com.tencent.supersonic.chat.server.parser.ChatQueryParser;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.server.processor.parse.ParseResultProcessor;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.jsqlparser.*;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatQueryServiceImpl implements ChatQueryService {

    private final List<ChatQueryParser> chatQueryParsers = ComponentFactory.getChatParsers();
    private final List<ChatQueryExecutor> chatQueryExecutors = ComponentFactory.getChatExecutors();
    private final List<ParseResultProcessor> parseResultProcessors =
            ComponentFactory.getParseProcessors();
    private final List<ExecuteResultProcessor> executeResultProcessors =
            ComponentFactory.getExecuteProcessors();
    @Autowired
    private ChatManageService chatManageService;
    @Autowired
    private ChatLayerService chatLayerService;
    @Autowired
    private SemanticLayerService semanticLayerService;
    @Autowired
    private AgentService agentService;

    @Override
    public List<SearchResult> search(ChatParseReq chatParseReq) {
        ParseContext parseContext = buildParseContext(chatParseReq, null);
        Agent agent = parseContext.getAgent();
        if (!agent.enableSearch()) {
            return Lists.newArrayList();
        }
        QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
        return chatLayerService.retrieve(queryNLReq);
    }

    /**
     * 解析方法，用于处理聊天解析请求
     *
     * @param chatParseReq 聊天解析请求对象，包含了解析所需的查询信息和用户信息
     * @return 返回聊天解析响应对象，包含了解析结果
     */
    @Override
    public ChatParseResp parse(ChatParseReq chatParseReq) {
        // 获取查询ID，如果为空，则创建新的查询记录
        Long queryId = chatParseReq.getQueryId();
        if (Objects.isNull(queryId)) {
            queryId = chatManageService.createChatQuery(chatParseReq);
        }

        // 构建解析上下文，用于在整个解析过程中传递和存储解析相关的数据
        ParseContext parseContext = buildParseContext(chatParseReq, new ChatParseResp(queryId));

        // 遍历所有查询解析器，对查询进行解析
        chatQueryParsers.forEach(p -> p.parse(parseContext));
        // 遍历所有解析结果处理器，对解析结果进行处理
        parseResultProcessors.forEach(p -> p.process(parseContext));

        // 如果不需要用户反馈，则将解析结果批量添加到系统，并更新解析耗时
        if (!parseContext.needFeedback()) {
            chatManageService.batchAddParse(chatParseReq, parseContext.getResponse());
            chatManageService.updateParseCostTime(parseContext.getResponse());
        }

        // 返回解析结果
        return parseContext.getResponse();
    }

    /**
     * 执行聊天查询请求
     * <p>
     * 该方法接收一个聊天执行请求对象，构建执行上下文，并遍历查询执行器集合以执行查询
     * 如果查询成功（即查询结果不为空），则进一步处理执行结果，并保存查询结果
     *
     * @param chatExecuteReq 聊天执行请求对象，包含执行查询所需的信息
     * @return 返回查询结果对象，包含查询执行的结果信息
     */
    @Override
    public QueryResult execute(ChatExecuteReq chatExecuteReq) {
        // 初始化查询结果对象
        QueryResult queryResult = new QueryResult();
        // 构建执行上下文，封装请求参数等信息
        ExecuteContext executeContext = buildExecuteContext(chatExecuteReq);

        // 遍历所有查询执行器，直到找到一个能够处理当前请求的执行器
        for (ChatQueryExecutor chatQueryExecutor : chatQueryExecutors) {
            queryResult = chatQueryExecutor.execute(executeContext);
            // 如果查询结果不为空，则跳出循环，不再尝试其他执行器
            if (queryResult != null) {
                break;
            }
        }

        // 如果查询结果不为空，则对结果进行进一步处理，并保存查询结果
        if (queryResult != null) {
            // 遍历所有结果处理器，对查询结果进行处理
            for (ExecuteResultProcessor processor : executeResultProcessors) {
                processor.process(executeContext, queryResult);
            }
            // 保存查询结果，以便后续可能的审计或回溯
            saveQueryResult(chatExecuteReq, queryResult);
        }

        // 返回查询结果
        return queryResult;
    }

    /**
     * 获取大模型对话
     *
     * @param executeContext 闲聊需要传递参数
     * @return 大模型对话数据
     */
    @Override
    public String largeModelsChatter(String queryText) {
        ChatExecuteReq chatExecuteReq = new ChatExecuteReq();
        chatExecuteReq.setQueryText(queryText);

        ExecuteContext executeContext = new ExecuteContext(chatExecuteReq);

        Agent agent = new Agent();
        agent.setId(3);

        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();

        executeContext.setParseInfo(semanticParseInfo);
        executeContext.setAgent(agent);

        // 初始化查询结果对象
        QueryResult queryResult = new QueryResult();

        // 遍历所有查询执行器，直到找到一个能够处理当前请求的执行器
        for (ChatQueryExecutor chatQueryExecutor : chatQueryExecutors) {
            queryResult = chatQueryExecutor.execute(executeContext);
            // 如果查询结果不为空，则跳出循环，不再尝试其他执行器
            if (queryResult != null) {
                break;
            }
        }

        if (queryResult == null) {
            return null;
        }
        return queryResult.getTextResult();
    }

    @Override
    public QueryResult parseAndExecute(ChatParseReq chatParseReq) {
        ChatParseResp parseResp = parse(chatParseReq);
        if (CollectionUtils.isEmpty(parseResp.getSelectedParses())) {
            log.debug("chatId:{}, agentId:{}, queryText:{}, parseResp.getSelectedParses() is empty",
                    chatParseReq.getChatId(), chatParseReq.getAgentId(),
                    chatParseReq.getQueryText());
            return null;
        }
        ChatExecuteReq executeReq = new ChatExecuteReq();
        executeReq.setQueryId(parseResp.getQueryId());
        executeReq.setParseId(parseResp.getSelectedParses().get(0).getId());
        executeReq.setQueryText(chatParseReq.getQueryText());
        executeReq.setChatId(chatParseReq.getChatId());
        executeReq.setUser(User.getDefaultUser());
        executeReq.setAgentId(chatParseReq.getAgentId());
        executeReq.setSaveAnswer(true);
        return execute(executeReq);
    }

    private ParseContext buildParseContext(ChatParseReq chatParseReq, ChatParseResp chatParseResp) {
        ParseContext parseContext = new ParseContext(chatParseReq, chatParseResp);
        Agent agent = agentService.getAgent(chatParseReq.getAgentId());
        parseContext.setAgent(agent);
        return parseContext;
    }

    /**
     * 构建执行上下文
     * <p>
     * 该方法根据聊天执行请求构建一个执行上下文对象，包括解析信息和代理对象
     * 执行上下文用于后续的执行流程，是连接解析阶段和执行阶段的关键结构
     *
     * @param chatExecuteReq 聊天执行请求，包含构建执行上下文所需的信息
     * @return 返回构建好的执行上下文对象
     */
    private ExecuteContext buildExecuteContext(ChatExecuteReq chatExecuteReq) {
        // 创建执行上下文实例，初始化时传入聊天执行请求
        ExecuteContext executeContext = new ExecuteContext(chatExecuteReq);

        // 从聊天管理服务中获取语义解析信息
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(chatExecuteReq.getQueryId(),
                chatExecuteReq.getParseId());

        // 从代理服务中获取代理对象
        Agent agent = agentService.getAgent(chatExecuteReq.getAgentId());

        // 设置执行上下文中的代理对象
        executeContext.setAgent(agent);

        // 设置执行上下文中的语义解析信息
        executeContext.setParseInfo(parseInfo);

        // 返回构建好的执行上下文
        return executeContext;
    }

    @Override
    public Object queryData(ChatQueryDataReq chatQueryDataReq, User user) throws Exception {
        Integer parseId = chatQueryDataReq.getParseId();
        SemanticParseInfo parseInfo =
                chatManageService.getParseInfo(chatQueryDataReq.getQueryId(), parseId);
        mergeParseInfo(parseInfo, chatQueryDataReq);
        DataSetSchema dataSetSchema =
                semanticLayerService.getDataSetSchema(parseInfo.getDataSetId());

        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        semanticQuery.setParseInfo(parseInfo);

        if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(parseInfo.getQueryMode())) {
            handleLLMQueryMode(chatQueryDataReq, semanticQuery, dataSetSchema, user);
        } else {
            handleRuleQueryMode(semanticQuery, dataSetSchema, user);
        }

        return executeQuery(semanticQuery, user, dataSetSchema);
    }

    private List<String> getFieldsFromSql(SemanticParseInfo parseInfo) {
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (Objects.isNull(sqlInfo) || StringUtils.isNotBlank(sqlInfo.getCorrectedS2SQL())) {
            return new ArrayList<>();
        }
        return SqlSelectHelper.getAllSelectFields(sqlInfo.getCorrectedS2SQL());
    }

    private void handleLLMQueryMode(ChatQueryDataReq chatQueryDataReq, SemanticQuery semanticQuery,
                                    DataSetSchema dataSetSchema, User user) throws Exception {
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        List<String> fields = getFieldsFromSql(parseInfo);
        if (checkMetricReplace(fields, chatQueryDataReq.getMetrics())) {
            log.info("llm begin replace metrics!");
            SchemaElement metricToReplace = chatQueryDataReq.getMetrics().iterator().next();
            replaceMetrics(parseInfo, metricToReplace);
        } else {
            log.info("llm begin revise filters!");
            String correctorSql = reviseCorrectS2SQL(chatQueryDataReq, parseInfo, dataSetSchema);
            parseInfo.getSqlInfo().setCorrectedS2SQL(correctorSql);
            semanticQuery.setParseInfo(parseInfo);
            SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
            SemanticTranslateResp explain = semanticLayerService.translate(semanticQueryReq, user);
            parseInfo.getSqlInfo().setQuerySQL(explain.getQuerySQL());
        }
    }

    private void handleRuleQueryMode(SemanticQuery semanticQuery, DataSetSchema dataSetSchema,
                                     User user) {
        log.info("rule begin replace metrics and revise filters!");
        validFilter(semanticQuery.getParseInfo().getDimensionFilters());
        validFilter(semanticQuery.getParseInfo().getMetricFilters());
        semanticQuery.initS2Sql(dataSetSchema, user);
    }

    private QueryResult executeQuery(SemanticQuery semanticQuery, User user,
                                     DataSetSchema dataSetSchema) throws Exception {
        SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        QueryResult queryResult = doExecution(semanticQueryReq, parseInfo.getQueryMode(), user);
        queryResult.setChatContext(semanticQuery.getParseInfo());
        SemanticLayerService semanticService = ContextUtils.getBean(SemanticLayerService.class);
        EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, dataSetSchema, user);
        queryResult.setEntityInfo(entityInfo);
        parseInfo.getSqlInfo().setQuerySQL(queryResult.getQuerySql());
        return queryResult;
    }

    private boolean checkMetricReplace(List<String> oriFields, Set<SchemaElement> metrics) {
        if (CollectionUtils.isEmpty(oriFields) || CollectionUtils.isEmpty(metrics)) {
            return false;
        }
        List<String> metricNames =
                metrics.stream().map(SchemaElement::getName).collect(Collectors.toList());
        return !oriFields.containsAll(metricNames);
    }

    private String reviseCorrectS2SQL(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
                                      DataSetSchema dataSetSchema) {
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("correctorSql before replacing:{}", correctorSql);
        // get where filter and having filter
        List<FieldExpression> whereExpressionList =
                SqlSelectHelper.getWhereExpressions(correctorSql);

        // replace where filter
        List<Expression> addWhereConditions = new ArrayList<>();
        Set<String> removeWhereFieldNames =
                updateFilters(whereExpressionList, queryData.getDimensionFilters(),
                        parseInfo.getDimensionFilters(), addWhereConditions);

        Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
        Set<String> removeDataFieldNames = updateDateInfo(queryData, parseInfo, dataSetSchema,
                filedNameToValueMap, whereExpressionList, addWhereConditions);
        removeWhereFieldNames.addAll(removeDataFieldNames);

        correctorSql = SqlReplaceHelper.replaceValue(correctorSql, filedNameToValueMap);
        correctorSql = SqlRemoveHelper.removeWhereCondition(correctorSql, removeWhereFieldNames);

        // replace having filter
        List<FieldExpression> havingExpressionList =
                SqlSelectHelper.getHavingExpressions(correctorSql);
        List<Expression> addHavingConditions = new ArrayList<>();
        Set<String> removeHavingFieldNames =
                updateFilters(havingExpressionList, queryData.getDimensionFilters(),
                        parseInfo.getDimensionFilters(), addHavingConditions);
        correctorSql = SqlReplaceHelper.replaceHavingValue(correctorSql, new HashMap<>());
        correctorSql = SqlRemoveHelper.removeHavingCondition(correctorSql, removeHavingFieldNames);

        correctorSql = SqlAddHelper.addWhere(correctorSql, addWhereConditions);
        correctorSql = SqlAddHelper.addHaving(correctorSql, addHavingConditions);
        log.info("correctorSql after replacing:{}", correctorSql);
        return correctorSql;
    }

    private void replaceMetrics(SemanticParseInfo parseInfo, SchemaElement metric) {
        List<String> oriMetrics = parseInfo.getMetrics().stream().map(SchemaElement::getName)
                .collect(Collectors.toList());
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("before replaceMetrics:{}", correctorSql);
        log.info("filteredMetrics:{},metrics:{}", oriMetrics, metric);
        Map<String, Pair<String, String>> fieldMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(oriMetrics) && !oriMetrics.contains(metric.getName())) {
            fieldMap.put(oriMetrics.get(0), Pair.of(metric.getName(), metric.getDefaultAgg()));
            correctorSql = SqlReplaceHelper.replaceAggFields(correctorSql, fieldMap);
        }
        log.info("after replaceMetrics:{}", correctorSql);
        parseInfo.getSqlInfo().setCorrectedS2SQL(correctorSql);
    }

    private QueryResult doExecution(SemanticQueryReq semanticQueryReq, String queryMode, User user)
            throws Exception {
        SemanticQueryResp queryResp = semanticLayerService.queryByReq(semanticQueryReq, user);
        QueryResult queryResult = new QueryResult();

        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
            queryResult.setQuerySql(queryResp.getSql());
            queryResult.setQueryResults(queryResp.getResultList());
            queryResult.setQueryColumns(queryResp.getColumns());
        } else {
            queryResult.setQueryResults(new ArrayList<>());
            queryResult.setQueryColumns(new ArrayList<>());
        }

        queryResult.setQueryMode(queryMode);
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }

    private Set<String> updateDateInfo(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
                                       DataSetSchema dataSetSchema, Map<String, Map<String, String>> filedNameToValueMap,
                                       List<FieldExpression> fieldExpressionList, List<Expression> addConditions) {
        Set<String> removeFieldNames = new HashSet<>();
        if (Objects.isNull(queryData.getDateInfo())) {
            return removeFieldNames;
        }
        if (queryData.getDateInfo().getUnit() > 1) {
            queryData.getDateInfo()
                    .setStartDate(DateUtils.getBeforeDate(queryData.getDateInfo().getUnit() + 1));
            queryData.getDateInfo().setEndDate(DateUtils.getBeforeDate(0));
        }
        SchemaElement partitionDimension = dataSetSchema.getPartitionDimension();
        // startDate equals to endDate
        for (FieldExpression fieldExpression : fieldExpressionList) {
            if (partitionDimension.getName().equals(fieldExpression.getFieldName())) {
                // first remove,then add
                removeFieldNames.add(partitionDimension.getName());
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                addTimeFilters(queryData.getDateInfo().getStartDate(), greaterThanEquals,
                        addConditions, partitionDimension);
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                addTimeFilters(queryData.getDateInfo().getEndDate(), minorThanEquals, addConditions,
                        partitionDimension);
                break;
            }
        }
        for (FieldExpression fieldExpression : fieldExpressionList) {
            for (QueryFilter queryFilter : queryData.getDimensionFilters()) {
                if (queryFilter.getOperator().equals(FilterOperatorEnum.LIKE)
                        && FilterOperatorEnum.LIKE.getValue()
                        .equalsIgnoreCase(fieldExpression.getOperator())) {
                    Map<String, String> replaceMap = new HashMap<>();
                    String preValue = fieldExpression.getFieldValue().toString();
                    String curValue = queryFilter.getValue().toString();
                    if (preValue.startsWith("%")) {
                        curValue = "%" + curValue;
                    }
                    if (preValue.endsWith("%")) {
                        curValue = curValue + "%";
                    }
                    replaceMap.put(preValue, curValue);
                    filedNameToValueMap.put(fieldExpression.getFieldName(), replaceMap);
                    break;
                }
            }
        }
        parseInfo.setDateInfo(queryData.getDateInfo());
        return removeFieldNames;
    }

    private <T extends ComparisonOperator> void addTimeFilters(String date, T comparisonExpression,
                                                               List<Expression> addConditions, SchemaElement partitionDimension) {
        Column column = new Column(partitionDimension.getName());
        StringValue stringValue = new StringValue(date);
        comparisonExpression.setLeftExpression(column);
        comparisonExpression.setRightExpression(stringValue);
        addConditions.add(comparisonExpression);
    }

    private Set<String> updateFilters(List<FieldExpression> fieldExpressionList,
                                      Set<QueryFilter> metricFilters, Set<QueryFilter> contextMetricFilters,
                                      List<Expression> addConditions) {
        Set<String> removeFieldNames = new HashSet<>();
        if (CollectionUtils.isEmpty(metricFilters)) {
            return removeFieldNames;
        }

        for (QueryFilter dslQueryFilter : metricFilters) {
            for (FieldExpression fieldExpression : fieldExpressionList) {
                if (fieldExpression.getFieldName() != null
                        && fieldExpression.getFieldName().contains(dslQueryFilter.getName())) {
                    removeFieldNames.add(dslQueryFilter.getName());
                    handleFilter(dslQueryFilter, contextMetricFilters, addConditions);
                    break;
                }
            }
        }
        return removeFieldNames;
    }

    private void handleFilter(QueryFilter dslQueryFilter, Set<QueryFilter> contextMetricFilters,
                              List<Expression> addConditions) {
        FilterOperatorEnum operator = dslQueryFilter.getOperator();

        if (operator == FilterOperatorEnum.IN) {
            addWhereInFilters(dslQueryFilter, new InExpression(), contextMetricFilters,
                    addConditions);
        } else {
            ComparisonOperator expression = FilterOperatorEnum.createExpression(operator);
            if (Objects.nonNull(expression)) {
                addWhereFilters(dslQueryFilter, expression, contextMetricFilters, addConditions);
            }
        }
    }

    // add in condition to sql where condition
    private void addWhereInFilters(QueryFilter dslQueryFilter, InExpression inExpression,
                                   Set<QueryFilter> contextMetricFilters, List<Expression> addConditions) {
        Column column = new Column(dslQueryFilter.getName());
        ParenthesedExpressionList parenthesedExpressionList = new ParenthesedExpressionList<>();
        List<String> valueList =
                JsonUtil.toList(JsonUtil.toString(dslQueryFilter.getValue()), String.class);
        if (CollectionUtils.isEmpty(valueList)) {
            return;
        }
        valueList.forEach(o -> {
            StringValue stringValue = new StringValue(o);
            parenthesedExpressionList.add(stringValue);
        });
        inExpression.setLeftExpression(column);
        inExpression.setRightExpression(parenthesedExpressionList);
        addConditions.add(inExpression);
        contextMetricFilters.forEach(o -> {
            if (o.getName().equals(dslQueryFilter.getName())) {
                o.setValue(dslQueryFilter.getValue());
                o.setOperator(dslQueryFilter.getOperator());
            }
        });
    }

    // add where filter
    private void addWhereFilters(QueryFilter dslQueryFilter,
                                 ComparisonOperator comparisonExpression, Set<QueryFilter> contextMetricFilters,
                                 List<Expression> addConditions) {
        String columnName = dslQueryFilter.getName();
        if (StringUtils.isNotBlank(dslQueryFilter.getFunction())) {
            columnName = dslQueryFilter.getFunction() + "(" + dslQueryFilter.getName() + ")";
        }
        if (Objects.isNull(dslQueryFilter.getValue())) {
            return;
        }
        Column column = new Column(columnName);
        comparisonExpression.setLeftExpression(column);
        if (StringUtils.isNumeric(dslQueryFilter.getValue().toString())) {
            LongValue longValue =
                    new LongValue(Long.parseLong(dslQueryFilter.getValue().toString()));
            comparisonExpression.setRightExpression(longValue);
        } else {
            StringValue stringValue = new StringValue(dslQueryFilter.getValue().toString());
            comparisonExpression.setRightExpression(stringValue);
        }
        addConditions.add(comparisonExpression);
        contextMetricFilters.forEach(o -> {
            if (o.getName().equals(dslQueryFilter.getName())) {
                o.setValue(dslQueryFilter.getValue());
                o.setOperator(dslQueryFilter.getOperator());
            }
        });
    }

    private void mergeParseInfo(SemanticParseInfo parseInfo, ChatQueryDataReq queryData) {
        if (LLMSqlQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            return;
        }
        if (!CollectionUtils.isEmpty(queryData.getDimensions())) {
            parseInfo.setDimensions(queryData.getDimensions());
        }
        if (!CollectionUtils.isEmpty(queryData.getMetrics())) {
            parseInfo.setMetrics(queryData.getMetrics());
        }
        if (!CollectionUtils.isEmpty(queryData.getDimensionFilters())) {
            parseInfo.setDimensionFilters(queryData.getDimensionFilters());
        }
        if (!CollectionUtils.isEmpty(queryData.getMetricFilters())) {
            parseInfo.setMetricFilters(queryData.getMetricFilters());
        }
        if (Objects.nonNull(queryData.getDateInfo())) {
            parseInfo.setDateInfo(queryData.getDateInfo());
        }
        parseInfo.setSqlInfo(new SqlInfo());
    }

    private void validFilter(Set<QueryFilter> filters) {
        Iterator<QueryFilter> iterator = filters.iterator();
        while (iterator.hasNext()) {
            QueryFilter queryFilter = iterator.next();
            Object queryFilterValue = queryFilter.getValue();
            if (Objects.isNull(queryFilterValue)) {
                iterator.remove();
                continue;
            }
            List<String> collection = new ArrayList<>();
            if (queryFilterValue instanceof List) {
                collection.addAll((List) queryFilterValue);
            } else if (queryFilterValue instanceof String) {
                collection.add((String) queryFilterValue);
            }
            if (FilterOperatorEnum.IN.equals(queryFilter.getOperator())
                    && CollectionUtils.isEmpty(collection)) {
                iterator.remove();
            }
        }
    }

    @Override
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) {
        Integer agentId = dimensionValueReq.getAgentId();
        Agent agent = agentService.getAgent(agentId);
        dimensionValueReq.setDataSetIds(agent.getDataSetIds());
        return semanticLayerService.queryDimensionValue(dimensionValueReq, user);
    }

    public void saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult) {
        // The history record only retains the query result of the first parse
        if (chatExecuteReq.getParseId() > 1) {
            return;
        }
        chatManageService.saveQueryResult(chatExecuteReq, queryResult);
    }
}
