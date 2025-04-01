package com.tencent.supersonic.chat.server.util;


import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.rest.ChatController;
import com.tencent.supersonic.common.pojo.ChatApp;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportUtil {
    public static String getReport(List<QueryResp> list, Agent agent) {
        List<String> htmls = new ArrayList<>();
        //提示词模板
        String promptStr = "你现在是一个前端html专家，我需要将一些数据分析的结果组合成一个报表以html的结果展示\n" +
                "我需要你帮我将数据转换出html中的表格形式的数据，要求只返回html的表格代码给我\n" +
                "数据如下 ```\n" +
                "{{data}}\n" +
                "```";
        //html代码模板
        String htmlStr = "<div class=\"analysis\" id=\"%s\">\n" +
                "\t<h2>%s</h2>\n" +
                "\t<p class=\"summary\">%s</p>\n" +
                "\t%s\n" +
                "</div>";
        //获取大模型配置
//        Agent agent = agentService.getAgent(5);
        ChatApp chatApp = agent.getChatAppConfig().get("DATA_INTERPRETER");
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> variable = new HashMap<>();
            variable.put("data", list.get(i).getQueryResult().getTextResult());
            Prompt prompt = PromptTemplate.from(promptStr).apply(variable);

            Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
            String tableText = response.content().text()
                    .replaceAll("```html","")
                    .replaceAll("```","");

            String format = String.format(htmlStr, "a" + i + 1,
                    (i+1)+"."+list.get(i).getQueryText(),
                    list.get(i).getQueryResult().getTextSummary(),
                    tableText);
            htmls.add(format);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("htmlCode",htmls);

        try {
            // 1. 初始化 FreeMarker 配置
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(ChatController.class, "/template");
            // 2. 加载模板
            Template template = cfg.getTemplate("template2.ftl");

            // 3. 准备数据模型
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("items",htmls);
            // 4. 渲染模板内容
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            String htmlContent = writer.toString();
            // 设置字体提供器（用于 HtmlConverter）
            String fontPath = ChatController.class.getClassLoader()
                    .getResource("font/simfang.ttf")
                    .getPath();
            ConverterProperties properties = new ConverterProperties();
            properties.setFontProvider(new DefaultFontProvider(false, false, false));
            properties.getFontProvider().addFont(fontPath);
            // 5. 将 HTML 转换为 PDF
            try (FileOutputStream pdfOutputStream = new FileOutputStream("d:/output3.pdf")) {
                HtmlConverter.convertToPdf(htmlContent, pdfOutputStream,properties);
                System.out.println("PDF 文档已成功生成！");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "resultMap";
    }

    public static String exportMarkDown(List<QueryResp> list) {
        // 准备数据模型
        List<Map<String, Object>> modules = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> module1 = new HashMap<>();

            module1.put("title",(i+1)+"."+list.get(i).getQueryText());
            module1.put("content",list.get(i).getQueryResult().getTextSummary());
            module1.put("table",list.get(i).getQueryResult().getTextResult());
            modules.add(module1);
        }

        try {
            // 1. 初始化 FreeMarker 配置
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(ChatController.class, "/template");
            // 2. 加载模板
            Template template = cfg.getTemplate("markdown2.ftl");


            // 3. 准备数据模型
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("modules",modules);
            // 4. 渲染模板内容
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            String markdownContent = writer.toString();
            // 写入文件
            try (FileWriter fileWriter = new FileWriter("d:/output.md")) {
                fileWriter.write(markdownContent);
                System.out.println("Markdown 文件已成功生成！");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "success";
    }
}
