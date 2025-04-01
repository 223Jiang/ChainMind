package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 
 * @author JiangWeiWei
 * @TableName temporary
 */
@Data
@Builder
@TableName(value ="temporary")
public class TemporaryDO implements Serializable {

    private static final long serialVersionUID = -7936779234179047521L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 表名
     */
    @TableField(value = "table_name")
    private String tableName;

    /**
     * 表注解
     */
    @TableField(value = "table_note")
    private String tableNote;

    /**
     * 表类型
     */
    @TableField(value = "table_type")
    private String tableType;

    /**
     * 创建用户id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 创建用户id
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 0-正常，1-上传中，2-上传失败
     */
    @TableField(value = "table_status")
    private Integer tableStatus;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private String createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private String updateTime;

    /**
     * 删除状态
     */
    @JsonIgnore
    @TableField(value = "is_delete")
    private Integer isDelete;
}