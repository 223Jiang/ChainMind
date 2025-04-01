package com.tencent.supersonic.chat.server.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.server.persistence.repository.TemplateRepository;
import com.tencent.supersonic.chat.server.req.InteractionReq;
import com.tencent.supersonic.chat.server.req.TemplateReq;
import com.tencent.supersonic.chat.server.req.TemplateSearchReq;
import com.tencent.supersonic.chat.server.req.vo.TemplateSearchVO;
import com.tencent.supersonic.common.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 模版接口
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/28
 */
@Slf4j
@RestController
@RequestMapping("/supersonic/api/template")
public class TemplateController {
    private final TemplateRepository templateRepository;

    public TemplateController(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * 模版查询
     *
     * @param templateSearchReq  模版
     * @return                   模板列表集
     */
    @PostMapping("/search")
    public IPage<TemplateSearchVO> search(@RequestBody TemplateSearchReq templateSearchReq,
                                          HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        try {
            return templateRepository.searchTemplate(templateSearchReq, user);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 模版创建
     *
     * @param templateReq  模版
     * @return          true成功，false失败
     */
    @PostMapping("/create")
    public Boolean create(@RequestBody TemplateReq templateReq,
                                  HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        try {
            templateRepository.createTemplate(templateReq, user);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 模版修改
     *
     * @param templateReq  模版（注意传模版id）
     * @return          true成功，false失败
     */
    @PostMapping("/update")
    public Boolean updateTemplate(@RequestBody TemplateReq templateReq,
                          HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        try {
            templateRepository.updateTemplate(templateReq, user);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 模版删除
     *
     * @param id        模版id
     * @param chatId    对话id
     * @return          true成功，false失败
     */
    @PostMapping("/delete/{id}/{chatId}")
    public Boolean deleteTemplate(@PathVariable Integer id,
                                  @PathVariable Long chatId,
                                  HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        try {
            templateRepository.deleteTemplate(id, chatId, user.getName());
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 模版问答
     *
     * @param interactionReq        模版问答请求数据
     * @return                      true成功，false失败
     */
    @PostMapping("/interaction")
    public Boolean interaction(@RequestBody InteractionReq interactionReq,
                               HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        try {
            templateRepository.interaction(interactionReq, user);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
