package com.tencent.supersonic.chat.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.QueryDO;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.persistence.repository.ChatRepository;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatManageServiceImpl implements ChatManageService {

    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private ChatQueryRepository chatQueryRepository;

    @Override
    public Long addChat(User user, String chatName, Integer agentId) {
        ChatDO chatDO = new ChatDO();
        chatDO.setChatName(chatName);
        chatDO.setCreator(user.getName());
        chatDO.setCreateTime(getCurrentTime());
        chatDO.setIsDelete(0);
        chatDO.setLastTime(getCurrentTime());
        chatDO.setLastQuestion("Hello, welcome to using supersonic");
        chatDO.setIsTop(0);
        chatDO.setAgentId(agentId);
        return chatRepository.createChat(chatDO);
    }

    @Override
    public List<ChatDO> getAll(String userName, Integer agentId) {
        return chatRepository.getAll(userName, agentId);
    }

    @Override
    public boolean updateChatName(Long chatId, String chatName, String userName) {
        return chatRepository.updateChatName(chatId, chatName, getCurrentTime(), userName);
    }

    @Override
    public boolean updateFeedback(Integer id, Integer score, String feedback) {
        QueryDO intelligentQueryDO = new QueryDO();
        intelligentQueryDO.setId(id);
        intelligentQueryDO.setScore(score);
        intelligentQueryDO.setFeedback(feedback);
        return chatRepository.updateFeedback(intelligentQueryDO);
    }

    @Override
    public boolean updateChatIsTop(Long chatId, int isTop) {
        return chatRepository.updateConversionIsTop(chatId, isTop);
    }

    @Override
    public Boolean deleteChat(Long chatId, String userName) {
        return chatRepository.deleteChat(chatId, userName);
    }

    /**
     * 根据查询条件和聊天ID查询信息
     * 此方法用于处理基于特定查询条件和聊天ID的信息检索它首先从数据库中获取匹配的查询结果，
     * 如果结果列表为空，则直接返回结果页如果结果列表不为空，则进一步填充解析信息后返回
     *
     * @param pageQueryInfoReq 包含查询条件的请求对象，用于指定查询的条件和分页信息
     * @param chatId 聊天ID，用于指定查询的聊天上下文
     * @return 返回包含查询结果的PageInfo对象，其中包含根据查询条件检索到的QueryResp实例列表
     */
    @Override
    public PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoReq, long chatId) {
        // 从数据库中获取符合查询条件和聊天ID的查询结果
        PageInfo<QueryResp> queryRespPageInfo =
                chatQueryRepository.getChatQuery(pageQueryInfoReq, chatId);

        // 如果查询结果列表为空，则直接返回结果页
        if (CollectionUtils.isEmpty(queryRespPageInfo.getList())) {
            return queryRespPageInfo;
        }

        // 填充解析信息到查询结果列表中
        fillParseInfo(queryRespPageInfo.getList());

        // 返回填充了解析信息的查询结果页
        return queryRespPageInfo;
    }

    @Override
    public Long createChatQuery(ChatParseReq chatParseReq) {
        return chatQueryRepository.createChatQuery(chatParseReq);
    }

    @Override
    public QueryResp getChatQuery(Long queryId) {
        return chatQueryRepository.getChatQuery(queryId);
    }

    @Override
    public List<QueryResp> getChatQueries(Integer chatId) {
        List<QueryResp> queries = chatQueryRepository.getChatQueries(chatId);
        fillParseInfo(queries);
        return queries;
    }

    @Override
    public ShowCaseResp queryShowCase(PageQueryInfoReq pageQueryInfoReq, int agentId) {
        ShowCaseResp showCaseResp = new ShowCaseResp();
        showCaseResp.setCurrent(pageQueryInfoReq.getCurrent());
        showCaseResp.setPageSize(pageQueryInfoReq.getPageSize());
        List<QueryResp> queryResps = chatQueryRepository.queryShowCase(pageQueryInfoReq, agentId);
        if (CollectionUtils.isEmpty(queryResps)) {
            return showCaseResp;
        }
        queryResps.removeIf(queryResp -> {
            if (queryResp.getQueryResult() == null) {
                return true;
            }
            if (queryResp.getQueryResult().getResponse() != null) {
                return false;
            }
            if (CollectionUtils.isEmpty(queryResp.getQueryResult().getQueryResults())) {
                return true;
            }
            Map<String, Object> data = queryResp.getQueryResult().getQueryResults().get(0);
            return CollectionUtils.isEmpty(data);
        });
        queryResps = new ArrayList<>(queryResps.stream()
                .collect(Collectors.toMap(QueryResp::getQueryText, Function.identity(),
                        (existing, replacement) -> existing, LinkedHashMap::new))
                .values());
        fillParseInfo(queryResps);
        Map<Long, List<QueryResp>> showCaseMap =
                queryResps.stream().collect(Collectors.groupingBy(QueryResp::getChatId));
        showCaseResp.setShowCaseMap(showCaseMap);
        return showCaseResp;
    }

    private void fillParseInfo(List<QueryResp> queryResps) {
        List<Long> queryIds =
                queryResps.stream().map(QueryResp::getQuestionId).collect(Collectors.toList());
        List<ChatParseDO> chatParseDOs = chatQueryRepository.getParseInfoList(queryIds);
        if (CollectionUtils.isEmpty(chatParseDOs)) {
            return;
        }
        Map<Long, List<ChatParseDO>> chatParseMap =
                chatParseDOs.stream().collect(Collectors.groupingBy(ChatParseDO::getQuestionId));
        for (QueryResp queryResp : queryResps) {
            List<ChatParseDO> chatParseDOList = chatParseMap.get(queryResp.getQuestionId());
            if (CollectionUtils.isEmpty(chatParseDOList)) {
                continue;
            }
            List<SemanticParseInfo> parseInfos = chatParseDOList.stream()
                    .map(chatParseDO -> JsonUtil.toObject(chatParseDO.getParseInfo(),
                            SemanticParseInfo.class))
                    .sorted(Comparator.comparingDouble(SemanticParseInfo::getScore).reversed())
                    .collect(Collectors.toList());
            queryResp.setParseInfos(parseInfos);
        }
    }

    @Override
    public ChatQueryDO saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult) {
        ChatQueryDO chatQueryDO = chatQueryRepository.getChatQueryDO(chatExecuteReq.getQueryId());
        chatQueryDO.setQuestionId(chatExecuteReq.getQueryId());
        chatQueryDO.setQueryResult(JsonUtil.toString(queryResult));
        chatQueryDO.setQueryState(1);
        updateQuery(chatQueryDO);
        chatRepository.updateLastQuestion(chatExecuteReq.getChatId().longValue(),
                chatExecuteReq.getQueryText(), getCurrentTime());
        return chatQueryDO;
    }

    @Override
    public int updateQuery(ChatQueryDO chatQueryDO) {
        return chatQueryRepository.updateChatQuery(chatQueryDO);
    }

    @Override
    public void deleteQuery(Long queryId) {
        ChatQueryDO chatQuery = chatQueryRepository.getChatQueryDO(queryId);
        if (Objects.nonNull(chatQuery)) {
            chatQuery.setQueryState(0);
            chatQueryRepository.updateChatQuery(chatQuery);
        }
    }

    @Override
    public void updateParseCostTime(ChatParseResp chatParseResp) {
        ChatQueryDO chatQueryDO = chatQueryRepository.getChatQueryDO(chatParseResp.getQueryId());
        chatQueryDO.setParseTimeCost(JsonUtil.toString(chatParseResp.getParseTimeCost()));
        updateQuery(chatQueryDO);
    }

    @Override
    public List<ChatParseDO> batchAddParse(ChatParseReq chatParseReq, ChatParseResp chatParseResp) {
        List<SemanticParseInfo> candidateParses = chatParseResp.getSelectedParses();
        return chatQueryRepository.batchSaveParseInfo(chatParseReq, chatParseResp, candidateParses);
    }

    private String getCurrentTime() {
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return tempDate.format(new java.util.Date());
    }

    @Override
    public SemanticParseInfo getParseInfo(Long questionId, int parseId) {
        ChatParseDO chatParseDO = chatQueryRepository.getParseInfo(questionId, parseId);
        return JSONObject.parseObject(chatParseDO.getParseInfo(), SemanticParseInfo.class);
    }
}
