package com.tencent.supersonic.chat.server.req.vo;

import lombok.Data;

import java.util.List;

/**
 * 模版数据结构
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/31
 */
@Data
public class TemplateSearchVO {
    /**
     * 模版返回数据
     */
    private TemplateVO templateVo;

    /**
     * 问题模版返回数据
     */
    private List<TemplateIssuesVO> templateIssuesVos;
}
