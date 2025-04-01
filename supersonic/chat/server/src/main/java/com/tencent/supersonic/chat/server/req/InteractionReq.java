package com.tencent.supersonic.chat.server.req;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 模版问答请求数据
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/31
 */
@Data
public class InteractionReq {

    /**
     * 助手id
     */
    @NotNull
    private Integer agentId;

    /**
     * 对话id
     */
    @NotNull
    private Integer chatId;

    /**
     * 问题内容
     */
    @NotNull
    private List<String> queryText;
}
