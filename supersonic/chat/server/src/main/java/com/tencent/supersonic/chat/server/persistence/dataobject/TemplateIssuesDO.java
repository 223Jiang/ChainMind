package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模版问题
 * @author JiangWeiWei
 * @TableName s2_template_issues
 */
@Data
@TableName(value ="s2_template_issues")
public class TemplateIssuesDO implements Serializable {
    private static final long serialVersionUID = -7520871853276736896L;
    /**
     * 主键
     */
    @TableId(value = "issues_id", type = IdType.AUTO)
    private Integer issuesId;

    /**
     * 模版id
     */
    @TableField(value = "template_id")
    private Integer templateId;

    /**
     * 问题内容
     */
    @TableField(value = "question_content")
    private String questionContent;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 0-未删除，1-删除
     */
    @TableField(value = "is_delete")
    @TableLogic(value = "0",delval = "1")
    private Integer isDelete;
}