package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.server.plugin.ExcelDataImport;
import com.tencent.supersonic.common.pojo.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/26
 */
public interface ExcelService {
    /**
     * 将Excel文件数据导入到表中
     * @param excelDataImport  创建表数据
     * @param user 用户数据
     */
    void processExcel(ExcelDataImport excelDataImport, User user);
}
