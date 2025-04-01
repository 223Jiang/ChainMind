package com.tencent.supersonic.chat.server.req;

import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemplateIssuesDO;
import com.tencent.supersonic.chat.server.req.dto.TemplateDTO;
import com.tencent.supersonic.chat.server.req.dto.TemplateIssuesDTO;
import lombok.Data;

import java.util.List;

/**
 * 模版创建实体
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/28
 */
@Data
public class TemplateReq {

    /**
     * 模版
     */
    private TemplateDTO templateDTO;

    /**
     * 模版问题id
     */
    private List<TemplateIssuesDTO> templateIssuesDTOList;
}
