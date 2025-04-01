package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.req.vo.TemplateDictionaryVO;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface AgentService {

    List<Agent> getAgents();

    Agent createAgent(Agent agent, User user);

    Agent updateAgent(Agent agent, User user);

    Agent getAgent(Integer id);

    void deleteAgent(Integer id);

    /**
     * 获取助手字典
     *
     * @return  助手字典数据集
     */
    List<TemplateDictionaryVO> templateDictionary();
}
