package com.tencent.supersonic.chat.server.req.vo;

import lombok.Data;

import java.util.Date;

/**
 * 问题模版返回数据
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/31
 */
@Data
public class TemplateIssuesVO {

    /**
     * 主键
     */
    private Integer issuesId;

    /**
     * 模版id
     */
    private Integer templateId;

    /**
     * 问题内容
     */
    private String questionContent;

    /**
     * 创建时间
     */
    private Date createTime;
}
