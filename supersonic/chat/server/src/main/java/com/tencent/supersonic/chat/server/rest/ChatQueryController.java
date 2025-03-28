package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.chat.server.service.ExcelService;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * query controller
 */
@RestController
@RequestMapping({"/supersonic/api/chat/query", "/supersonic/openapi/chat/query"})
public class ChatQueryController {

    @Autowired
    private ChatQueryService chatQueryService;

    @Resource
    private ExcelService excelService;

    @PostMapping("search")
    public Object search(@RequestBody ChatParseReq chatParseReq, HttpServletRequest request,
                         HttpServletResponse response) {
        chatParseReq.setUser(UserHolder.findUser(request, response));
        return chatQueryService.search(chatParseReq);
    }

    @PostMapping("parse")
    public Object parse(@RequestBody ChatParseReq chatParseReq, HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
        chatParseReq.setUser(UserHolder.findUser(request, response));
        return chatQueryService.parse(chatParseReq);
    }

    /**
     * 处理聊天执行请求的端点
     * 该方法主要用于接收聊天执行请求，并根据请求内容执行相应的操作
     *
     * @param chatExecuteReq 聊天执行请求对象，包含执行请求的必要信息
     * @param request        HTTP请求对象，用于获取用户信息
     * @param response       HTTP响应对象，用于处理用户信息
     * @return 执行请求后的结果对象
     * @throws Exception 如果执行过程中发生错误，抛出异常
     */
    @PostMapping("execute")
    public Object execute(@RequestBody ChatExecuteReq chatExecuteReq, HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
        // 将用户信息添加到请求对象中，以便在执行操作时使用
        chatExecuteReq.setUser(UserHolder.findUser(request, response));
        // 调用服务层的execute方法，执行聊天操作，并返回执行结果
        return chatQueryService.execute(chatExecuteReq);
    }

    @PostMapping("/")
    public Object query(@RequestBody ChatParseReq chatParseReq, HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        chatParseReq.setUser(user);
        ChatParseResp parseResp = chatQueryService.parse(chatParseReq);

        if (CollectionUtils.isEmpty(parseResp.getSelectedParses())) {
            throw new InvalidArgumentException("parser error,no selectedParses");
        }
        SemanticParseInfo semanticParseInfo = parseResp.getSelectedParses().get(0);
        ChatExecuteReq chatExecuteReq = ChatExecuteReq.builder().build();
        BeanUtils.copyProperties(chatParseReq, chatExecuteReq);
        chatExecuteReq.setQueryId(parseResp.getQueryId());
        chatExecuteReq.setParseId(semanticParseInfo.getId());
        return chatQueryService.execute(chatExecuteReq);
    }

    @PostMapping("queryData")
    public Object queryData(@RequestBody ChatQueryDataReq chatQueryDataReq,
                            HttpServletRequest request, HttpServletResponse response) throws Exception {
        chatQueryDataReq.setUser(UserHolder.findUser(request, response));
        return chatQueryService.queryData(chatQueryDataReq, UserHolder.findUser(request, response));
    }

    @PostMapping("queryDimensionValue")
    public Object queryDimensionValue(@RequestBody @Valid DimensionValueReq dimensionValueReq,
                                      HttpServletRequest request, HttpServletResponse response) throws Exception {
        return chatQueryService.queryDimensionValue(dimensionValueReq,
                UserHolder.findUser(request, response));
    }


}
