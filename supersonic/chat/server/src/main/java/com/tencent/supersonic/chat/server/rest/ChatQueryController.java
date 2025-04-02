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
 * 处理聊天接口
 */
@RestController
@RequestMapping({"/supersonic/api/chat/query", "/supersonic/openapi/chat/query"})
public class ChatQueryController {

    @Autowired
    private ChatQueryService chatQueryService;

    @Resource
    private ExcelService excelService;

    /**
     * 处理搜索请求的接口方法
     * 用于处理来自客户端的搜索请求，将请求中的数据解析并执行搜索操作
     *
     * @param chatParseReq 包含搜索请求数据的对象，由客户端发送的JSON数据转换而来
     * @param request      HTTP请求对象，用于获取请求头信息等
     * @param response     HTTP响应对象，用于设置响应头信息等
     * @return 返回搜索结果，具体结果类型依赖于搜索服务的实现
     */
    @PostMapping("search")
    public Object search(@RequestBody ChatParseReq chatParseReq, HttpServletRequest request,
                         HttpServletResponse response) {
        // 在请求中查找用户信息，并设置到搜索请求对象中
        chatParseReq.setUser(UserHolder.findUser(request, response));
        // 调用搜索服务执行搜索操作，并返回搜索结果
        return chatQueryService.search(chatParseReq);
    }

    /**
     * 处理解析请求的端点
     * 该方法负责接收HTTP POST请求，解析聊天请求参数，并调用服务层方法进行处理
     *
     * @param chatParseReq 包含聊天解析请求数据的对象，由请求体自动转换而来
     * @param request      HTTP请求对象，用于获取请求信息
     * @param response     HTTP响应对象，用于设置响应信息
     * @return 返回由chatQueryService解析后的结果对象
     * @throws Exception 如果解析过程中发生错误，抛出异常
     */
    @PostMapping("parse")
    public Object parse(@RequestBody ChatParseReq chatParseReq, HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
        // 在请求参数中添加用户信息，该信息通过UserHolder根据请求和响应获取
        chatParseReq.setUser(UserHolder.findUser(request, response));
        // 调用chatQueryService的parse方法处理聊天解析请求，并返回结果
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
