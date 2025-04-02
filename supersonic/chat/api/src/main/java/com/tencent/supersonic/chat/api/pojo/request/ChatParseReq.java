package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatParseReq类用于封装聊天解析请求的相关信息
 * 它作为一个数据传输对象（DTO），用于在系统内部传递聊天查询的相关参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParseReq {
    /**
     * 用户查询的文本信息
     */
    private String queryText;

    /**
     * 聊天的唯一标识符
     */
    private Integer chatId;

    /**
     * 助手的唯一标识符
     */
    private Integer agentId;

    /**
     * 数据集的唯一标识符
     */
    private Long dataSetId;

    /**
     * 用户信息对象，包含有关用户的具体信息
     */
    private User user;

    /**
     * 查询过滤器，用于指定查询的特定条件或参数
     */
    private QueryFilters queryFilters;

    /**
     * 是否保存答案的标志，默认值为true
     * 当设置为true时，表示查询的答案将被保存；反之则不保存
     */
    private boolean saveAnswer = true;

    /**
     * 是否禁用语言模型的标志，默认值为false
     * 当设置为true时，表示在处理查询时不使用语言模型；反之则使用
     */
    private boolean disableLLM = false;

    /**
     * 对话的唯一标识符
     */
    private Long queryId;

    /**
     * 选定的语义解析信息，用于记录查询的解析详情
     */
    private SemanticParseInfo selectedParse;
}

