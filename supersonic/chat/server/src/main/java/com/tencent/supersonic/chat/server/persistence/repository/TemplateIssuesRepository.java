package com.tencent.supersonic.chat.server.persistence.repository;

import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateIssuesDO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author JiangWeiWei
* @description 针对表【s2_template_issues(模版问题)】的数据库操作Service
* @createDate 2025-03-28 14:30:04
*/
public interface TemplateIssuesRepository extends IService<TemplateIssuesDO> {

    /**
     * 根据模版id删除模版问题
     * @param templateId    模版templateId
     */
    void removeByTemplateId(Integer templateId);
}
