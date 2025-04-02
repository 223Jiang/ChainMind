package com.tencent.supersonic.chat.server.persistence.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateDO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tencent.supersonic.chat.server.req.InteractionReq;
import com.tencent.supersonic.chat.server.req.TemplateReq;
import com.tencent.supersonic.chat.server.req.TemplateSearchReq;
import com.tencent.supersonic.chat.server.req.dto.DataExportDTO;
import com.tencent.supersonic.chat.server.req.vo.InteractionVO;
import com.tencent.supersonic.chat.server.req.vo.TemplateSearchVO;
import com.tencent.supersonic.common.pojo.User;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author JiangWeiWei
 * @description 针对表【s2_template(模版)】的数据库操作Service
 * @createDate 2025-03-28 14:30:04
 */
public interface TemplateRepository extends IService<TemplateDO> {
    /**
     * @param templateSearchReq 查询模版
     * @param user              查询用户
     * @return 模版数据集
     */
    IPage<TemplateSearchVO> searchTemplate(TemplateSearchReq templateSearchReq, User user);

    /**
     * 模版创建
     *
     * @param templateReq 模版
     * @param user        用户
     */
    void createTemplate(TemplateReq templateReq, User user);

    /**
     * 模版修改
     *
     * @param templateReq 模版
     * @param user        用户
     */
    void updateTemplate(TemplateReq templateReq, User user);

    /**
     * 模版删除
     *
     * @param templateId 模版id
     * @param chatId     对话id
     * @param userName   删除用户
     */
    void deleteTemplate(Integer templateId, Long chatId, String userName);

    /**
     * 模版问答
     *
     * @param interactionReq 问答请求数据
     * @param user           用户数据
     */
    InteractionVO interaction(InteractionReq interactionReq, User user);

    /**
     * 问答数据导出为PDF请求实体
     *
     * @param dto 导出模版问答实传递实体
     */
    void pdfExport(HttpServletResponse response, DataExportDTO dto);

    /**
     * 问答数据导出为MD请求实体
     *
     * @param dto 导出模版问答实传递实体
     */
    void mdExport(HttpServletResponse response, DataExportDTO dto);
}
