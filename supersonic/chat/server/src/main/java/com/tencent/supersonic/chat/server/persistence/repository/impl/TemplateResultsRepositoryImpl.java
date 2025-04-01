package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateResultsDO;
import com.tencent.supersonic.chat.server.persistence.repository.TemplateResultsRepository;
import com.tencent.supersonic.chat.server.persistence.mapper.TemplateResultsMapper;
import org.springframework.stereotype.Service;

/**
* @author JiangWeiWei
* @description 针对表【s2_template_results(模版结果)】的数据库操作Service实现
* @createDate 2025-03-28 14:30:04
*/
@Service
public class TemplateResultsRepositoryImpl extends ServiceImpl<TemplateResultsMapper, TemplateResultsDO>
    implements TemplateResultsRepository {

}




