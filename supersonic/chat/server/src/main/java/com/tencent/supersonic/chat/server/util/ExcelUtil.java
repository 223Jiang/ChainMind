package com.tencent.supersonic.chat.server.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/26
 */
public class ExcelUtil {
    /**
     * 使用 EasyExcel 读取 Excel 数据（跳过前1行数据）
     *
     * @param file Excel 文件
     * @return 数据列表
     */
    public static List<List<String>> readExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return new ArrayList<>();
        }

        // 用于存储前四行数据
        List<List<String>> resultList = new ArrayList<>();

        // 读取Excel
        EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                List<String> rowData = new ArrayList<>();
                // 将Map转换为List，保持列的顺序
                for (int i = 0; i < data.size(); i++) {
                    String value = data.get(i);
                    if (value != null) {
                        value = value.replace(" ", "");
                    }
                    rowData.add(value != null ? value.trim() : "");
                }
                resultList.add(rowData);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).headRowNumber(1).sheet().doRead();

        // 将结果转换为指定格式的字符串
        return resultList;
    }

    /**
     * 获取Excel文件前四行数据
     * @param file  Excel文件
     * @return      前四行数据
     */
    public static String readFirstFourRows(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }

        // 用于存储前四行数据
        List<List<String>> resultList = new ArrayList<>();

        // 读取Excel
        EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // 只读取前4行
                if (context.readRowHolder().getRowIndex() < 4) {
                    List<String> rowData = new ArrayList<>();
                    // 将Map转换为List，保持列的顺序
                    for (int i = 0; i < data.size(); i++) {
                        String value = data.get(i);
                        rowData.add(value != null ? value.trim() : "");
                    }
                    resultList.add(rowData);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        // 将结果转换为指定格式的字符串
        return formatResult(resultList);
    }

    /**
     * 将结果转换为指定格式的字符串
     * @param resultList    结果集
     * @return              指定格式的字符串
     */
    private static String formatResult(List<List<String>> resultList) {
        StringBuilder result = new StringBuilder();

        // 处理每一行数据
        for (int i = 0; i < resultList.size(); i++) {
            List<String> row = resultList.get(i);
            // 使用空格连接每列数据
            result.append(String.join(" ", row));
            // 除了最后一行，每行后面添加换行符
            if (i < resultList.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }
}
