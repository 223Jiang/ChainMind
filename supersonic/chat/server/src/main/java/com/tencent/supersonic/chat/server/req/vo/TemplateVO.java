package com.tencent.supersonic.chat.server.req.vo;

import lombok.Data;

import java.util.Date;

/**
 * 模版返回数据
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/31
 */
@Data
public class TemplateVO {
    /**
     * 模版id
     */
    private Integer templateId;

    /**
     * 模版名称
     */
    private String templateName;

    /**
     * 模版说明
     */
    private String templateDescription;

    /**
     * 模版类型
     */
    private String templateType;

    /**
     * 适用范围
     */
    private String scopeOfApplication;

    /**
     * 助手id
     */
    private Integer agentId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 对话id
     */
    private Long chatId;

    /**
     * 添加时间
     */
    private Date createTime;
}
