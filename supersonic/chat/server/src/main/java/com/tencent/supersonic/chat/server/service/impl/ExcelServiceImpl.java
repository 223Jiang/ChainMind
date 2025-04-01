package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.server.service.ExcelService;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemporaryDO;
import com.tencent.supersonic.chat.server.persistence.repository.TemporaryRepository;
import com.tencent.supersonic.chat.server.plugin.ExcelDataImport;
import com.tencent.supersonic.chat.server.util.ExcelUtil;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.SqlExecuteReq;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/26
 */
@Service
@Slf4j
public class ExcelServiceImpl implements ExcelService {

    @Value("${file.upload-dir: D:/temporary}")
    private String uploadDir;

    private final String promptWords = "请根据以上excel部分数据创建一张表，用mysql创建语句，注意其它东西都不用说，直接回显创建语句就行，在回复创表语句的时候，需要验证该sql是否可行，注意需要添加字段注解comment，字段全部用varchar类型，字段用英文。不用回复其他东西！只需要回复sql建表语句，重要的事情说三遍。";

    private final ChatQueryService chatQueryService;

    private final DatabaseService databaseService;

    private final TemporaryRepository temporaryRepository;

    public ExcelServiceImpl(ChatQueryService chatQueryService, DatabaseService databaseService, TemporaryRepository temporaryRepository) {
        this.chatQueryService = chatQueryService;
        this.databaseService = databaseService;
        this.temporaryRepository = temporaryRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processExcel(ExcelDataImport excelDataImport, User user) {
        try {
            MultipartFile file = getMultipartFile(uploadDir + "/" + excelDataImport.getFileName());
            Long tableId = excelDataImport.getId();
            String tableNote = excelDataImport.getTableNote();
            String tableType = excelDataImport.getTableType();
            String tableName = "";

            SqlExecuteReq sqlExecuteReq = new SqlExecuteReq();
            sqlExecuteReq.setId(5L);

            // 读取Excel文件前四行数据
            String excelFourLines = ExcelUtil.readFirstFourRows(file) + "\n" + promptWords;
            log.info("获取Excel文件前四行数据：\n{}", excelFourLines);
            // 当表不存在的时候，进行表创建
            if (tableId == null) {
                String sql = chatQueryService.largeModelsChatter(excelFourLines);

                // 进行表名赋值
                String preTableName = extractTableName(sql);
                log.info("搜集表名：{}", preTableName);

                tableName = preTableName + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                log.info("完善后表名：{}", tableName);

                sql = sql.replace("```sql", "").replace("```", "")
                         .replace(" " + preTableName + " ", " " + tableName + " ");
                log.info("创建表语句：\n{}", sql);

                sqlExecuteReq.setSql(sql);
                // 执行sql进行表创建
                try {
                    databaseService.executeTableSql(sqlExecuteReq, 5L, user);
                } catch (Exception e) {
                    log.error("创建表失败！");
                    throw new RuntimeException(e.getMessage());
                }

                // 进行表存储
                TemporaryDO temporaryDO = TemporaryDO.builder()
                        .tableName(tableName).userId(user.getId())
                        .userName(user.getName()).tableNote(tableNote)
                        .tableType(tableType).build();

                temporaryRepository.save(temporaryDO);
                tableId = temporaryDO.getId();
                log.info("生成表id：{}", temporaryDO.getId());
            } else {
                tableName = temporaryRepository.getById(tableId).getTableName();
            }

            // 进行excel表数据导入
            asyncImportData(tableId, tableName, file, sqlExecuteReq, user)
                    .exceptionally(ex -> {
                        log.error("异步导入失败: {}", ex.getMessage());
                        return null;
                    });
            log.info("异步导入任务已提交");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Async("asyncTaskExecutor") // 指定线程池
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CompletableFuture<Void> asyncImportData(Long tableId, String tableName, MultipartFile file,
                                                   SqlExecuteReq sqlExecuteReq, User user) {

        // TODO: 需要使用锁，防止同时导入数据出现问题
        TemporaryDO tempDO = temporaryRepository.getById(tableId);

        try {
            List<List<String>> excelDataList = ExcelUtil.readExcel(file);
            String insertSql = insertDataIntoTable(tableName, excelDataList.get(0));
            log.info("生成插入sql语句：{}", insertSql);

            List<Object[]> batchArgs = new ArrayList<>();
            for (int i = 1; i < excelDataList.size(); i++) {
                batchArgs.add(excelDataList.get(i).toArray());
            }
            log.info("批量插入数据集.......");

            sqlExecuteReq.setSql(insertSql);
            databaseService.executeSaveSql(sqlExecuteReq, batchArgs, 5L, user);

            // 更新表状态为导入成功
            tempDO.setTableStatus(0);
            temporaryRepository.save(tempDO);

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            // 更新表状态为导入失败
            tempDO.setTableStatus(3);
            temporaryRepository.save(tempDO);

            throw new CompletionException(e);
        }
    }

    /**
     * 根据路径获取构建MultipartFile
     * @param filePath  文件路径
     * @return          MultipartFile对象
     */
    private static MultipartFile getMultipartFile(String filePath) throws IOException {
        // 参数校验
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        // 防止路径遍历攻击
        Path path = Paths.get(filePath);
        if (!path.normalize().equals(path)) {
            throw new SecurityException("非法路径：" + filePath);
        }

        File file = path.toFile();

        // 确保文件存在且可读
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("文件不存在或不可读：" + filePath);
        }

        // 获取文件名和MIME类型
        String fileName = file.getName();
        String contentType = Files.probeContentType(path);

        // 使用流式处理避免一次性加载大文件
        try (FileInputStream fis = new FileInputStream(file)) {
            // 创建 MockMultipartFile 对象
            return new MockMultipartFile(
                    // 表单字段名
                    fileName,
                    // 原始文件名
                    fileName,
                    // MIME 类型
                    contentType,
                    // 文件输入流
                    fis
            );
        }
    }


    /**
     * 插入sql语句
     *
     * @param tableName   表名
     * @param excelData   数据列表
     */
    private String insertDataIntoTable(String tableName, List<String> excelData) {
        if (excelData.isEmpty()) {
            throw new IllegalArgumentException("Excel 数据为空");
        }

        // 获取第一行数据的列数
        int columnCount = excelData.size();

        // 动态生成 SQL 插入语句
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(tableName).append(" VALUES (");
        for (int i = 0; i < columnCount; i++) {
            sqlBuilder.append("?");
            if (i < columnCount - 1) {
                sqlBuilder.append(", ");
            }
        }
        sqlBuilder.append(")");

        return sqlBuilder.toString();
    }

    /**
     * 从 SQL 建表语句中提取表名
     *
     * @param createTableSql SQL 建表语句
     * @return 表名（如果无法解析则返回 null）
     */
    public static String extractTableName(String createTableSql) {
        if (createTableSql == null || createTableSql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty.");
        }

        // 定义正则表达式，匹配 CREATE TABLE 后的表名
        String regex = "(?i)CREATE\\s+TABLE\\s+(\\w+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(createTableSql);

        // 如果匹配成功，返回表名
        if (matcher.find()) {
            // 提取第一个捕获组（表名）
            return matcher.group(1);
        }

        // 如果未匹配到，返回 null
        return null;
    }
}
