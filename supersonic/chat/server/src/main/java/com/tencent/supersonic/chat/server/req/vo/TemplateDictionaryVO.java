package com.tencent.supersonic.chat.server.req.vo;

import lombok.Data;

/**
 * 助手字典
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/31
 */
@Data
public class TemplateDictionaryVO {
    /**
     * 助手id
     */
    private Integer id;

    /**
     * 助手名称
     */
    private String name;
}
