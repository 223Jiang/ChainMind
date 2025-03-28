package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;

import java.util.List;

/**
 * @author JiangWeiWei
 */
public interface ChatQueryService {

    List<SearchResult> search(ChatParseReq chatParseReq);

    ChatParseResp parse(ChatParseReq chatParseReq);

    QueryResult execute(ChatExecuteReq chatExecuteReq) throws Exception;

    /**
     * 获取大模型对话
     *
     * @param queryText 闲聊需要传递参数
     * @return 大模型对话数据
     */
    String largeModelsChatter(String queryText);

    QueryResult parseAndExecute(ChatParseReq chatParseReq);

    Object queryData(ChatQueryDataReq chatQueryDataReq, User user) throws Exception;

    Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception;
}
