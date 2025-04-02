package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模版
 * @author JiangWeiWei
 * @TableName s2_template
 */
@Data
@TableName(value ="s2_template")
public class TemplateDO implements Serializable {
    private static final long serialVersionUID = 7695038608245450584L;
    /**
     * 模版id
     */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Integer templateId;

    /**
     * 模版名称
     */
    @TableField(value = "template_name")
    private String templateName;

    /**
     * 模版说明
     */
    @TableField(value = "template_description")
    private String templateDescription;

    /**
     * 模版类型
     */
    @TableField(value = "template_type")
    private String templateType;

    /**
     * 适用范围
     */
    @TableField(value = "scope_of_application")
    private String scopeOfApplication;

    /**
     * 助手id
     */
    @TableField(value = "agent_id")
    private Integer agentId;

    /**
     * 用户id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 对话id
     */
    @TableField(value = "chat_id")
    private Long chatId;


    /**
     * 添加时间
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
    @TableField(value = "id_delete")
    @TableLogic(value = "0",delval = "1")
    private Integer idDelete;
}