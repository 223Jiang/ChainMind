package com.tencent.supersonic.chat.server.plugin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * ExcelDataImport实体
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/27
 */
@Data
public class ExcelDataImport {
    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 表备注
     */
    @NotBlank(message = "表备注不能为空")
    private String tableNote;

    /**
     * 表类型
     */
    @NotBlank(message = "表类型不能为空")
    private String tableType;

    /**
     * 表id
     */
    private Long id;
}
