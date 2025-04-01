package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateIssuesDO;
import com.tencent.supersonic.chat.server.persistence.repository.TemplateIssuesRepository;
import com.tencent.supersonic.chat.server.persistence.mapper.TemplateIssuesMapper;
import org.springframework.stereotype.Service;

/**
* @author JiangWeiWei
* @description 针对表【s2_template_issues(模版问题)】的数据库操作Service实现
* @createDate 2025-03-28 14:30:04
*/
@Service
public class TemplateIssuesRepositoryImpl extends ServiceImpl<TemplateIssuesMapper, TemplateIssuesDO>
    implements TemplateIssuesRepository {

    @Override
    public void removeByTemplateId(Integer templateId) {
        LambdaQueryWrapper<TemplateIssuesDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateIssuesDO::getTemplateId, templateId);

        this.remove(wrapper);
    }
}




