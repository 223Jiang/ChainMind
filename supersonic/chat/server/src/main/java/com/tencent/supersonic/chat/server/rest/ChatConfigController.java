package com.tencent.supersonic.chat.server.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.server.service.ConfigService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/supersonic/api/chat/conf", "/supersonic/openapi/chat/conf"})
public class ChatConfigController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private SemanticLayerService semanticLayerService;

    @PostMapping
    public Long addChatConfig(@RequestBody ChatConfigBaseReq extendBaseCmd,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.addConfig(extendBaseCmd, user);
    }

    @PutMapping
    public Long editModelExtend(@RequestBody ChatConfigEditReqReq extendEditCmd,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.editConfig(extendEditCmd, user);
    }

    @PostMapping("/search")
    public List<ChatConfigResp> search(@RequestBody ChatConfigFilter filter,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.search(filter, user);
    }

    @GetMapping("/richDesc/{modelId}")
    public ChatConfigRichResp getModelExtendRichInfo(@PathVariable("modelId") Long modelId) {
        return configService.getConfigRichInfo(modelId);
    }

    @GetMapping("/richDesc/all")
    public List<ChatConfigRichResp> getAllChatRichConfig() {
        return configService.getAllChatRichConfig();
    }

    @GetMapping("/getDomainDataSetTree")
    public List<ItemResp> getDomainDataSetTree() {
        return semanticLayerService.getDomainDataSetTree();
    }

    @GetMapping("/getDataSetSchema/{id}")
    public DataSetSchema getDataSetSchema(@PathVariable("id") Long id) {
        return semanticLayerService.getDataSetSchema(id);
    }
}
