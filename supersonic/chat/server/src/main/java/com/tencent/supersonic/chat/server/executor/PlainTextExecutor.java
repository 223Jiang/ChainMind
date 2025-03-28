package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlainTextExecutor implements ChatQueryExecutor {

    public static final String APP_KEY = "SMALL_TALK";
    private static final String INSTRUCTION = "" + "#Role: You are a nice person to talk to."
            + "\n#Task: Respond quickly and nicely to the user."
            + "\n#Rules: 1.ALWAYS use the same language as the `#Current Input`."
            + "\n#History Inputs: %s" + "\n#Current Input: %s" + "\n#Response: ";

    public PlainTextExecutor() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("闲聊对话")
                .appModule(AppModule.CHAT).description("直接将原始输入透传大模型").enable(false).build());
    }

    /**
     * 执行查询操作，专用于PLAIN_TEXT查询模式
     *
     * @param executeContext 执行上下文，包含解析信息和请求详情
     * @return 返回查询结果对象，如果条件不满足则返回null
     */
    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        // 仅处理查询模式为PLAIN_TEXT的请求
        if (!"PLAIN_TEXT".equals(executeContext.getParseInfo().getQueryMode())) {
            return null;
        }

        // 获取AgentService实例
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        // 根据执行上下文中的Agent ID获取具体的Agent
        Agent chatAgent = agentService.getAgent(executeContext.getAgent().getId());
        // 获取Agent配置的ChatApp
        ChatApp chatApp = chatAgent.getChatAppConfig().get(APP_KEY);
        // 如果ChatApp不存在或未启用，则不继续处理
        if (Objects.isNull(chatApp) || !chatApp.isEnable()) {
            return null;
        }

        // 根据历史输入和当前查询文本格式化提示字符串
        String promptStr = String.format(chatApp.getPrompt(), getHistoryInputs(executeContext),
                executeContext.getRequest().getQueryText());
        // 创建Prompt对象
        Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.EMPTY_MAP);
        // 获取聊天语言模型
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        // 使用语言模型生成响应
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());

        // 创建查询结果对象
        QueryResult result = new QueryResult();
        // 设置查询状态为成功
        result.setQueryState(QueryState.SUCCESS);
        // 设置查询模式
        result.setQueryMode(executeContext.getParseInfo().getQueryMode());
        // 设置文本结果
        result.setTextResult(response.content().text());

        // 返回查询结果
        return result;
    }


    private String getHistoryInputs(ExecuteContext executeContext) {
        StringBuilder historyInput = new StringBuilder();
        List<QueryResp> queryResps = new ArrayList<>();
        if (executeContext.getRequest().getChatId() != null) {
            queryResps = getHistoryQueries(executeContext.getRequest().getChatId(), 5);
        }
        queryResps.forEach(p -> {
            historyInput.append(p.getQueryText());
            historyInput.append(";");

        });

        return historyInput.toString();
    }

    private List<QueryResp> getHistoryQueries(int chatId, int multiNum) {
        ChatManageService chatManageService = ContextUtils.getBean(ChatManageService.class);
        List<QueryResp> contextualParseInfoList = chatManageService.getChatQueries(chatId).stream()
                .filter(q -> Objects.nonNull(q.getQueryResult())
                        && q.getQueryResult().getQueryState() == QueryState.SUCCESS)
                .collect(Collectors.toList());

        List<QueryResp> contextualList = contextualParseInfoList.subList(0,
                Math.min(multiNum, contextualParseInfoList.size()));
        Collections.reverse(contextualList);

        return contextualList;
    }
}
