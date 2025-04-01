package com.tencent.supersonic.chat.server.req;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.Data;

/**
 * 查询模版
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/31
 */
@Data
public class TemplateSearchReq extends PageBaseReq {
    /**
     * 模版名称
     */
    private String templateName;

    /**
     * 模版类型
     */
    private String templateType;

    /**
     * 助手id
     */
    private Integer agentId;

    /**
     * 用户id
     */
    private Long userId;
}
