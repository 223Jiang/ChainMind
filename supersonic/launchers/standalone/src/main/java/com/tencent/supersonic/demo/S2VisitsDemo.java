package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.DatasetTool;
import com.tencent.supersonic.chat.server.agent.ToolConfig;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.chat.server.plugin.build.webpage.WebPageQuery;
import com.tencent.supersonic.chat.server.plugin.build.webservice.WebServiceQuery;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.*;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.*;
import com.tencent.supersonic.headless.api.pojo.request.*;
import com.tencent.supersonic.headless.api.pojo.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@Order(1)
public class S2VisitsDemo extends S2BaseDemo {

    @Override
    public void doRun() {
        try {
            // create domain
            DomainResp s2Domain = addDomain();
            TagObjectResp s2TagObject = addTagObjectUser(s2Domain);

            // create models
            ModelResp userModel = addUserDepartmentModel(s2Domain, demoDatabase, s2TagObject);
            ModelResp pvUvModel = addPvUvModel(s2Domain, demoDatabase);
            ModelResp stayTimeModel = addModel_3(s2Domain, demoDatabase);
            addModelRela_1(s2Domain, userModel, pvUvModel);
            addModelRela_2(s2Domain, userModel, stayTimeModel);
            addTags(userModel);

            // create metrics and dimensions
            DimensionResp departmentDimension = getDimension("department", userModel);
            MetricResp metricUv = addMetric_uv(pvUvModel, departmentDimension);
            MetricResp metricPv = getMetric("pv", pvUvModel);
            addMetric_pv_avg(metricPv, metricUv, departmentDimension, pvUvModel);

            DimensionResp pageDimension = getDimension("page", stayTimeModel);
            updateDimension(stayTimeModel, pageDimension);
            DimensionResp userDimension = getDimension("user_name", userModel);
            updateMetric(stayTimeModel, departmentDimension, userDimension);
            updateMetric_pv(pvUvModel, departmentDimension, userDimension, metricPv);

            // create dict conf for dimensions
            createDictConf(departmentDimension);
            createDictConf(userDimension);

            // create data set
            DataSetResp s2DataSet = addDataSet(s2Domain);
            addAuthGroup_1(stayTimeModel);
            addAuthGroup_2(pvUvModel);

            // create terms and plugin
            addTerm(s2Domain);
            addTerm_1(s2Domain);
            addPlugin(s2DataSet);
            addPlugin_1();

            // load dict word
            loadDictWord();

            // create agent
            Integer agentId = addAgent(s2DataSet.getId());
            addSampleChats(agentId);
            updateQueryScore(1);
            updateQueryScore(4);
        } catch (Exception e) {
            log.error("Failed to add S2Visits demo data", e);
        }
    }

    @Override
    boolean checkNeedToRun() {
        List<DomainResp> domainList = domainService.getDomainList();
        for (DomainResp domainResp : domainList) {
            if (domainResp.getBizName().equalsIgnoreCase("supersonic")) {
                log.info("Already exist domain:supersonic, no need to run demo");
                return false;
            }
        }
        return true;
    }

    public void addSampleChats(Integer agentId) {
        Long chatId = chatManageService.addChat(defaultUser, "样例对话1", agentId);
        submitText(chatId.intValue(), agentId, "超音数 访问次数");
        submitText(chatId.intValue(), agentId, "按部门统计近7天访问次数");
        submitText(chatId.intValue(), agentId, "alice 停留时长");
        submitText(chatId.intValue(), agentId, "访问次数最高的部门");
    }

    private void submitText(int chatId, int agentId, String queryText) {
        chatQueryService.parseAndExecute(ChatParseReq.builder().chatId(chatId).agentId(agentId)
                .queryText(queryText).user(defaultUser).disableLLM(true).build());
    }

    private Integer addAgent(Long dataSetId) {
        Agent agent = new Agent();
        agent.setName("算指标");
        agent.setDescription("帮助您用自然语言查询指标，支持时间限定、条件筛选、下钻维度以及聚合统计");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("近15天超音数访问次数汇总", "按部门统计超音数的访问人数", "对比alice和lucy的停留时长",
                "过去30天访问次数最高的部门top3", "近1个月总访问次数超过100次的部门有几个", "过去半个月每个核心用户的总停留时长"));

