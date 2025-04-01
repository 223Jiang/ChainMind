package com.tencent.supersonic.chat.server.req.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 模版
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/28
 */
@Data
public class TemplateDTO {
    /**
     * 主键
     */
    private Integer templateId;

    /**
     * 模版名称
     */
    @NotBlank
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
    @NotNull
    private Integer agentId;
}
