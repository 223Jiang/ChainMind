package com.tencent.supersonic.chat.server.req.dto;

import lombok.Data;

import java.util.List;

/**
 * 问答数据导出为请求实体
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/4/1
 */
@Data
public class DataExportDTO {
    /**
     * 问答数据集id
     */
    private List<Long> queryIds;

    /**
     * 助手id
     */
    private Integer agentId;
}
