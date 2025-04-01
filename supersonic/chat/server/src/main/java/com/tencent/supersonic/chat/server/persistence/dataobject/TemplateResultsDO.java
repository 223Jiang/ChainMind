package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模版结果
 * @author JiangWeiWei
 * @TableName s2_template_results
 */
@Data
@TableName(value ="s2_template_results")
public class TemplateResultsDO implements Serializable {
    private static final long serialVersionUID = 944203307408379457L;
    /**
     * 主键
     */
    @TableId(value = "result_id")
    private Integer resultId;

    /**
     * 模版问题id
     */
    @TableField(value = "issues_id")
    private Integer issuesId;

    /**
     * 问答id
     */
    @TableField(value = "chat_id")
    private Long chatId;

    /**
     * 实际问题
     */
    @TableField(value = "practical_problems")
    private String practicalProblems;

    /**
     * 问题结果
     */
    @TableField(value = "problem_result")
    private String problemResult;

    /**
     * 序号
     */
    @TableField(value = "ordinal")
    private Integer ordinal;

    /**
     * 问答时间
     */
    @TableField(value = "create_time")
    private Date createTime;
}