package com.tencent.supersonic.chat.server.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.req.vo.TemplateDictionaryVO;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 助手接口
 */
@RestController
@RequestMapping({"/supersonic/api/chat/agent", "/supersonic/openapi/chat/agent"})
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping
    public Agent createAgent(@RequestBody Agent agent, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return agentService.createAgent(agent, user);
    }

    @PutMapping
    public Agent updateAgent(@RequestBody Agent agent, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return agentService.updateAgent(agent, user);
    }

    @DeleteMapping("/{id}")
    public boolean deleteAgent(@PathVariable("id") Integer id) {
        agentService.deleteAgent(id);
        return true;
    }

    @RequestMapping("/getAgentList")
    public List<Agent> getAgentList() {
        return agentService.getAgents();
    }

    @RequestMapping("/getToolTypes")
    public Map<AgentToolType, String> getToolTypes() {
        return AgentToolType.getToolTypes();
    }

    /**
     * 获取助手字典
     *
     * @return  助手字典数据集
     */
    @RequestMapping("/templateDictionary")
    public List<TemplateDictionaryVO> templateDictionary() {
        return agentService.templateDictionary();
    }
}
