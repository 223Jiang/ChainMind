package com.tencent.supersonic.chat.server.req.vo;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模版问答返回数据
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionVO {
    /**
     * 问答数据集id
     */
    private List<Long> queryIds;

    /**
     * 问答结果集
     */
    private List<QueryResult> queryResults;

    /**
     * 助手id
     */
    private Integer agentId;
}
