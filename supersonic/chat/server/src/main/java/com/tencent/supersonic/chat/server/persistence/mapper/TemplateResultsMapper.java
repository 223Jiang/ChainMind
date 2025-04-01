package com.tencent.supersonic.chat.server.persistence.mapper;

import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateResultsDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author JiangWeiWei
* @description 针对表【s2_template_results(模版结果)】的数据库操作Mapper
* @createDate 2025-03-28 14:30:04
* @Entity com.tencent.supersonic.chat.server.persistence.dataobject.TemplateResults
*/
@Mapper
public interface TemplateResultsMapper extends BaseMapper<TemplateResultsDO> {

}




