package com.tencent.supersonic.chat.server.req.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 问题模版
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/28
 */
@Data
public class TemplateIssuesDTO {
    /**
     * 主键
     */
    private Integer issuesId;

    /**
     * 问题内容
     */
    @NotNull
    private String questionContent;
}
