package com.tencent.supersonic.headless.server.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.*;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.server.utils.ModelConfigHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/supersonic/api/chat/model", "/supersonic/openapi/chat/model"})
public class ChatModelController {
    @Autowired
    private ChatModelService chatModelService;

    @PostMapping
    public ChatModel createModel(@RequestBody ChatModel model,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return chatModelService.createChatModel(model, user);
    }

    @PutMapping
    public ChatModel updateModel(@RequestBody ChatModel model,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return chatModelService.updateChatModel(model, user);
    }

    @DeleteMapping("/{id}")
    public boolean deleteModel(@PathVariable("id") Integer id) {
        chatModelService.deleteChatModel(id);
        return true;
    }

    @RequestMapping("/getModelList")
    public List<ChatModel> getModelList() {
        return chatModelService.getChatModels();
    }

    @RequestMapping("/getModelAppList")
    public Map<String, ChatApp> getChatAppList() {
        return ChatAppManager.getAllApps(AppModule.CHAT);
    }

    @RequestMapping("/getModelParameters")
    public List<Parameter> getModelParameters() {
        return ChatModelParameters.getParameters();
    }

    @PostMapping("/testConnection")
    public boolean testConnection(@RequestBody ChatModelConfig modelConfig) {
        return ModelConfigHelper.testConnection(modelConfig);
    }
}
