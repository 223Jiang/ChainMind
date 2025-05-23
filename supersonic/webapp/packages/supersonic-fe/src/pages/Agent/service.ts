import {request} from 'umi';
import {AgentType, MemoryType, MetricType, ModelType} from './type';

export function getAgentList() {
    return request<Result<AgentType[]>>('/supersonic/api/chat/agent/getAgentList');
}

export function saveAgent(agent: AgentType) {
    return request<Result<any>>('/supersonic/api/chat/agent', {
        method: agent?.id ? 'PUT' : 'POST',
        data: {...agent, status: agent.status !== undefined ? agent.status : 1},
    });
}

export function deleteAgent(id: number) {
    return request<Result<any>>(`/supersonic/api/chat/agent/${id}`, {
        method: 'DELETE',
    });
}

export function getModelList() {
    return request<Result<ModelType[]>>('/supersonic/api/chat/conf/getDomainDataSetTree', {
        method: 'GET',
    });
}

export function getMetricList(modelId: number) {
    return request<Result<{ list: MetricType[] }>>('/supersonic/api/semantic/metric/queryMetric', {
        method: 'POST',
        data: {
            modelIds: [modelId],
            current: 1,
            pageSize: 2000,
        },
    });
}

export function getMemeoryList(data: { agentId: number; chatMemoryFilter: any; current: number }) {
    const {agentId, chatMemoryFilter, current} = data;
    return request<Result<{ list: MetricType[] }>>('/supersonic/api/chat/memory/pageMemories', {
        method: 'POST',
        data: {
            ...data,
            chatMemoryFilter: {agentId, ...chatMemoryFilter},
            current,
            pageSize: 10,
            sort: 'desc',
            // orderCondition: 'updatedAt',
        },
    });
}

export function saveMemory(data: MemoryType) {
    return request<Result<string>>('/supersonic/api/chat/memory/updateMemory', {
        method: 'POST',
        data,
    });
}

export function batchDeleteMemory(ids: number[]) {
    return request<Result<string>>('/supersonic/api/chat/memory/batchDelete', {
        method: 'POST',
        data: {ids},
    });
}

export function getToolTypes(): Promise<any> {
    return request(`${process.env.CHAT_API_BASE_URL}agent/getToolTypes`, {
        method: 'GET',
    });
}

export function createMemory(data: any) {
    return request<Result<string>>('/supersonic/api/chat/memory/createMemory', {
        method: 'POST',
        data,
    });
}
