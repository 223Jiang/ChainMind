package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporaryReq extends PageBaseReq {
    /**
     * 表类型
     */
    private String tableType;

    /**
     * 文件简介
     */
    private String tableNote;

    /**
     * 用户id
     */
    private Long userId;
}
