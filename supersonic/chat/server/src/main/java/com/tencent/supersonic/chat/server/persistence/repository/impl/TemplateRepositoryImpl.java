package com.tencent.supersonic.chat.server.persistence.repository.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateIssuesDO;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.persistence.repository.TemplateIssuesRepository;
import com.tencent.supersonic.chat.server.persistence.repository.TemplateRepository;
import com.tencent.supersonic.chat.server.persistence.mapper.TemplateMapper;
import com.tencent.supersonic.chat.server.req.InteractionReq;
import com.tencent.supersonic.chat.server.req.TemplateReq;
import com.tencent.supersonic.chat.server.req.TemplateSearchReq;
import com.tencent.supersonic.chat.server.req.vo.TemplateIssuesVO;
import com.tencent.supersonic.chat.server.req.vo.TemplateSearchVO;
import com.tencent.supersonic.chat.server.req.vo.TemplateVO;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.server.util.ReportUtil;
import com.tencent.supersonic.common.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author JiangWeiWei
* @description 针对表【s2_template(模版)】的数据库操作Service实现
* @createDate 2025-03-28 14:30:04
*/
@Service
public class TemplateRepositoryImpl extends ServiceImpl<TemplateMapper, TemplateDO>
    implements TemplateRepository {

    private final TemplateIssuesRepository templateIssuesRepository;

    private final ChatManageService chatService;

    private final ChatQueryRepository chatQueryRepository;

    private final ChatQueryService chatQueryService;

    private final AgentService agentService;

    public TemplateRepositoryImpl(
            TemplateIssuesRepository templateIssuesRepository
            , @Qualifier("chatManageServiceImpl") ChatManageService chatService, ChatQueryRepository chatQueryRepository, ChatQueryService chatQueryService, AgentService agentService) {
        this.templateIssuesRepository = templateIssuesRepository;
        this.chatService = chatService;
        this.chatQueryRepository = chatQueryRepository;
        this.chatQueryService = chatQueryService;
        this.agentService = agentService;
    }

    @Override
    public IPage<TemplateSearchVO> searchTemplate(TemplateSearchReq templateSearchReq, User user) {
        // 输入校验
        if (templateSearchReq == null || user == null) {
            throw new IllegalArgumentException("模板搜索请求或用户信息不能为空");
        }

        // 构建查询条件
        LambdaQueryWrapper<TemplateDO> wrapper = buildTemplateQueryWrapper(templateSearchReq);

        // 查询总数
        long count = this.count(wrapper);
        if (count == 0) {
            return new Page<>(templateSearchReq.getCurrent(), templateSearchReq.getPageSize(), count);
        }

        // 分页查询模板数据
        Page<TemplateDO> page = this.page(new Page<>(templateSearchReq.getCurrent(), templateSearchReq.getPageSize()), wrapper);

        // 查询所有相关的问题记录
        List<TemplateIssuesDO> allIssues = fetchAllRelatedIssues(page.getRecords());

        // 转换为VO并构建分页结果
        return convertToTemplateSearchVOPage(page, allIssues);
    }

    // 构建模板查询条件
    private LambdaQueryWrapper<TemplateDO> buildTemplateQueryWrapper(TemplateSearchReq templateSearchReq) {
        LambdaQueryWrapper<TemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(templateSearchReq.getAgentId() != null, TemplateDO::getAgentId, templateSearchReq.getAgentId())
                .like(StringUtils.isNotBlank(templateSearchReq.getTemplateName()), TemplateDO::getTemplateName, templateSearchReq.getTemplateName())
                .like(StringUtils.isNotBlank(templateSearchReq.getTemplateType()), TemplateDO::getTemplateType, templateSearchReq.getTemplateType())
                .eq(templateSearchReq.getUserId() != null, TemplateDO::getUserId, templateSearchReq.getUserId());
        return wrapper;
    }

    // 批量查询所有相关的问题记录
    private List<TemplateIssuesDO> fetchAllRelatedIssues(List<TemplateDO> templates) {
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取所有模板ID
        List<Integer> templateIds = templates.stream()
                .map(TemplateDO::getTemplateId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询相关问题记录
        LambdaQueryWrapper<TemplateIssuesDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(CollectionUtils.isNotEmpty(templateIds), TemplateIssuesDO::getTemplateId, templateIds);
        return templateIssuesRepository.list(queryWrapper);
    }

    // 转换为TemplateSearchVO分页结果
    private Page<TemplateSearchVO> convertToTemplateSearchVOPage(Page<TemplateDO> templatePage, List<TemplateIssuesDO> allIssues) {
        // 构建分页结果
        Page<TemplateSearchVO> resultPage = new Page<>(templatePage.getCurrent(), templatePage.getSize(), templatePage.getTotal());

        // 将问题记录按模板ID分组
        Map<Integer, List<TemplateIssuesDO>> issuesByTemplateId = allIssues.stream()
                .collect(Collectors.groupingBy(TemplateIssuesDO::getTemplateId));

        // 转换为VO列表
        List<TemplateSearchVO> templateSearchVos = templatePage.getRecords().stream().map(template -> {
            TemplateSearchVO templateSearchVO = new TemplateSearchVO();

            // 模版信息
            TemplateVO templateVO = new TemplateVO();
            BeanUtil.copyProperties(template, templateVO);

            // 问题信息
            List<TemplateIssuesVO> templateIssuesVos = Optional.ofNullable(issuesByTemplateId.get(template.getTemplateId()))
                    .orElse(Collections.emptyList()).stream()
                    .map(issue -> {
                        TemplateIssuesVO templateIssuesVO = new TemplateIssuesVO();
                        BeanUtil.copyProperties(issue, templateIssuesVO);
                        return templateIssuesVO;
                    })
                    .collect(Collectors.toList());

            templateSearchVO.setTemplateVo(templateVO);
            templateSearchVO.setTemplateIssuesVos(templateIssuesVos);

            return templateSearchVO;
        }).collect(Collectors.toList());

        resultPage.setRecords(templateSearchVos);
        return resultPage;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(TemplateReq templateReq, User user) {
        // 参数校验
        if (templateReq == null || templateReq.getTemplateDTO() == null || templateReq.getTemplateIssuesDTOList() == null) {
            throw new RuntimeException("TemplateReq 或其字段不能为 null");
        }

        TemplateDO templateDO = new TemplateDO();

        // 进行助理对话创建（修改不用创建对话）
        if (templateReq.getTemplateDTO().getTemplateId() == null) {
            try {
                Long chatId = chatService.addChat(user
                        , "模版对话-" + templateReq.getTemplateDTO().getTemplateName()
                        , templateReq.getTemplateDTO().getAgentId());
                templateDO.setChatId(chatId);
                templateDO.setUserId(user.getId());
            } catch (Exception e) {
                throw new RuntimeException("模版对话创建失败");
            }
        }

        try {
            BeanUtil.copyProperties(templateReq.getTemplateDTO(), templateDO);
        } catch (Exception e) {
            log.error("无法将属性从 TemplateDTO 复制到 TemplateDO", e);
            throw new RuntimeException("属性复制失败", e);
        }

        try {
            this.saveOrUpdate(templateDO);
        } catch (Exception e) {
            log.error("无法保存 TemplateDO", e);
            throw new RuntimeException("保存 TemplateDO 失败", e);
        }
        Integer templateId = templateReq.getTemplateDTO().getTemplateId();

        List<TemplateIssuesDO> templateIssuesDTOS = new ArrayList<>();
        try {
            // 使用并行流优化性能
            templateReq.getTemplateIssuesDTOList().parallelStream().forEach(templateIssuesDTO -> {
                TemplateIssuesDO templateIssuesDO = new TemplateIssuesDO();
                try {
                    BeanUtil.copyProperties(templateIssuesDTO, templateIssuesDO);
                    templateIssuesDO.setTemplateId(templateId);
                } catch (Exception e) {
                    log.error("无法将属性从 TemplateIssuesDTO 复制到 TemplateIssuesDO", e);
                    throw new RuntimeException("属性复制失败", e);
                }
                templateIssuesDTOS.add(templateIssuesDO);
            });
        } catch (Exception e) {
            log.error("处理 TemplateIssuesDTOList 时出错", e);
            throw new RuntimeException("处理 TemplateIssuesDTOList 失败", e);
        }

        // 数据校验
        if (!validateTemplateIssues(templateIssuesDTOS)) {
            throw new IllegalArgumentException("某些 TemplateIssuesDO 对象无效");
        }

        try {
            templateIssuesRepository.saveOrUpdateBatch(templateIssuesDTOS);
        } catch (Exception e) {
            log.error("无法保存批量 TemplateIssuesDO", e);
            throw new RuntimeException("批量保存 TemplateIssuesDO 失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(TemplateReq templateReq, User user) {
        // 修改时，模版方法不能修改
        templateReq.getTemplateDTO().setAgentId(null);

        createTemplate(templateReq, user);
    }


    // 数据校验方法
    private boolean validateTemplateIssues(List<TemplateIssuesDO> templateIssuesDTOS) {
        for (TemplateIssuesDO templateIssuesDO : templateIssuesDTOS) {
            if (templateIssuesDO == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Integer templateId, Long chatId, String userName) {
        // 进行模版删除
        this.removeById(templateId);

        // 进行模版问题删除
        templateIssuesRepository.removeByTemplateId(templateId);

        // 进行聊天数据删除
        chatService.deleteChat(chatId, userName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void interaction(InteractionReq interactionReq, User user) {
        // 空值检查
        if (interactionReq == null || interactionReq.getQueryText() == null || interactionReq.getQueryText().isEmpty()) {
            throw new IllegalArgumentException("无效的 interactionReq 或 queryText 为空");
        }
        if (interactionReq.getChatId() == null || interactionReq.getAgentId() == null) {
            throw new IllegalArgumentException("ChatId 或 AgentId 不能为 null");
        }

        List<Long> queryIds = new ArrayList<>();

        try {
            // 批量解析 queryText
            for (String queryText : interactionReq.getQueryText()) {
                ChatParseReq chatParseReq = new ChatParseReq();
                chatParseReq.setQueryText(queryText);
                chatParseReq.setChatId(interactionReq.getChatId());
                chatParseReq.setAgentId(interactionReq.getAgentId());

                try {
                    // 单独捕获每个 queryText 的异常
                    Long queryId = chatQueryService.parse(chatParseReq).getQueryId();
                    queryIds.add(queryId);
                } catch (Exception e) {
                    // 记录日志，继续处理下一个 queryText
                    log.error(String.format("无法解析 queryText: %s, error: %s", queryText, e.getMessage()));
                }
            }

            // 获取对话记录
            List<QueryResp> chatQueries = getChatQueries(queryIds);

            // 获取 Agent 信息
            Agent agent = getAgent(interactionReq.getAgentId());

            // 生成报告
            ReportUtil.getReport(chatQueries, agent);
        } catch (Exception e) {
            log.error(String.format("交互处理过程中的错误: %s", e.getMessage()));
            // 重新抛出异常，确保事务回滚
            throw new RuntimeException(e);
        }
    }

    // 提取独立方法，减少事务范围
    private List<QueryResp> getChatQueries(List<Long> queryIds) {
        if (queryIds == null || queryIds.isEmpty()) {
            // 处理空列表的情况
            return Collections.emptyList();
        }
        return chatQueryRepository.getChatQueries(queryIds);
    }

    // 提取独立方法，减少事务范围
    private Agent getAgent(Integer agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("AgentId 不能为 null");
        }
        return agentService.getAgent(agentId);
    }
}




