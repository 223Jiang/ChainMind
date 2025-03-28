package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/27
 */
@Data
public class TemporaryUpdateReq {
    /**
     * 临时表id
     */
    @NotNull
    private Long id;

    /**
     * 表类型
     */
    @NotBlank
    private String tableType;

    /**
     * 文件简介
     */
    @NotBlank
    private String tableNote;
}