        // configure tools
        ToolConfig toolConfig = new ToolConfig();
        DatasetTool datasetTool = new DatasetTool();
        datasetTool.setId("1");
        datasetTool.setType(AgentToolType.DATASET);
        datasetTool.setDataSetIds(Lists.newArrayList(dataSetId));
        if (toolConfig.getTools() == null) {
            toolConfig.setTools(new ArrayList<>());
        }
        toolConfig.getTools().add(datasetTool);
        agent.setToolConfig(JSONObject.toJSONString(toolConfig));

        // configure chat apps
        Map<String, ChatApp> chatAppConfig =
                Maps.newHashMap(ChatAppManager.getAllApps(AppModule.CHAT));
        chatAppConfig.values().forEach(app -> app.setChatModelId(demoChatModel.getId()));
        agent.setChatAppConfig(chatAppConfig);
        Agent agentCreated = agentService.createAgent(agent, defaultUser);
        return agentCreated.getId();
    }

    public DomainResp addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("超音数");
        domainReq.setBizName("supersonic");
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom"));
        domainReq.setAdmins(Arrays.asList("admin", "jack"));
        domainReq.setIsOpen(1);
        return domainService.createDomain(domainReq, defaultUser);
    }

    public ModelResp addUserDepartmentModel(DomainResp s2Domain, DatabaseResp s2Database,
            TagObjectResp s2TagObject) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("用户部门");
        modelReq.setBizName("user_department");
        modelReq.setDescription("用户部门信息");
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setTagObjectId(s2TagObject.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Arrays.asList("admin", "alice"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户", IdentifyType.primary.name(), "user_name", 1));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        dimensions.add(new Dim("部门", "department", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("department").dataType("Varchar").build());
        modelDetail.setFields(fields);
        modelDetail.setMeasures(Collections.emptyList());
        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("select user_name,department from s2_user_department");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, defaultUser);
    }

    public ModelResp addPvUvModel(DomainResp s2Domain, DatabaseResp s2Database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("PVUV统计");
        modelReq.setBizName("s2_pv_uv_statis");
        modelReq.setDescription("PVUV统计");
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        List<Identify> identifiers = new ArrayList<>();
        ModelDetail modelDetail = new ModelDetail();
        identifiers.add(new Identify("用户名", IdentifyType.foreign.name(), "user_name", 0));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("日期", "imp_date", DimensionType.partition_time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("页面", "page", DimensionType.categorical.name(), 0);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);
        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数", "pv", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure1);
        Measure measure2 = new Measure("访问用户数", "user_id", AggOperatorEnum.SUM.name(), 0);
        measures.add(measure2);
        modelDetail.setMeasures(measures);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("imp_date").dataType("Date").build());
        fields.add(Field.builder().fieldName("page").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("pv").dataType("Long").build());
        fields.add(Field.builder().fieldName("user_id").dataType("Varchar").build());
        modelDetail.setFields(fields);
        modelDetail.setSqlQuery("SELECT imp_date, user_name, page, 1 as pv, "
                + "user_name as user_id FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, defaultUser);
    }

    public ModelResp addModel_3(DomainResp s2Domain, DatabaseResp s2Database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("停留时长统计");
        modelReq.setBizName("s2_stay_time_statis");
        modelReq.setDescription("停留时长统计");
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        List<Identify> identifiers = new ArrayList<>();
        ModelDetail modelDetail = new ModelDetail();
        identifiers.add(new Identify("用户", IdentifyType.foreign.name(), "user_name", 0));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("数据日期", "imp_date", DimensionType.partition_time.name(), 1);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("页面", "page", DimensionType.categorical.name(), 1);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("停留时长", "stay_hours", AggregateTypeEnum.SUM.name(), 1);
        measures.add(measure1);
        modelDetail.setMeasures(measures);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("imp_date").dataType("Date").build());
        fields.add(Field.builder().fieldName("page").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("stay_hours").dataType("Double").build());
        modelDetail.setFields(fields);
        modelDetail
                .setSqlQuery("select imp_date,user_name,stay_hours,page from s2_stay_time_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, defaultUser);
    }

    public void addModelRela_1(DomainResp s2Domain, ModelResp userDepartmentModel,
            ModelResp pvUvModel) {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("user_name", "user_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(s2Domain.getId());
        modelRelaReq.setFromModelId(userDepartmentModel.getId());
        modelRelaReq.setToModelId(pvUvModel.getId());
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, defaultUser);
    }

    public void addModelRela_2(DomainResp s2Domain, ModelResp userDepartmentModel,
            ModelResp stayTimeModel) {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("user_name", "user_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(s2Domain.getId());
        modelRelaReq.setFromModelId(userDepartmentModel.getId());
        modelRelaReq.setToModelId(stayTimeModel.getId());
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, defaultUser);
    }

    private void addTags(ModelResp model) {
        addTag(dimensionService.getDimension("department", model.getId()).getId(),
                TagDefineType.DIMENSION);
    }

    public void updateDimension(ModelResp stayTimeModel, DimensionResp pageDimension)
            throws Exception {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setType(DimensionType.categorical.name());
        dimensionReq.setId(pageDimension.getId());
        dimensionReq.setName("页面");
        dimensionReq.setBizName("page");
        dimensionReq.setModelId(stayTimeModel.getId());
        dimensionReq.setAlias("page");
        dimensionReq.setSemanticType(SemanticType.CATEGORY.name());
        dimensionReq.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        dimensionReq.setDescription("页面");
        dimensionReq.setExpr("page");
        dimensionReq.setDimValueMaps(Collections.emptyList());
        dimensionService.updateDimension(dimensionReq, defaultUser);
    }

    private void createDictConf(DimensionResp dimension) {
        dictConfService.addDictConf(DictItemReq.builder().type(TypeEnums.DIMENSION)
                .itemId(dimension.getId()).status(StatusEnum.ONLINE).build(), defaultUser);
        dictTaskService.addDictTask(DictSingleTaskReq.builder().itemId(dimension.getId())
                .type(TypeEnums.DIMENSION).build(), defaultUser);
    }

    public void updateMetric(ModelResp stayTimeModel, DimensionResp departmentDimension,
            DimensionResp userDimension) throws Exception {
        MetricResp stayHoursMetric = metricService.getMetric(stayTimeModel.getId(), "stay_hours");
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(stayTimeModel.getId());
        metricReq.setId(stayHoursMetric.getId());
        metricReq.setName("停留时长");
        metricReq.setBizName("stay_hours");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        metricReq.setDescription("停留时长");
        metricReq.setClassifications(Collections.singletonList("核心指标"));
        MetricDefineByMeasureParams metricTypeParams = new MetricDefineByMeasureParams();
        metricTypeParams.setExpr("s2_stay_time_statis_stay_hours");
        List<MeasureParam> measures = new ArrayList<>();
        MeasureParam measure = new MeasureParam("s2_stay_time_statis_stay_hours", "",
                AggOperatorEnum.SUM.getOperator());
        measures.add(measure);
        metricTypeParams.setMeasures(measures);
        metricReq.setMetricDefineByMeasureParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.MEASURE);
        metricReq.setRelateDimension(getRelateDimension(
                Lists.newArrayList(departmentDimension.getId(), userDimension.getId())));
        metricService.updateMetric(metricReq, defaultUser);
    }

    public void updateMetric_pv(ModelResp pvUvModel, DimensionResp departmentDimension,
            DimensionResp userDimension, MetricResp metricPv) throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(pvUvModel.getId());
        metricReq.setId(metricPv.getId());
        metricReq.setName("访问次数");
        metricReq.setBizName("pv");
        metricReq.setDescription("一段时间内用户的访问次数");
        MetricDefineByMeasureParams metricTypeParams = new MetricDefineByMeasureParams();
        metricTypeParams.setExpr("s2_pv_uv_statis_pv");
        List<MeasureParam> measures = new ArrayList<>();
        MeasureParam measure =
                new MeasureParam("s2_pv_uv_statis_pv", "", AggOperatorEnum.SUM.getOperator());
        measures.add(measure);
        metricTypeParams.setMeasures(measures);
        metricReq.setMetricDefineByMeasureParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.MEASURE);
        metricReq.setRelateDimension(getRelateDimension(
                Lists.newArrayList(departmentDimension.getId(), userDimension.getId())));
        metricService.updateMetric(metricReq, defaultUser);
    }

    public MetricResp addMetric_uv(ModelResp uvModel, DimensionResp departmentDimension)
            throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(uvModel.getId());
        metricReq.setName("访问用户数");
        metricReq.setBizName("uv");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.LOW.getCode());
        metricReq.setDescription("访问的用户个数");
        metricReq.setAlias("UV,访问人数");
        MetricDefineByFieldParams metricTypeParams = new MetricDefineByFieldParams();
        metricTypeParams.setExpr("count(distinct user_id)");
        List<FieldParam> fieldParams = new ArrayList<>();
        fieldParams.add(new FieldParam("user_id"));
        metricTypeParams.setFields(fieldParams);
        metricReq.setMetricDefineByFieldParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.FIELD);
        metricReq.setRelateDimension(
                getRelateDimension(Lists.newArrayList(departmentDimension.getId())));
        return metricService.createMetric(metricReq, defaultUser);
    }

    public MetricResp addMetric_pv_avg(MetricResp metricPv, MetricResp metricUv,
            DimensionResp departmentDimension, ModelResp pvModel) throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(pvModel.getId());
        metricReq.setName("人均访问次数");
        metricReq.setBizName("pv_avg");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        metricReq.setDescription("每个用户平均访问的次数");
        metricReq.setClassifications(Collections.singletonList("核心指标"));
        metricReq.setAlias("平均访问次数");
        MetricDefineByMetricParams metricTypeParams = new MetricDefineByMetricParams();
        metricTypeParams.setExpr("pv/uv");
        List<MetricParam> metrics = new ArrayList<>();
        MetricParam pv = new MetricParam(metricPv.getId(), metricPv.getBizName());
        MetricParam uv = new MetricParam(metricUv.getId(), metricUv.getBizName());
        metrics.add(pv);
        metrics.add(uv);
        metricTypeParams.setMetrics(metrics);
        metricReq.setMetricDefineByMetricParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.METRIC);
        metricReq.setRelateDimension(
                getRelateDimension(Lists.newArrayList(departmentDimension.getId())));
        return metricService.createMetric(metricReq, defaultUser);
    }

    public DataSetResp addDataSet(DomainResp s2Domain) {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("超音数数据集");
        dataSetReq.setBizName("s2");
        dataSetReq.setDomainId(s2Domain.getId());
        dataSetReq.setDescription("包含超音数访问统计相关的指标和维度等");
        dataSetReq.setAdmins(Lists.newArrayList("admin"));
        List<DataSetModelConfig> dataSetModelConfigs = getDataSetModelConfigs(s2Domain.getId());
        DataSetDetail dataSetDetail = new DataSetDetail();
        dataSetDetail.setDataSetModelConfigs(dataSetModelConfigs);
        dataSetReq.setDataSetDetail(dataSetDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);
        return dataSetService.save(dataSetReq, defaultUser);
    }

    public void addTerm(DomainResp s2Domain) {
        TermReq termReq = new TermReq();
        termReq.setName("近期");
        termReq.setDescription("指近10天");
        termReq.setAlias(Lists.newArrayList("近一段时间"));
        termReq.setDomainId(s2Domain.getId());
        termService.saveOrUpdate(termReq, defaultUser);
    }

    public void addTerm_1(DomainResp s2Domain) {
        TermReq termReq = new TermReq();
        termReq.setName("核心用户");
        termReq.setDescription("用户为tom和lucy");
        termReq.setAlias(Lists.newArrayList("VIP用户"));
        termReq.setDomainId(s2Domain.getId());
        termService.saveOrUpdate(termReq, defaultUser);
    }

    public void addAuthGroup_1(ModelResp stayTimeModel) {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId(stayTimeModel.getId());
        authGroupReq.setName("jack_column_permission");

        List<AuthRule> authRules = new ArrayList<>();
        AuthRule authRule = new AuthRule();
        authRule.setMetrics(Collections.singletonList("stay_hours"));
        authRule.setDimensions(Collections.singletonList("page"));
        authRules.add(authRule);

        authGroupReq.setAuthRules(authRules);
        authGroupReq.setAuthorizedUsers(Collections.singletonList("jack"));
        authGroupReq.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(authGroupReq);
    }

    public void addAuthGroup_2(ModelResp pvuvModel) {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId(pvuvModel.getId());
        authGroupReq.setName("tom_row_permission");

        List<AuthRule> authRules = new ArrayList<>();
        authGroupReq.setAuthRules(authRules);
        authGroupReq.setDimensionFilters(Collections.singletonList("user_name = 'tom'"));
        authGroupReq.setDimensionFilterDescription("用户名='tom'");
        authGroupReq.setAuthorizedUsers(Collections.singletonList("tom"));
        authGroupReq.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(authGroupReq);
    }

    private void addPlugin(DataSetResp s2DataSet) {
        ChatPlugin plugin1 = new ChatPlugin();
        plugin1.setType(WebPageQuery.QUERY_MODE);
        plugin1.setDataSetList(Collections.singletonList(s2DataSet.getId()));
        plugin1.setPattern("用于分析超音数的流量概况，包含UV、PV等核心指标的追踪。P.S. 仅作为示例展示，无实际看板");
        plugin1.setName("超音数流量分析看板");
        PluginParseConfig pluginParseConfig = new PluginParseConfig();
        pluginParseConfig.setDescription(plugin1.getPattern());
        pluginParseConfig.setName(plugin1.getName());
        pluginParseConfig.setExamples(Lists.newArrayList("tom最近访问超音数情况怎么样"));
        plugin1.setParseModeConfig(JSONObject.toJSONString(pluginParseConfig));
        WebBase webBase = new WebBase();
        webBase.setUrl("www.yourbi.com");
        webBase.setParamOptions(Lists.newArrayList());
        plugin1.setConfig(JsonUtil.toString(webBase));
        pluginService.createPlugin(plugin1, defaultUser);
    }

    private void addPlugin_1() {
        ChatPlugin plugin1 = new ChatPlugin();
        plugin1.setType(WebServiceQuery.QUERY_MODE);
        plugin1.setDataSetList(Collections.singletonList(-1L));
        plugin1.setPattern("用于分析超音数的流量概况，包含UV、PV等核心指标的追踪。P.S. 仅作为示例展示，无实际内容");
        plugin1.setName("超音数流量分析小助手");
        PluginParseConfig pluginParseConfig = new PluginParseConfig();
        pluginParseConfig.setDescription(plugin1.getPattern());
        pluginParseConfig.setName(plugin1.getName());
        pluginParseConfig.setExamples(Lists.newArrayList("tom最近访问超音数情况怎么样"));
        plugin1.setParseModeConfig(JSONObject.toJSONString(pluginParseConfig));
        WebBase webBase = new WebBase();
        webBase.setUrl("http://localhost:9080/supersonic/api/chat/plugin/pluginDemo");
        webBase.setParamOptions(Lists.newArrayList());
        plugin1.setConfig(JsonUtil.toString(webBase));
        pluginService.createPlugin(plugin1, defaultUser);
    }

    private TagObjectResp addTagObjectUser(DomainResp s2Domain) throws Exception {
        TagObjectReq tagObjectReq = new TagObjectReq();
        tagObjectReq.setDomainId(s2Domain.getId());
        tagObjectReq.setName("用户");
        tagObjectReq.setBizName("user");
        return tagObjectService.create(tagObjectReq, defaultUser);
    }

    private void loadDictWord() {
        dictWordService.loadDictWord();
    }
}
